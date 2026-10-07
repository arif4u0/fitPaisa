/**
 * redeem-product — spends the user's available FitCoins on a catalog product.
 *
 * POST body:
 *   { product_id, recipient_name, recipient_phone, recipient_address }
 *
 * Auth: user JWT (Authorization: Bearer <access_token>).
 * All money logic runs atomically in the public.redeem_product() RPC
 * (stock check + balance check + coin spend + stock decrement + order row).
 * The phone never computes coins.
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

  const productId = body["product_id"] as string | undefined;
  const recipientName = (body["recipient_name"] as string | undefined ?? "").trim();
  const recipientPhone = (body["recipient_phone"] as string | undefined ?? "").trim();
  const recipientAddress = (body["recipient_address"] as string | undefined ?? "").trim();

  if (!productId) return json({ error: "missing_product_id" }, 400);
  if (!recipientName || !recipientPhone || !recipientAddress) {
    return json({ error: "missing_recipient_details" }, 400);
  }

  const db = createClient(SUPABASE_URL, SERVICE_KEY, {
    auth: { persistSession: false },
  });

  const { data, error } = await db.rpc("redeem_product", {
    p_user_id: userId,
    p_product_id: productId,
    p_recipient_name: recipientName,
    p_recipient_phone: recipientPhone,
    p_recipient_address: recipientAddress,
  });

  if (error) {
    const msg = error.message ?? "";
    if (msg.includes("product_not_available")) return json({ error: "product_not_available" }, 400);
    if (msg.includes("product_out_of_stock")) return json({ error: "product_out_of_stock" }, 400);
    if (msg.includes("insufficient_coins")) return json({ error: "insufficient_coins" }, 400);
    return json({ error: "redeem_failed" }, 500);
  }

  return json({ ok: true, redemption_id: data });
});
