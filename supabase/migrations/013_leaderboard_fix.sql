-- 013: Fix the weekly leaderboard and make levels visible.
--
-- Why: the old view ran with the caller's permissions, so if profiles or
-- daily_steps are locked down by row-level security, each user only saw
-- their own row (or nothing). It also grouped from profiles, so a user
-- without a profile row could not appear at all. The new view runs as the
-- view owner (Postgres 15 security_invoker = false) and starts from
-- daily_steps, so it only exposes the public leaderboard columns:
-- user_id, display_name and this week's step total.
--
-- The week starts on Monday in Asia/Karachi, not UTC.

drop view if exists public.weekly_leaderboard;

create view public.weekly_leaderboard
with (security_invoker = false) as
select
  d.user_id as user_id,
  coalesce(nullif(p.display_name, ''), 'Walker') as display_name,
  coalesce(sum(d.eligible_total), 0)::bigint as week_steps
from public.daily_steps d
left join public.profiles p
  on p.id = d.user_id
where d.local_date >= (date_trunc('week', (now() at time zone 'Asia/Karachi')))::date
group by d.user_id, p.display_name
having coalesce(sum(d.eligible_total), 0) > 0
order by week_steps desc
limit 50;

grant select on public.weekly_leaderboard to anon, authenticated;
