-- 014: Daily Ads — one server-counted row per watched ad.
--
-- Why a table: the daily ad limit must be enforced on the server, not
-- on the phone, or any user could edit the count and farm coins.
-- The watch-ad Edge Function counts rows here for the current
-- Asia/Karachi day and refuses once the user's level limit is reached.
--
-- Coins per ad follow the level table: floor(10 * ad_bonus_multiplier),
-- matching EconomyConfig.adCoinsForLevel in the app.

create table if not exists public.ad_watches (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  local_date date not null,
  coins integer not null,
  created_at timestamptz not null default now()
);

create index if not exists ad_watches_user_date
  on public.ad_watches (user_id, local_date);

alter table public.ad_watches enable row level security;

drop policy if exists "ad_watches_owner_read" on public.ad_watches;
create policy "ad_watches_owner_read" on public.ad_watches
  for select using (auth.uid() = user_id);
