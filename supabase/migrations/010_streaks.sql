-- 010: daily streaks.
--
-- Tracks consecutive days the user converts steps. Bonus coins are awarded
-- at milestones (7, 14, 30 days) via the sync-steps edge function.

alter table public.profiles
  add column if not exists streak_days integer not null default 0,
  add column if not exists last_streak_date date;

comment on column public.profiles.streak_days is 'Consecutive days with a step conversion.';
comment on column public.profiles.last_streak_date is 'Local date of the most recent day counted toward the streak.';
