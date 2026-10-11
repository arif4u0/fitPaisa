-- 012: Lucky Spin, Leaderboard, Achievements
-- Spin: 1 free spin/day + unlimited ad spins. Server picks the prize.
-- Leaderboard: weekly top walkers (public usernames + step totals).
-- Achievements: milestone badges stored per user.

-- Spin history: one row per spin.
create table if not exists public.spin_history (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  local_date date not null,
  prize_coins integer not null,
  spin_type text not null check (spin_type in ('free', 'ad')),
  created_at timestamptz not null default now()
);
create index if not exists spin_history_user_date
  on public.spin_history (user_id, local_date);

-- Achievements: one row per unlocked badge.
create table if not exists public.user_achievements (
  user_id uuid not null references auth.users(id) on delete cascade,
  achievement_id text not null,
  unlocked_at timestamptz not null default now(),
  primary key (user_id, achievement_id)
);

-- Users can read their own spins and achievements.
alter table public.spin_history enable row level security;
alter table public.user_achievements enable row level security;

drop policy if exists "spin_history_owner_read" on public.spin_history;
create policy "spin_history_owner_read" on public.spin_history
  for select using (auth.uid() = user_id);

drop policy if exists "user_achievements_owner_read" on public.user_achievements;
create policy "user_achievements_owner_read" on public.user_achievements
  for select using (auth.uid() = user_id);

-- Weekly leaderboard: public read of display names + weekly step totals.
-- Uses profiles.display_name (falls back to 'Walker' when null).
drop view if exists public.weekly_leaderboard;
create view public.weekly_leaderboard as
select
  p.id as user_id,
  coalesce(nullif(p.display_name, ''), 'Walker') as display_name,
  coalesce(sum(d.eligible_total), 0)::bigint as week_steps
from public.profiles p
left join public.daily_steps d
  on d.user_id = p.id
  and d.local_date >= date_trunc('week', now())::date
group by p.id, p.display_name
having coalesce(sum(d.eligible_total), 0) > 0
order by week_steps desc
limit 50;
