-- ============================================================================
-- 002_commercial_economics.sql — confirmed commercial economics (2026-10-04)
--
-- Run AFTER 001_initial_schema.sql in the Supabase SQL editor.
-- Economics: 100 steps = 1 FitCoin, 1 FitCoin = 1 paisa (100 coins = Rs 1).
-- Cashout minimum: 10,000 coins = Rs 100. Step coins mature after 7 days.
-- ============================================================================

-- --------------------------------------------------------------------------
-- levels: per-level daily step caps, ad limits, and ad bonus multipliers.
-- --------------------------------------------------------------------------
alter table public.levels
    add column if not exists daily_step_cap int not null default 10000,
    add column if not exists daily_ad_limit int not null default 5,
    add column if not exists ad_bonus_multiplier numeric not null default 1.0;

insert into public.levels
    (level, name, lifetime_steps_threshold, rewarded_ads_per_day, unlock_note,
     daily_step_cap, daily_ad_limit, ad_bonus_multiplier)
values
    (1, 'Starter',   0,     5, 'Your starting level',   10000,  5, 1.00),
    (2, 'Walker',    50000,  6, 'Bigger daily step cap', 12000,  6, 1.10),
    (3, 'Active',   150000,  8, 'More ads, bonus coins', 15000,  8, 1.25),
    (4, 'Athlete',  300000, 10, 'Higher caps, bonuses', 20000, 10, 1.50),
    (5, 'Champion', 500000, 12, 'Double ad coins',       25000, 12, 2.00)
on conflict (level) do update set
    name                     = excluded.name,
    lifetime_steps_threshold = excluded.lifetime_steps_threshold,
    rewarded_ads_per_day     = excluded.rewarded_ads_per_day,
    unlock_note              = excluded.unlock_note,
    daily_step_cap           = excluded.daily_step_cap,
    daily_ad_limit           = excluded.daily_ad_limit,
    ad_bonus_multiplier      = excluded.ad_bonus_multiplier;

-- --------------------------------------------------------------------------
-- profiles: lifetime step tracking + current level (maintained by sync-steps).
-- --------------------------------------------------------------------------
alter table public.profiles
    add column if not exists lifetime_steps bigint not null default 0,
    add column if not exists level int not null default 1;

-- Backfill from any days already synced.
update public.profiles p
set lifetime_steps = coalesce((
    select sum(d.credited_steps) from public.daily_steps d where d.user_id = p.id
), 0);

update public.profiles p
set level = coalesce((
    select max(l.level) from public.levels l
    where p.lifetime_steps >= l.lifetime_steps_threshold
), 1);

-- --------------------------------------------------------------------------
-- reward_rules v2 (commercial) + activate it.
-- --------------------------------------------------------------------------
insert into public.reward_rules
    (version, steps_per_coin, daily_step_cap, coin_cash_value_paisa,
     payout_min_coins, reward_hold_days)
values
    (2, 100, 10000, 1, 10000, 7)
on conflict (version) do update set
    steps_per_coin         = excluded.steps_per_coin,
    daily_step_cap         = excluded.daily_step_cap,
    coin_cash_value_paisa  = excluded.coin_cash_value_paisa,
    payout_min_coins       = excluded.payout_min_coins,
    reward_hold_days       = excluded.reward_hold_days;

update public.app_config set value = '2'::jsonb where key = 'rule_version';

-- --------------------------------------------------------------------------
-- release_matured_coins: flip 'held' entries whose hold expired to 'available'.
-- Called by the get-balance edge function on every balance read, so no cron
-- job is needed. Returns the number of entries released.
-- --------------------------------------------------------------------------
create or replace function public.release_matured_coins(p_user_id uuid)
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
    v_released integer;
begin
    update public.ledger_entries
    set state = 'available'
    where user_id = p_user_id
      and state = 'held'
      and available_at is not null
      and available_at <= now();

    get diagnostics v_released = row_count;
    return v_released;
end;
$$;
