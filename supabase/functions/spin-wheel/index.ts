/**
 * spin-wheel — awards a lucky-spin prize (server-side, weighted random).
 *
 * POST body: { local_date, spin_type: 'free' | 'ad' }
 *
 * Rules:
 *   - 'free': exactly 1 per user per day. Extra calls return already_used.
 *   - 'ad': unlimited, but the app must show a rewarded ad BEFORE calling.
 *     (The ad SDK is client-side; prize values are small by design.)
 *   - Prize is picked server-side with weighted randomness.
 *   - Coins are 'available' immediately.
 */
import { serve } from "https://deno.land/std@0.208.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

// [prize_coins, weight]
const PRIZES: Array<[number, number]> = [
  [5, 30],
  [10, 25],
  [15, 20],
  [25, 12],
  [50, 8],
  [100, 5],
];

function pickPrize(): number {
  const total = PRIZES.reduce((s, [, w]) => s + w, 0);
  let r = Math.random() * total;
  for (const [prize, weight] of PRIZES) {
    r -= weight;
    if (r <= 0) return prize;
  }
  return PRIZES[0][0];
}

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
  const svc = createClient(SUPABASE_URL, SERVICE_KEY, { auth: { persistSession: false } });
  const { data: userData, error: userErr } = await svc.auth.getUser(jwt);
  if (userErr || !userData.user) return json({ error: "unauthorized" }, 401);
  const userId = userData.user.id;

  let body: Record<string, unknown>;
  try {
    body = await req.json();
  } catch {
    return json({ error: "bad_json" }, 400);
  }
  const localDate = String(body.local_date ?? "");
  const spinType = String(body.spin_type ?? "");
  if (!/^\d{4}-\d{2}-\d{2}$/.test(localDate)) return json({ error: "invalid_date" }, 400);
  if (spinType !== "free" && spinType !== "ad") return json({ error: "invalid_spin_type" }, 400);

  const db = svc;

  // Free spin: one per day.
  if (spinType === "free") {
    const { data: existing } = await db
      .from("spin_history")
      .select("id")
      .eq("user_id", userId)
      .eq("local_date", localDate)
      .eq("spin_type", "free")
      .maybeSingle();
    if (existing) return json({ ok: true, already_used: true, prize_coins: 0 });
  }

  const prize = pickPrize();

  await db.from("spin_history").insert({
    user_id: userId,
    local_date: localDate,
    prize_coins: prize,
    spin_type: spinType,
  });

  await db.from("ledger_entries").insert({
    user_id: userId,
    amount_coins: prize,
    state: "available",
    source_type: "spin",
    source_id: `spin:${userId}:${localDate}:${spinType}:${Date.now()}`,
    rule_version: 1,
    available_at: new Date().toISOString(),
    metadata: { spin_type: spinType, prize_coins: prize },
  });

  return json({ ok: true, already_used: false, prize_coins: prize });
});
