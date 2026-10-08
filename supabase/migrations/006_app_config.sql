-- 006 (fixed): in-app update checker config.
-- Drops any broken earlier version of the table, then recreates it cleanly.

drop table if exists public.app_config;

create table public.app_config (
  key text primary key,
  value text not null,
  updated_at timestamptz not null default now()
);

insert into public.app_config (key, value) values
  ('latest_version', '1.0.0'),
  ('apk_url', ''),
  ('changelog', ''),
  ('force_update', 'false');

alter table public.app_config enable row level security;

drop policy if exists "app_config_public_read" on public.app_config;
create policy "app_config_public_read" on public.app_config
  for select using (true);
