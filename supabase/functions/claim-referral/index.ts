/**
 * claim-referral — applies a referral code for the signed-in user.
 *
 * POST body:
 *   { code: "AB12CD34" }
 *
 * Auth: user JWT (Authorization: Bearer <access_token>).
 * Rules (all enforced server-side):
 *   - Code must exist and not be the user's own.
 *   - Each user can claim only once (referral_bonus_claimed).
 *   - Both referrer and new user get REFERRAL_BONUS_COINS (ledger credit).
 */
import { serve } from "https://deno.land/std@0.208.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const REFERRAL_BONUS_COINS = 500; // Rs 5 each

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

async function getUserId(req: Request): Promise<string | null> {
  const authHeader = req.headers.get("Authorization");
  if (!authHeader) return null;
  const jwt = authHeader.replace(/^Bearer\s+/i, "");
  if (!jwt) return null;
  const service = createClient(SUPABASE_URL, SERVICE_KEY, {
    auth: { persistSession: false },
  });
  const { data, error } = await service.auth.getUser(jwt);
  if (error || !data.user) return null;
  return data.user.id;
}

serve(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  const userId = await getUserId(req);
  if (!userId) return json({ error: "unauthorized" }, 401);

  let code: string;
  try {
    const body = await req.json();
    code = (body.code as string || "").trim().toUpperCase();
  } catch {
    return json({ error: "invalid_body" }, 400);
  }
  if (!code) return json({ error: "code_required" }, 400);

  const service = createClient(SUPABASE_URL, SERVICE_KEY, {
    auth: { persistSession: false },
  });

  // Load the claimer's profile.
  const { data: claimer, error: claimerErr } = await service
    .from("profiles")
    .select("id, referral_bonus_claimed, referral_code")
    .eq("id", userId)
    .single();
  if (claimerErr || !claimer) return json({ error: "profile_not_found" }, 404);
  if (claimer.referral_bonus_claimed) {
    return json({ error: "already_claimed" }, 409);
  }

  // Find the referrer by code.
  const { data: referrer, error: referrerErr } = await service
    .from("profiles")
    .select("id")
    .eq("referral_code", code)
    .single();
  if (referrerErr || !referrer) return json({ error: "invalid_code" }, 404);
  if (referrer.id === userId) return json({ error: "cannot_refer_self" }, 400);

  // Credit both sides via ledger entries (available immediately, no hold
  // for bonus coins — they're marketing cost, not step earnings).
  const now = new Date().toISOString();
  const entries = [
    {
      user_id: userId,
      coins: REFERRAL_BONUS_COINS,
      entry_type: "referral_bonus",
      note: `Referral bonus (code ${code})`,
      created_at: now,
    },
    {
      user_id: referrer.id,
      coins: REFERRAL_BONUS_COINS,
      entry_type: "referral_reward",
      note: `Referral reward (invited ${userId.slice(0, 8)}…)`,
      created_at: now,
    },
  ];
  const { error: ledgerErr } = await service.from("ledger_entries").insert(entries);
  if (ledgerErr) return json({ error: "ledger_failed", detail: ledgerErr.message }, 500);

  // Mark claimed and record who referred them.
  const { error: updateErr } = await service
    .from("profiles")
    .update({ referral_bonus_claimed: true, referred_by: referrer.id })
    .eq("id", userId);
  if (updateErr) return json({ error: "update_failed", detail: updateErr.message }, 500);

  return json({
    ok: true,
    bonus_coins: REFERRAL_BONUS_COINS,
  });
});
