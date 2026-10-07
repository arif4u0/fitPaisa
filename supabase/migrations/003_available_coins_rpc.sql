-- 003: add the missing user_available_coins() helper used by get-balance.
-- Returns the user's spendable coin balance: sum of ledger entries in 'available' state.

create or replace function public.user_available_coins(p_user_id uuid)
returns integer
language sql
stable
security definer
set search_path = public
as $$
  select coalesce(sum(amount_coins), 0)::integer
  from public.ledger_entries
  where user_id = p_user_id
    and state = 'available';
$$;

comment on function public.user_available_coins(uuid) is
  'Spendable coin balance: sum of ledger_entries in available state for the user.';
