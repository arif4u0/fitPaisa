-- 005: admin helpers — is_admin() check, admin RLS policies, resolution RPCs.

-- Helper: true when the calling user is an admin.
create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select coalesce((select is_admin from public.profiles where id = auth.uid()), false);
$$;

-- Admins can manage the catalog.
drop policy if exists "products_admin_write" on public.products;
create policy "products_admin_write" on public.products
  for all using (public.is_admin()) with check (public.is_admin());

-- Admins can see and update all redemptions.
drop policy if exists "redemptions_admin_all" on public.redemptions;
create policy "redemptions_admin_all" on public.redemptions
  for all using (public.is_admin()) with check (public.is_admin());

-- Admins can see and update all cashout requests.
drop policy if exists "cashout_admin_all" on public.cashout_requests;
create policy "cashout_admin_all" on public.cashout_requests
  for all using (public.is_admin()) with check (public.is_admin());

-- Resolve a redemption (fulfill or cancel). Cancelling refunds the coins.
create or replace function public.resolve_redemption(p_redemption_id uuid, p_fulfilled boolean)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user_id uuid;
  v_coins integer;
  v_status text;
begin
  if not public.is_admin() then
    raise exception 'not_admin';
  end if;

  select user_id, coins_spent, status into v_user_id, v_coins, v_status
  from public.redemptions where id = p_redemption_id for update;

  if not found then raise exception 'not_found'; end if;
  if v_status <> 'pending' then raise exception 'already_decided'; end if;

  if p_fulfilled then
    update public.redemptions
    set status = 'fulfilled', fulfilled_at = now()
    where id = p_redemption_id;
  else
    -- Refund the coins.
    insert into public.ledger_entries (user_id, amount_coins, state, source_type, source_id)
    values (v_user_id, v_coins, 'available', 'refund', 'refund:' || p_redemption_id::text);
    update public.redemptions
    set status = 'cancelled'
    where id = p_redemption_id;
  end if;
end;
$$;

-- Resolve a cashout request (paid or rejected). Rejecting refunds the coins.
create or replace function public.resolve_cashout(
  p_request_id uuid,
  p_paid boolean,
  p_reference text default null
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user_id uuid;
  v_coins integer;
  v_status text;
begin
  if not public.is_admin() then
    raise exception 'not_admin';
  end if;

  select user_id, coins, status into v_user_id, v_coins, v_status
  from public.cashout_requests where id = p_request_id for update;

  if not found then raise exception 'not_found'; end if;
  if v_status <> 'pending' then raise exception 'already_decided'; end if;

  if p_paid then
    update public.cashout_requests
    set status = 'paid', provider_reference = p_reference, decided_at = now()
    where id = p_request_id;
  else
    -- Refund the reserved coins.
    insert into public.ledger_entries (user_id, amount_coins, state, source_type, source_id)
    values (v_user_id, v_coins, 'available', 'refund', 'refund:' || p_request_id::text);
    update public.cashout_requests
    set status = 'rejected', decided_at = now()
    where id = p_request_id;
  end if;
end;
$$;
