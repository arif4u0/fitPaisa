/**
 * get-balance — returns the user's coin balance.
 *
 * POST body: {} (empty)
 *
 * Auth: user JWT (Authorization: Bearer <access_token>).
 * Secrets used: SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_SERVICE_ROLE_KEY
 *   (function secrets — never inside the APK).
 *
 * Logic:
 *   1. Release any matured 'held' coins (hold expired -> 'available').
 *      This keeps balances self-maintaining with no cron job.
 *   2. Returns available_coins (spendable now) and maturing_coins
 *      (held coins whose hold has not expired yet).
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

async function getUserId(req: Request): Promise<{ userId: string | null; reason: string }> {
  const authHeader = req.headers.get("Authorization");
  if (!authHeader) return { userId: null, reason: "no_auth_header" };
  const jwt = authHeader.replace(/^Bearer\s+/i, "");
  if (!jwt) return { userId: null, reason: "empty_bearer_token" };
  // Verify the JWT with the service-role client and the explicit token.
  // (Does not depend on the anon key, which is auto-injected and never set manually.)
  const service = createClient(SUPABASE_URL, SERVICE_KEY, {
    auth: { persistSession: false },
  });
  const { data, error } = await service.auth.getUser(jwt);
  if (error) return { userId: null, reason: "jwt_rejected: " + error.message };
  if (!data.user) return { userId: null, reason: "jwt_valid_but_no_user" };
  return { userId: data.user.id, reason: "ok" };
}

serve(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  const { userId, reason } = await getUserId(req);
  if (!userId) return json({ error: reason }, 401);

  const db = createClient(SUPABASE_URL, SERVICE_KEY, {
    auth: { persistSession: false },
  });

  // Release matured holds first (no cron needed).
  await db.rpc("release_matured_coins", { p_user_id: userId });

  // Spendable balance (canonical definition, matches cashout reservation).
  const { data: available } = await db.rpc("user_available_coins", {
    p_user_id: userId,
  });

  // Coins still maturing inside the reward hold window.
  const { data: maturingRows } = await db
    .from("ledger_entries")
    .select("amount_coins")
    .eq("user_id", userId)
    .eq("state", "held");

  const maturing: number = (maturingRows ?? []).reduce(
    (sum: number, r: { amount_coins: number }) => sum + (r.amount_coins as number),
    0,
  );

  return json({
    ok: true,
    available_coins: (available as number) ?? 0,
    maturing_coins: maturing,
  });
});
