/**
 * delete-account — permanently deletes the user's account and all data.
 *
 * POST body: { confirm: true }
 *
 * Uses the service-role key to delete the auth user. All app tables
 * reference auth.users with ON DELETE CASCADE, so profiles, ledger
 * entries, daily steps, spins, achievements, etc. are removed too.
 * This cannot be undone.
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
  if (body.confirm !== true) return json({ error: "confirm_required" }, 400);

  // Delete the auth user; ON DELETE CASCADE removes all app data.
  const { error: delErr } = await svc.auth.admin.deleteUser(userId);
  if (delErr) return json({ error: "delete_failed: " + delErr.message }, 500);

  return json({ ok: true });
});
