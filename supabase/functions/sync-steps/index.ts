/**
 * sync-steps — validates a daily step sync and posts a held coin credit.
 *
 * POST body:
 *   { local_date, observed_total, timezone, app_version, install_id,
 *     source_summary, idempotency_key }
 *
 * Auth: user JWT (Authorization: Bearer <access_token>).
 * Secrets used: SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_SERVICE_ROLE_KEY
 *   (function secrets — never inside the APK).
 *
 * Logic:
 *   - The user's CURRENT level (from profiles) decides the daily_step_cap.
 *   - eligible = min(observed, level_cap); manual-entry data is excluded.
 *   - new_steps = max(0, eligible - already_credited)
 *   - coins = floor(new_steps / steps_per_coin), posted ONCE per
 *     (user, date, rule_version) via UNIQUE(source_type, source_id).
 *     Replays credit zero.
 *   - profiles.lifetime_steps += new_steps; level recomputed from thresholds.
 *   - Coins are posted 'held' and mature after reward_hold_days (7).
 */
import { serve } from "https://deno.land/std@0.208.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY")!;
const SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function serviceClient() {
  return createClient(SUPABASE_URL, SERVICE_KEY, {
    auth: { persistSession: false },
  });
}

async function getUserId(req: Request): Promise<string | null> {
  const auth = req.headers.get("Authorization");
  if (!auth) return null;
  const anon = createClient(SUPABASE_URL, ANON_KEY, {
    global: { headers: { Authorization: auth } },
    auth: { persistSession: false },
  });
  const { data, error } = await anon.auth.getUser();
  if (error || !data.user) return null;
  return data.user.id;
}

serve(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  const userId = await getUserId(req);
  if (!userId) return json({ error: "unauthorized" }, 401);

  let body: Record<string, unknown>;
  try {
    body = await req.json();
  } catch {
    return json({ error: "bad_json" }, 400);
  }

  const {
    local_date: localDate,
    observed_total: observedTotal,
    timezone,
    app_version: appVersion,
    source_summary: sourceSummary,
    idempotency_key: idempotencyKey,
  } = body as {
    local_date: string;
    observed_total: number;
    timezone?: string;
    app_version?: string;
    source_summary?: Record<string, unknown>;
    idempotency_key: string;
  };

  if (!localDate || !Number.isInteger(observedTotal) || observedTotal < 0 || !idempotencyKey) {
    return json({ error: "invalid_payload" }, 400);
  }

  const db = serviceClient();

  // Idempotency: a retried sync with the same key is a no-op.
  const { data: seen } = await db
    .from("step_sync_events")
    .select("id")
    .eq("idempotency_key", idempotencyKey)
    .maybeSingle();
  if (seen) return json({ ok: true, deduped: true });

  // Active rule version from app_config.
  const { data: cfg } = await db
    .from("app_config")
    .select("value")
    .eq("key", "rule_version")
    .single();
  const ruleVersion: number = (cfg?.value as number) ?? 1;
  const { data: rule, error: ruleErr } = await db
    .from("reward_rules")
    .select("*")
    .eq("version", ruleVersion)
    .single();
  if (ruleErr || !rule) return json({ error: "no_active_rule" }, 500);

  // User's current level -> per-level daily step cap.
  const { data: profile } = await db
    .from("profiles")
    .select("level, lifetime_steps")
    .eq("id", userId)
    .maybeSingle();
  const userLevel: number = (profile?.level as number) ?? 1;
  const { data: levelRow } = await db
    .from("levels")
    .select("daily_step_cap")
    .eq("level", userLevel)
    .maybeSingle();
  const dailyCap: number =
    (levelRow?.daily_step_cap as number) ?? (rule.daily_step_cap as number);

  // Anti-abuse: manually entered records are not eligible for rewards.
  const manual = (sourceSummary as Record<string, unknown> | undefined)?.manual === true;
  const eligibleTotal = manual ? 0 : Math.min(observedTotal, dailyCap);

  // Current credited progress for this local date.
  const { data: day } = await db
    .from("daily_steps")
    .select("credited_steps")
    .eq("user_id", userId)
    .eq("local_date", localDate)
    .maybeSingle();
  const alreadyCredited: number = day?.credited_steps ?? 0;
  const newSteps = Math.max(0, eligibleTotal - alreadyCredited);
  const coins = Math.floor(newSteps / (rule.steps_per_coin as number));

  // Append sync evidence (unique idempotency_key guards double-submit races).
  const { error: evErr } = await db.from("step_sync_events").insert({
    user_id: userId,
    local_date: localDate,
    idempotency_key: idempotencyKey,
    observed_total: observedTotal,
    eligible_total: eligibleTotal,
    source_summary: sourceSummary ?? {},
    app_version: appVersion ?? null,
  });
  if (evErr && (evErr as { code?: string }).code === "23505") {
    return json({ ok: true, deduped: true });
  }
  if (evErr) return json({ error: "sync_event_failed" }, 500);

  await db.from("daily_steps").upsert(
    {
      user_id: userId,
      local_date: localDate,
      observed_total: observedTotal,
      eligible_total: eligibleTotal,
      credited_steps: alreadyCredited + newSteps,
      timezone: timezone ?? "Asia/Karachi",
      updated_at: new Date().toISOString(),
    },
    { onConflict: "user_id,local_date" },
  );

  // Post the held credit once. UNIQUE(source_type, source_id) makes replays
  // (same day + rule version) credit zero instead of double-paying.
  let ledgerId: string | null = null;
  if (coins > 0) {
    const holdDays = rule.reward_hold_days as number;
    const availableAt = new Date(Date.now() + holdDays * 86400_000).toISOString();
    const { data: entry, error: leErr } = await db
      .from("ledger_entries")
      .insert({
        user_id: userId,
        amount_coins: coins,
        state: "held",
        source_type: "steps",
        source_id: `steps:${userId}:${localDate}:v${ruleVersion}`,
        rule_version: ruleVersion,
        available_at: availableAt,
        metadata: {
          observed_total: observedTotal,
          eligible_total: eligibleTotal,
          new_steps: newSteps,
          level: userLevel,
          daily_cap: dailyCap,
        },
      })
      .select("id")
      .maybeSingle();
    if (!leErr) ledgerId = entry?.id ?? null;
    // 23505 here = this day/rule was already credited; treat as replay, no error.
  }

  // Advance lifetime steps and recompute level from thresholds.
  if (newSteps > 0) {
    const newLifetime: number = ((profile?.lifetime_steps as number) ?? 0) + newSteps;
    const { data: newLevelRow } = await db
      .from("levels")
      .select("level")
      .lte("lifetime_steps_threshold", newLifetime)
      .order("level", { ascending: false })
      .limit(1)
      .maybeSingle();
    const newLevel: number = (newLevelRow?.level as number) ?? userLevel;
    await db
      .from("profiles")
      .update({ lifetime_steps: newLifetime, level: newLevel })
      .eq("id", userId);
  }

  return json({
    ok: true,
    eligible_total: eligibleTotal,
    new_steps: newSteps,
    coins_held: coins,
    ledger_id: ledgerId,
    level: userLevel,
    daily_cap: dailyCap,
  });
});
