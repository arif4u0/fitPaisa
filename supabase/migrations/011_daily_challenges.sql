-- 011: Daily challenges — walk X steps, earn bonus coins.
-- The challenge target and bonus are configurable via app_config.
-- sync-steps awards the bonus once per day when the target is hit.

-- Configurable challenge settings.
insert into public.app_config (key, value) values
  ('daily_challenge_target', '8000'),
  ('daily_challenge_bonus', '50')
on conflict (key) do nothing;

-- Track whether today's challenge bonus was already claimed.
alter table public.daily_steps
  add column if not exists challenge_bonus_claimed boolean not null default false;
