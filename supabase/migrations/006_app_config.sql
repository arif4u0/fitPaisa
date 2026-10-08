-- 006: in-app update checker config (for sideloaded APKs, not Play Store).
--
-- The app reads these keys on startup and prompts the user when a newer
-- version is available. The founder updates the row + uploads the APK to
-- the 'app-releases' storage bucket whenever a new version is published.

create table if not exists public.app_config (
  key text primary key,
  value text not null,
  updated_at timestamptz not null default now()
);

insert into public.app_config (key, value) values
  ('latest_version', '1.0.0'),
  ('apk_url', ''),
  ('changelog', ''),
  ('force_update', 'false')
on conflict (key) do nothing;

alter table public.app_config enable row level security;

drop policy if exists "app_config_public_read" on public.app_config;
create policy "app_config_public_read" on public.app_config
  for select using (true);

comment on table public.app_config is
  'In-app update checker: latest_version (e.g. 1.1.0), apk_url (public APK link), changelog, force_update (true/false).';
