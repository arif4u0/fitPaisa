/**
 * watch-ad — credits coins for one fully watched rewarded ad.
 *
 * POST body: {} (empty — the app calls this only after the ad SDK
 * reports the ad was watched to the end).
 *
 * Auth: user JWT (Authorization: Bearer <access_token>).
 *
 * Rules (all enforced here, never on the phone):
 *   - Daily limit comes from the user's level row (levels.daily_ad_limit):
 *     Level 1 = 5, Level 2 = 6, Level 3 = 8, Level 4 = 10, Level 5 = 12.
 *   - Coins per ad = floor(10 * levels.ad_bonus_multiplier):
 *     Level 1 = 10, Level 2 = 11, Level 3 = 12, Level 4 = 15, Level 5 = 20.
 *   - The day is Asia/Karachi, counted from ad_watches rows.
 *   - Coins are 'available' immediately (small instant reward, like spins).
 *   - This counter is only for the Daily Ads button. Convert ads and
 *     Spin ad-spins are separate and are not counted here.
 */
import { serve } from "https://deno.land/std@0.208.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

serve(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  const authHeader = req.headers.get("Authorization");
  if (!authHeader) return json({ error: "no_auth_header" }, 401);
  const jwt = authHeader.replace(/^Bearer\s+/i, "");
  const db = createClient(SUPABASE_URL, SERVICE_KEY, {
    auth: { persistSession: false },
  });
  const { data: userData, error: userErr } = await db.auth.getUser(jwt);
  if (userErr || !userData.user) return json({ error: "unauthorized" }, 401);
  const userId = userData.user.id;

  // Today in Pakistan, whatever the phone's timezone says.
  const today = new Date().toLocaleDateString("en-CA", {
    timeZone: "Asia/Karachi",
  });

  // Profile (create it if the sign-in trigger never ran).
  let { data: profile } = await db
    .from("profiles")
    .select("level")
    .eq("id", userId)
    .maybeSingle();
  if (!profile) {
    await db.from("profiles").upsert(
      { id: userId, level: 1, lifetime_steps: 0 },
      { onConflict: "id", ignoreDuplicates: true },
    );
    profile = { level: 1 };
  }
  const userLevel: number = (profile?.level as number) ?? 1;

  const { data: levelRow } = await db
    .from("levels")
    .select("daily_ad_limit, ad_bonus_multiplier")
    .eq("level", userLevel)
    .maybeSingle();
  const dailyLimit: number = (levelRow?.daily_ad_limit as number) ?? 5;
  const multiplier: number = Number(levelRow?.ad_bonus_multiplier ?? 1);
  const coinsPerAd: number = Math.floor(10 * multiplier);

  const { data: watchedRows } = await db
    .from("ad_watches")
    .select("id")
    .eq("user_id", userId)
    .eq("local_date", today);
  const watchedToday: number = (watchedRows ?? []).length;

  if (watchedToday >= dailyLimit) {
    return json({
      ok: false,
      error: "limit_reached",
      watched_today: watchedToday,
      daily_ad_limit: dailyLimit,
      coins_per_ad: coinsPerAd,
    });
  }

  const { data: cfg } = await db
    .from("app_config")
    .select("value")
    .eq("key", "rule_version")
    .single();
  const ruleVersion: number = (cfg?.value as number) ?? 2;

  const { error: watchErr } = await db.from("ad_watches").insert({
    user_id: userId,
    local_date: today,
    coins: coinsPerAd,
  });
  if (watchErr) return json({ error: "watch_failed" }, 500);

  await db.from("ledger_entries").insert({
    user_id: userId,
    amount_coins: coinsPerAd,
    state: "available",
    source_type: "ad",
    source_id: `ad:${userId}:${today}:${watchedToday + 1}`,
    rule_version: ruleVersion,
    available_at: new Date().toISOString(),
    metadata: {
      level: userLevel,
      daily_ad_limit: dailyLimit,
      ad_number_today: watchedToday + 1,
    },
  });

  return json({
    ok: true,
    coins: coinsPerAd,
    watched_today: watchedToday + 1,
    daily_ad_limit: dailyLimit,
    coins_per_ad: coinsPerAd,
    remaining: dailyLimit - (watchedToday + 1),
  });
});
