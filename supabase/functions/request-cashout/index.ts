/**
 * request-cashout — files a manual cashout request (founder pays out by hand).
 *
 * POST body:
 *   { coins, method: 'easypaisa'|'jazzcash'|'bank', account_title, account_number }
 *
 * Auth: user JWT (Authorization: Bearer <access_token>).
 * Minimum 10,000 coins (= Rs 100). Coins are reserved immediately via the
 * atomic public.request_cashout() RPC. The founder reviews in the admin
 * screen: marks paid (after sending money) or rejected (coins refunded).
 */
import { serve } from "https://deno.land/std@0.208.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const MIN_COINS = 10000;

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

  let body: Record<string, unknown>;
  try {
    body = await req.json();
  } catch {
    return json({ error: "bad_json" }, 400);
  }

  const coins = Number(body["coins"] ?? 0);
  const method = String(body["method"] ?? "");
  const accountTitle = String(body["account_title"] ?? "").trim();
  const accountNumber = String(body["account_number"] ?? "").trim();

  if (!Number.isInteger(coins) || coins < MIN_COINS) {
    return json({ error: "below_minimum", min_coins: MIN_COINS }, 400);
  }
  if (!["easypaisa", "jazzcash", "bank"].includes(method)) {
    return json({ error: "bad_method" }, 400);
  }
  if (!accountTitle || !accountNumber) {
    return json({ error: "missing_account_details" }, 400);
  }

  const db = createClient(SUPABASE_URL, SERVICE_KEY, {
    auth: { persistSession: false },
  });

  const { data, error } = await db.rpc("request_cashout", {
    p_user_id: userId,
    p_coins: coins,
    p_method: method,
    p_account_title: accountTitle,
    p_account_number: accountNumber,
  });

  if (error) {
    const msg = error.message ?? "";
    if (msg.includes("below_minimum")) return json({ error: "below_minimum", min_coins: MIN_COINS }, 400);
    if (msg.includes("insufficient_coins")) return json({ error: "insufficient_coins" }, 400);
    if (msg.includes("bad_method")) return json({ error: "bad_method" }, 400);
    return json({ error: "cashout_failed" }, 500);
  }

  return json({
    ok: true,
    request_id: data,
    rupees: (coins / 100).toFixed(2),
  });
});
