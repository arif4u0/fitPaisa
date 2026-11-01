-- 015_wallet_security.sql
-- Hardens wallet transactions against concurrency double-spend races,
-- limits pending cashouts to 1 per user, and adds sybil account fraud detection.

-- 1. Hardened atomic cashout request RPC
create or replace function public.request_cashout(
  p_user_id uuid,
  p_coins integer,
  p_method text,
  p_account_title text,
  p_account_number text
)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_available integer;
  v_min_coins integer := 10000; -- Rs 100 minimum (100 coins = 1 PKR)
  v_paisa integer;
  v_request_id uuid;
begin
  -- Concurrency guard: serialize concurrent requests per user
  perform pg_advisory_xact_lock(hashtext(p_user_id::text));

  -- Minimum threshold check
  if p_coins < v_min_coins then
    raise exception 'below_minimum';
  end if;

  -- Method validation
  if p_method not in ('easypaisa', 'jazzcash', 'bank') then
    raise exception 'bad_method';
  end if;

  -- Enforce single pending cashout limit to protect founder manual payouts
  if exists (
    select 1 from public.cashout_requests
    where user_id = p_user_id and status = 'pending'
  ) then
    raise exception 'pending_cashout_exists';
  end if;

  -- Recompute current available balance strictly within transaction lock
  select coalesce(sum(amount_coins), 0)::integer into v_available
  from public.ledger_entries
  where user_id = p_user_id and state = 'available';

  if v_available < p_coins then
    raise exception 'insufficient_coins';
  end if;

  v_paisa := p_coins;

  -- Reserve the coins immediately with collision-free UUID source_id
  insert into public.ledger_entries (user_id, amount_coins, state, source_type, source_id, metadata)
  values (
    p_user_id,
    -p_coins,
    'available',
    'cashout',
    'cashout:' || gen_random_uuid(),
    jsonb_build_object(
      'method', p_method,
      'account_title', p_account_title,
      'account_number', p_account_number
    )
  );

  insert into public.cashout_requests (
    user_id,
    coins,
    rupees_paisa,
    method,
    account_title,
    account_number,
    status
  )
  values (
    p_user_id,
    p_coins,
    v_paisa,
    p_method,
    p_account_title,
    p_account_number,
    'pending'
  )
  returning id into v_request_id;

  return v_request_id;
end;
$$;

-- 2. Concurrency guard for physical store redemptions
create or replace function public.redeem_product(
  p_user_id uuid,
  p_product_id uuid,
  p_shipping_address jsonb,
  p_contact_phone text
)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_available integer;
  v_price integer;
  v_stock integer;
  v_redemption_id uuid;
begin
  -- Concurrency guard
  perform pg_advisory_xact_lock(hashtext(p_user_id::text));

  select price_coins, stock into v_price, v_stock
  from public.products
  where id = p_product_id and is_active = true;

  if not found then
    raise exception 'product_not_found';
  end if;

  if v_stock <= 0 then
    raise exception 'out_of_stock';
  end if;

  select coalesce(sum(amount_coins), 0)::integer into v_available
  from public.ledger_entries
  where user_id = p_user_id and state = 'available';

  if v_available < v_price then
    raise exception 'insufficient_coins';
  end if;

  update public.products
  set stock = stock - 1
  where id = p_product_id;

  insert into public.ledger_entries (user_id, amount_coins, state, source_type, source_id)
  values (p_user_id, -v_price, 'available', 'product_redemption', 'redemption:' || gen_random_uuid());

  insert into public.redemptions (user_id, product_id, coins_spent, shipping_address, contact_phone)
  values (p_user_id, p_product_id, v_price, p_shipping_address, p_contact_phone)
  returning id into v_redemption_id;

  return v_redemption_id;
end;
$$;

-- 3. Sybil Account Detection View for Founder Dashboard
create or replace view public.view_sybil_wallet_alerts as
select
  account_number,
  method,
  count(distinct user_id) as linked_accounts_count,
  array_agg(distinct user_id) as linked_user_ids,
  sum(coins) as total_coins_requested,
  max(created_at) as last_request_at
from public.cashout_requests
group by account_number, method
having count(distinct user_id) > 1;

grant select on public.view_sybil_wallet_alerts to authenticated;
