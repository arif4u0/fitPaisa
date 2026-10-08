-- 009: referral system.
--
-- Each user gets a unique referral code. When a new user signs up with
-- someone's code, both get bonus coins (one-time, server-credited).

-- Add referral columns to profiles.
alter table public.profiles
  add column if not exists referral_code text unique,
  add column if not exists referred_by uuid references public.profiles(id),
  add column if not exists referral_bonus_claimed boolean not null default false;

-- Generate codes for existing users (8-char uppercase alphanumeric).
update public.profiles
set referral_code = upper(substring(md5(random()::text) from 1 for 8))
where referral_code is null;

-- Function to generate a referral code for new users.
create or replace function public.generate_referral_code()
returns text
language plpgsql
as $$
declare
  code text;
begin
  loop
    code := upper(substring(md5(random()::text || clock_timestamp()::text) from 1 for 8));
    exit when not exists (select 1 from public.profiles where referral_code = code);
  end loop;
  return code;
end;
$$;

-- Trigger: auto-assign a referral code when a profile is created.
drop trigger if exists profiles_set_referral_code on public.profiles;
create or replace function public.set_referral_code()
returns trigger
language plpgsql
as $$
begin
  if new.referral_code is null then
    new.referral_code := public.generate_referral_code();
  end if;
  return new;
end;
$$;
create trigger profiles_set_referral_code
  before insert on public.profiles
  for each row execute function public.set_referral_code();

comment on column public.profiles.referral_code is 'Unique code the user shares; new users enter it to give both sides bonus coins.';
comment on column public.profiles.referred_by is 'The referrer''s profile id, if this user signed up with a code.';
