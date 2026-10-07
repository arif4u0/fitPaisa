-- 004: product catalog, redemptions, cashouts, admin flag.
--
-- Products users buy with FitCoins; redemptions track orders;
-- cashout_requests track manual Easypaisa/JazzCash/bank payouts.
-- Coin spends are atomic via security-definer RPCs (no client-side math).

-- Admin flag on profiles (set your own row to true via SQL).
alter table public.profiles
  add column if not exists is_admin boolean not null default false;

-- Product catalog.
create table if not exists public.products (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  description text not null default '',
  image_url text,
  coin_price integer not null check (coin_price > 0),
  stock integer not null default 0 check (stock >= 0),
  is_active boolean not null default true,
  sort_order integer not null default 0,
  created_at timestamptz not null default now()
);
create index if not exists products_active_idx
  on public.products (is_active, sort_order) where is_active;

-- Product redemptions (orders).
create table if not exists public.redemptions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade,
  product_id uuid not null references public.products (id),
  product_name text not null,
  coins_spent integer not null check (coins_spent > 0),
  status text not null default 'pending'
    check (status in ('pending', 'fulfilled', 'cancelled')),
  recipient_name text,
  recipient_phone text,
  recipient_address text,
  created_at timestamptz not null default now(),
  fulfilled_at timestamptz
);
create index if not exists redemptions_user_idx on public.redemptions (user_id, created_at desc);
create index if not exists redemptions_status_idx on public.redemptions (status, created_at desc);

-- Manual cashout requests (founder pays via Easypaisa/JazzCash/bank, then marks paid).
create table if not exists public.cashout_requests (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade,
  coins integer not null check (coins > 0),
  rupees_paisa integer not null check (rupees_paisa > 0),
  method text not null check (method in ('easypaisa', 'jazzcash', 'bank')),
  account_title text not null,
  account_number text not null,
  status text not null default 'pending'
    check (status in ('pending', 'paid', 'rejected')),
  provider_reference text,
  created_at timestamptz not null default now(),
  decided_at timestamptz
);
create index if not exists cashout_user_idx on public.cashout_requests (user_id, created_at desc);
create index if not exists cashout_status_idx on public.cashout_requests (status, created_at desc);

-- Atomic product redemption: checks stock + balance, spends coins, decrements stock.
create or replace function public.redeem_product(
  p_user_id uuid,
  p_product_id uuid,
  p_recipient_name text,
  p_recipient_phone text,
  p_recipient_address text
)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_price integer;
  v_name text;
  v_available integer;
  v_redemption_id uuid;
begin
  -- Lock the product row.
  select coin_price, name into v_price, v_name
  from public.products
  where id = p_product_id and is_active
  for update;

  if not found then
    raise exception 'product_not_available';
  end if;

  if (select stock from public.products where id = p_product_id) <= 0 then
    raise exception 'product_out_of_stock';
  end if;

  -- Check spendable balance.
  select coalesce(sum(amount_coins), 0)::integer into v_available
  from public.ledger_entries
  where user_id = p_user_id and state = 'available';

  if v_available < v_price then
    raise exception 'insufficient_coins';
  end if;

  -- Spend the coins (negative available entry).
  insert into public.ledger_entries (user_id, amount_coins, state, source_type, source_id)
  values (p_user_id, -v_price, 'available', 'redemption', 'redeem:' || p_product_id::text || ':' || extract(epoch from now())::bigint);

  -- Decrement stock.
  update public.products set stock = stock - 1 where id = p_product_id;

  -- Record the redemption.
  insert into public.redemptions (user_id, product_id, product_name, coins_spent, recipient_name, recipient_phone, recipient_address)
  values (p_user_id, p_product_id, v_name, v_price, p_recipient_name, p_recipient_phone, p_recipient_address)
  returning id into v_redemption_id;

  return v_redemption_id;
end;
$$;

-- Atomic cashout request: checks minimum + balance, reserves coins.
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
  v_min_coins integer := 10000; -- Rs 100 minimum, matches EconomyConfig
  v_paisa integer;
  v_request_id uuid;
begin
  if p_coins < v_min_coins then
    raise exception 'below_minimum';
  end if;

  if p_method not in ('easypaisa', 'jazzcash', 'bank') then
    raise exception 'bad_method';
  end if;

  select coalesce(sum(amount_coins), 0)::integer into v_available
  from public.ledger_entries
  where user_id = p_user_id and state = 'available';

  if v_available < p_coins then
    raise exception 'insufficient_coins';
  end if;

  -- 1 coin = 1 paisa.
  v_paisa := p_coins;

  -- Reserve the coins (negative available entry).
  insert into public.ledger_entries (user_id, amount_coins, state, source_type, source_id)
  values (p_user_id, -p_coins, 'available', 'cashout', 'cashout:' || extract(epoch from now())::bigint);

  insert into public.cashout_requests (user_id, coins, rupees_paisa, method, account_title, account_number)
  values (p_user_id, p_coins, v_paisa, p_method, p_account_title, p_account_number)
  returning id into v_request_id;

  return v_request_id;
end;
$$;

-- RLS: products are publicly readable; everything else is owner-or-admin.
alter table public.products enable row level security;
alter table public.redemptions enable row level security;
alter table public.cashout_requests enable row level security;

drop policy if exists "products_public_read" on public.products;
create policy "products_public_read" on public.products
  for select using (is_active = true);

drop policy if exists "redemptions_owner_read" on public.redemptions;
create policy "redemptions_owner_read" on public.redemptions
  for select using (auth.uid() = user_id);

drop policy if exists "cashout_owner_read" on public.cashout_requests;
create policy "cashout_owner_read" on public.cashout_requests
  for select using (auth.uid() = user_id);

comment on table public.products is 'FitCoin product catalog (admin-managed).';
comment on table public.redemptions is 'Product orders placed with FitCoins.';
comment on table public.cashout_requests is 'Manual cashout requests (founder pays out).';
