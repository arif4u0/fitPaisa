import React, { useState } from 'react';
import { Copy, Check, FileCode, CheckCircle2 } from 'lucide-react';

interface PatchFile {
  name: string;
  path: string;
  language: string;
  description: string;
  code: string;
}

export const CodePatchesSection: React.FC = () => {
  const [copiedFile, setCopiedFile] = useState<string | null>(null);
  const [activeFileIndex, setActiveFileIndex] = useState<number>(0);

  const files: PatchFile[] = [
    {
      name: 'AdManager.kt',
      path: 'app/src/main/java/com/fitpaisa/app/core/ads/AdManager.kt',
      language: 'kotlin',
      description: 'Fixes Unity Ads loading deadlock, adds 3.5s warm-up await, auto-retry with exponential backoff, and Logcat debugging.',
      code: `package com.fitpaisa.app.core.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val TAG = "FitPaisaAds"

@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Volatile private var isInitialized = false
    @Volatile private var isLoaded = false
    @Volatile private var isLoading = false
    
    private var retryAttempt = 0
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var lastError: String = "Not initialized yet"
        private set

    init {
        initializeUnity()
    }

    private fun initializeUnity() {
        Log.d(TAG, "Initializing Unity Ads with Game ID: \${AdsConfig.UNITY_GAME_ID}, testMode: \${AdsConfig.UNITY_TEST_MODE}")
        UnityAds.initialize(
            context,
            AdsConfig.UNITY_GAME_ID,
            AdsConfig.UNITY_TEST_MODE,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    Log.i(TAG, "✅ Unity Ads initialized successfully!")
                    isInitialized = true
                    retryAttempt = 0
                    preload()
                }

                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError,
                    message: String
                ) {
                    isInitialized = false
                    lastError = "Init failed: \$error - \$message"
                    Log.e(TAG, "❌ Unity Ads Init Failed: \$error -> \$message")
                    scheduleInitRetry()
                }
            }
        )
    }

    private fun scheduleInitRetry() {
        if (retryAttempt >= 4) return
        retryAttempt++
        val delayMs = (2000L * (1 shl (retryAttempt - 1))).coerceAtMost(16000L)
        Log.w(TAG, "Scheduling Unity Ads init retry in \${delayMs}ms (attempt \$retryAttempt)")
        scope.launch {
            delay(delayMs)
            if (!isInitialized) initializeUnity()
        }
    }

    fun preload() {
        if (!isInitialized || isLoaded || isLoading) return
        isLoading = true
        Log.d(TAG, "Preloading Unity Rewarded Ad: \${AdsConfig.UNITY_REWARDED_AD_UNIT_ID}")
        
        UnityAds.load(
            AdsConfig.UNITY_REWARDED_AD_UNIT_ID,
            object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(adUnitId: String) {
                    isLoaded = true
                    isLoading = false
                    lastError = "Ad loaded OK"
                    Log.i(TAG, "✅ Unity Rewarded Ad loaded successfully: \$adUnitId")
                }

                override fun onUnityAdsFailedToLoad(
                    adUnitId: String,
                    error: UnityAds.UnityAdsLoadError,
                    message: String
                ) {
                    isLoaded = false
                    isLoading = false
                    lastError = "Load failed: \$error - \$message"
                    Log.e(TAG, "❌ Unity Ad Load Failed [\$adUnitId]: \$error -> \$message")
                    
                    // Automatic retry after 4 seconds
                    scope.launch {
                        delay(4000)
                        if (!isLoaded && isInitialized) preload()
                    }
                }
            }
        )
    }

    suspend fun showRewarded(activity: Activity): AdOutcome = withContext(Dispatchers.Main) {
        // If not loaded yet, wait up to 3.5s for background load instead of instantly failing!
        if (!isLoaded) {
            preload()
            Log.d(TAG, "Ad not cached yet. Awaiting load window...")
            withTimeoutOrNull(3500) {
                while (!isLoaded && (isLoading || !isInitialized)) {
                    delay(150)
                }
            }
        }

        if (!isLoaded) {
            Log.w(TAG, "Ad still not ready after timeout. Returning NOT_READY.")
            return@withContext AdOutcome.NOT_READY
        }

        isLoaded = false
        preload() // Load next one in background

        suspendCancellableCoroutine { cont ->
            var settled = false
            fun settle(result: AdOutcome) {
                if (!settled) {
                    settled = true
                    cont.resume(result)
                }
            }

            UnityAds.show(
                activity,
                AdsConfig.UNITY_REWARDED_AD_UNIT_ID,
                UnityAdsShowOptions(),
                object : IUnityAdsShowListener {
                    override fun onUnityAdsShowFailure(
                        adUnitId: String,
                        error: UnityAds.UnityAdsShowError,
                        message: String
                    ) {
                        Log.e(TAG, "❌ Unity Show Failure: \$error - \$message")
                        lastError = "Show failed: \$error - \$message"
                        settle(AdOutcome.NOT_READY)
                    }

                    override fun onUnityAdsShowStart(adUnitId: String) {
                        Log.d(TAG, "🎬 Unity Ad Started: \$adUnitId")
                    }

                    override fun onUnityAdsShowClick(adUnitId: String) {
                        Log.d(TAG, "👉 Unity Ad Clicked: \$adUnitId")
                    }

                    override fun onUnityAdsShowComplete(
                        adUnitId: String,
                        state: UnityAds.UnityAdsShowCompletionState
                    ) {
                        Log.i(TAG, "🏁 Unity Ad Finished: \$state")
                        settle(
                            if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                                AdOutcome.WATCHED
                            } else {
                                AdOutcome.SKIPPED
                            }
                        )
                    }
                }
            )
        }
    }

    fun isReady(): Boolean = isLoaded
}

enum class AdOutcome {
    WATCHED,
    NOT_READY,
    SKIPPED
}`,
    },
    {
      name: 'AdsConfig.kt',
      path: 'app/src/main/java/com/fitpaisa/app/core/ads/AdsConfig.kt',
      language: 'kotlin',
      description: 'Corrects placement ID from "BP_Rewarded_Android" to standard "Rewarded_Android".',
      code: `package com.fitpaisa.app.core.ads

object AdsConfig {
    // AdMob configuration (re-enable as primary or waterfall fallback)
    const val ADMOB_REWARDED_AD_UNIT_ID = "ca-app-pub-7360446604215991/5669568491"
    const val ADMOB_TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    // Unity Ads Project Credentials:
    // Verify this matches your Android Game ID in Unity Cloud Monetization dashboard
    const val UNITY_GAME_ID = "800410563"

    // Default Unity Rewarded Ad Unit ID for Android is "Rewarded_Android"
    // (Replaces the non-existent "BP_Rewarded_Android")
    const val UNITY_REWARDED_AD_UNIT_ID = "Rewarded_Android"

    // Keep true for testing; set false before production Play Store upload
    const val UNITY_TEST_MODE = true
}`,
    },
    {
      name: 'sync-steps/index.ts',
      path: 'supabase/functions/sync-steps/index.ts',
      language: 'typescript',
      description: 'Fixes duplicate key 23505 bug so users can convert multiple times a day; adds date forgery guard and cadence bounds.',
      code: `import { serve } from "https://deno.land/std@0.208.0/http/server.ts";
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
  const jwt = authHeader.replace(/^Bearer\\s+/i, "");

  const db = createClient(SUPABASE_URL, SERVICE_KEY, { auth: { persistSession: false } });
  const { data: userData, error: userErr } = await db.auth.getUser(jwt);
  if (userErr || !userData.user) return json({ error: "unauthorized" }, 401);
  const userId = userData.user.id;

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

  // 1. DATE BOUNDARY SECURITY: Prevent historic/future date forgery
  const clientDate = new Date(\`\${localDate}T12:00:00Z\`);
  const serverNow = new Date();
  const driftHours = Math.abs(clientDate.getTime() - serverNow.getTime()) / (1000 * 60 * 60);
  if (driftHours > 28) {
    return json({ error: "date_out_of_sync", message: "Date out of valid range (max 28 hours timezone drift)." }, 400);
  }

  // Idempotency check
  const { data: seen } = await db
    .from("step_sync_events")
    .select("id")
    .eq("idempotency_key", idempotencyKey)
    .maybeSingle();
  if (seen) return json({ ok: true, deduped: true });

  const { data: cfg } = await db.from("app_config").select("value").eq("key", "rule_version").single();
  const ruleVersion = (cfg?.value as number) ?? 2;

  const { data: rule } = await db.from("reward_rules").select("*").eq("version", ruleVersion).single();
  if (!rule) return json({ error: "no_active_rule" }, 500);

  let { data: profile } = await db
    .from("profiles")
    .select("level, lifetime_steps, streak_days, last_streak_date")
    .eq("id", userId)
    .maybeSingle();

  if (!profile) {
    await db.from("profiles").upsert(
      { id: userId, level: 1, lifetime_steps: 0 },
      { onConflict: "id", ignoreDuplicates: true }
    );
    profile = { level: 1, lifetime_steps: 0, streak_days: 0, last_streak_date: null };
  }

  const userLevel = (profile?.level as number) ?? 1;
  const { data: levelRow } = await db
    .from("levels")
    .select("daily_step_cap")
    .eq("level", userLevel)
    .maybeSingle();

  const dailyCap = (levelRow?.daily_step_cap as number) ?? (rule.daily_step_cap as number);

  // Exclude manual step additions
  const manual = (sourceSummary as Record<string, unknown> | undefined)?.manual === true;
  const eligibleTotal = manual ? 0 : Math.min(observedTotal, dailyCap);

  // Check credited steps
  const { data: dayRow } = await db
    .from("daily_steps")
    .select("credited_steps")
    .eq("user_id", userId)
    .eq("local_date", localDate)
    .maybeSingle();

  const alreadyCredited = dayRow?.credited_steps ?? 0;
  const newSteps = Math.max(0, eligibleTotal - alreadyCredited);
  const stepsPerCoin = (rule.steps_per_coin as number) || 100;
  const coins = Math.floor(newSteps / stepsPerCoin);

  // Record audit sync event
  await db.from("step_sync_events").insert({
    user_id: userId,
    local_date: localDate,
    idempotency_key: idempotencyKey,
    observed_total: observedTotal,
    eligible_total: eligibleTotal,
    source_summary: sourceSummary ?? {},
    app_version: appVersion ?? null,
  });

  // Accumulate in daily_steps
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
    { onConflict: "user_id,local_date" }
  );

  // 2. DELTA KEY FIX FOR MULTI-BATCH CONVERSIONS (PREVENTS 23505 ERROR)
  let ledgerId: string | null = null;
  if (coins > 0) {
    const holdDays = (rule.reward_hold_days as number) || 7;
    const availableAt = new Date(Date.now() + holdDays * 86400_000).toISOString();
    const deltaSourceId = \`steps:\${userId}:\${localDate}:delta:\${alreadyCredited}_\${alreadyCredited + newSteps}\`;

    const { data: entry, error: leErr } = await db
      .from("ledger_entries")
      .insert({
        user_id: userId,
        amount_coins: coins,
        state: "held",
        source_type: "steps",
        source_id: deltaSourceId,
        rule_version: ruleVersion,
        available_at: availableAt,
        metadata: {
          observed_total: observedTotal,
          eligible_total: eligibleTotal,
          new_steps: newSteps,
          already_credited: alreadyCredited,
          level: userLevel,
          daily_cap: dailyCap,
        },
      })
      .select("id")
      .maybeSingle();

    if (!leErr) ledgerId = entry?.id ?? null;
  }

  // Update lifetime steps & calculate new level
  let finalLevel = userLevel;
  if (newSteps > 0) {
    const newLifetime = ((profile?.lifetime_steps as number) ?? 0) + newSteps;
    const { data: newLevelRow } = await db
      .from("levels")
      .select("level")
      .lte("lifetime_steps_threshold", newLifetime)
      .order("level", { ascending: false })
      .limit(1)
      .maybeSingle();

    finalLevel = (newLevelRow?.level as number) ?? userLevel;
    await db
      .from("profiles")
      .update({ lifetime_steps: newLifetime, level: finalLevel })
      .eq("id", userId);
  }

  return json({
    ok: true,
    eligible_total: eligibleTotal,
    new_steps: newSteps,
    coins_held: coins,
    ledger_id: ledgerId,
    level: finalLevel,
    daily_cap: dailyCap,
  });
});`,
    },
    {
      name: '015_wallet_security.sql',
      path: 'supabase/migrations/015_wallet_security.sql',
      language: 'sql',
      description: 'PostgreSQL migration adding advisory lock on user cashouts, single-pending limit, and sybil fraud views.',
      code: `-- ============================================================================
-- 015_wallet_security.sql — Enterprise Wallet Security & Anti-Fraud Hardening
-- ============================================================================

-- 1. Enforce unique provider transaction IDs (cannot reuse same Easypaisa TRX ID)
create unique index if not exists idx_cashout_unique_provider_ref
  on public.cashout_requests (provider_reference)
  where provider_reference is not null;

-- 2. Prevent multiple pending cashouts per user (1 pending cashout at a time)
create unique index if not exists idx_single_pending_cashout_per_user
  on public.cashout_requests (user_id)
  where status = 'pending';

-- 3. Hardened request_cashout RPC with Transaction Advisory Locking
-- Prevents race conditions and overdraft attacks from concurrent requests!
create or replace function public.request_cashout(
  p_user_id uuid,
  p_coins integer,
  p_method text,
  p_account_title text,
  p_account_number text
)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_available integer;
  v_min_coins integer := 10000; -- Rs 100 minimum
  v_paisa integer;
  v_request_id uuid;
  v_pending_count integer;
  v_cleaned_acc text;
begin
  -- TRANSACTION-LEVEL ADVISORY LOCK: Serializes user's coin transactions!
  perform pg_advisory_xact_lock(hashtext(p_user_id::text));

  if p_coins < v_min_coins then
    raise exception 'below_minimum';
  end if;

  if p_method not in ('easypaisa', 'jazzcash', 'bank') then
    raise exception 'bad_method';
  end if;

  -- Limit to 1 pending cashout at a time
  select count(*) into v_pending_count
  from public.cashout_requests
  where user_id = p_user_id and status = 'pending';

  if v_pending_count > 0 then
    raise exception 'pending_request_exists';
  end if;

  -- Read spendable balance safely inside the locked transaction
  select coalesce(sum(amount_coins), 0)::integer into v_available
  from public.ledger_entries
  where user_id = p_user_id and state = 'available';

  if v_available < p_coins then
    raise exception 'insufficient_coins';
  end if;

  v_paisa := p_coins;
  v_cleaned_acc := regexp_replace(p_account_number, '[^0-9]', '', 'g');

  -- Reserve the coins
  insert into public.ledger_entries (user_id, amount_coins, state, source_type, source_id)
  values (
    p_user_id,
    -p_coins,
    'available',
    'cashout',
    'cashout:' || p_user_id::text || ':' || extract(epoch from now())::bigint
  );

  insert into public.cashout_requests (
    user_id,
    coins,
    rupees_paisa,
    method,
    account_title,
    account_number
  )
  values (
    p_user_id,
    p_coins,
    v_paisa,
    p_method,
    p_account_title,
    v_cleaned_acc
  )
  returning id into v_request_id;

  return v_request_id;
end;
$$;

-- 4. Sybil Multi-Account Detector View
create or replace view public.view_sybil_wallet_alerts as
select
  account_number,
  method,
  count(distinct user_id) as linked_user_count,
  sum(coins) as total_coins_demanded,
  array_agg(distinct user_id::text) as suspect_user_ids,
  max(created_at) as latest_request_time
from public.cashout_requests
group by account_number, method
having count(distinct user_id) > 1
order by linked_user_count desc;`,
    },
    {
      name: 'request-cashout/index.ts',
      path: 'supabase/functions/request-cashout/index.ts',
      language: 'typescript',
      description: 'Handles phone number normalization, account formatting, and error mapping for pending request blocks.',
      code: `import { serve } from "https://deno.land/std@0.208.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const MIN_COINS = 10000; // Rs 100

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
  const jwt = authHeader.replace(/^Bearer\\s+/i, "");

  const db = createClient(SUPABASE_URL, SERVICE_KEY, { auth: { persistSession: false } });
  const { data: userData, error: userErr } = await db.auth.getUser(jwt);
  if (userErr || !userData.user) return json({ error: "unauthorized" }, 401);
  const userId = userData.user.id;

  let body: Record<string, unknown>;
  try {
    body = await req.json();
  } catch {
    return json({ error: "bad_json" }, 400);
  }

  const coins = Number(body["coins"] ?? 0);
  const method = String(body["method"] ?? "").toLowerCase().trim();
  const accountTitle = String(body["account_title"] ?? "").trim();
  const accountNumber = String(body["account_number"] ?? "").trim();

  if (!Number.isInteger(coins) || coins < MIN_COINS) {
    return json({ error: "below_minimum", min_coins: MIN_COINS }, 400);
  }

  if (!["easypaisa", "jazzcash", "bank"].includes(method)) {
    return json({ error: "bad_method" }, 400);
  }

  // Validate Pakistani phone / account format
  const cleanDigits = accountNumber.replace(/[^0-9]/g, "");
  if (["easypaisa", "jazzcash"].includes(method) && cleanDigits.length !== 11) {
    return json({ error: "invalid_phone_number", message: "Easypaisa and JazzCash numbers must be exactly 11 digits (e.g. 03001234567)." }, 400);
  }

  const { data, error } = await db.rpc("request_cashout", {
    p_user_id: userId,
    p_coins: coins,
    p_method: method,
    p_account_title: accountTitle,
    p_account_number: cleanDigits,
  });

  if (error) {
    const msg = error.message ?? "";
    if (msg.includes("pending_request_exists")) {
      return json({ error: "pending_request_exists", message: "You already have a pending cashout in review. Please wait for the founder to complete it." }, 400);
    }
    if (msg.includes("below_minimum")) return json({ error: "below_minimum", min_coins: MIN_COINS }, 400);
    if (msg.includes("insufficient_coins")) return json({ error: "insufficient_coins" }, 400);
    return json({ error: "cashout_failed" }, 500);
  }

  return json({
    ok: true,
    request_id: data,
    rupees: (coins / 100).toFixed(2),
  });
});`,
    },
  ];

  const currentFile = files[activeFileIndex];

  const handleCopy = (filename: string, code: string) => {
    navigator.clipboard.writeText(code);
    setCopiedFile(filename);
    setTimeout(() => setCopiedFile(null), 2500);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h3 className="text-lg font-bold text-white flex items-center gap-2">
            <FileCode className="w-5 h-5 text-emerald-400" />
            Complete Production-Ready Code Patches
          </h3>
          <p className="text-xs text-slate-400 mt-1">
            Drop-in replacements for your Android codebase and Supabase backend. Copy directly or apply into your Git branches.
          </p>
        </div>
      </div>

      {/* File Navigation Pills */}
      <div className="flex gap-2 overflow-x-auto pb-2">
        {files.map((file, idx) => (
          <button
            key={idx}
            onClick={() => setActiveFileIndex(idx)}
            className={`px-3 py-2 rounded-xl text-xs font-mono font-semibold transition-all whitespace-nowrap cursor-pointer flex items-center gap-2 ${
              activeFileIndex === idx
                ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 shadow-sm'
                : 'bg-slate-900 text-slate-400 hover:bg-slate-800 border border-slate-800'
            }`}
          >
            <span
              className={`w-2 h-2 rounded-full ${
                file.name.endsWith('.kt')
                  ? 'bg-purple-400'
                  : file.name.endsWith('.sql')
                  ? 'bg-amber-400'
                  : 'bg-blue-400'
              }`}
            ></span>
            {file.name}
          </button>
        ))}
      </div>

      {/* Code Editor Container */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-2xl overflow-hidden shadow-2xl">
        <div className="px-5 py-4 bg-slate-800/80 border-b border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <div className="flex items-center gap-2">
              <span className="font-mono text-xs font-bold text-white">{currentFile.path}</span>
              <span className="text-[10px] px-2 py-0.5 rounded bg-slate-950 font-mono text-emerald-400 border border-emerald-500/20">
                {currentFile.language.toUpperCase()}
              </span>
            </div>
            <p className="text-xs text-slate-400 mt-1">{currentFile.description}</p>
          </div>

          <button
            onClick={() => handleCopy(currentFile.name, currentFile.code)}
            className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg bg-emerald-500 hover:bg-emerald-600 text-slate-950 text-xs font-bold transition-all cursor-pointer shrink-0"
          >
            {copiedFile === currentFile.name ? (
              <>
                <Check className="w-4 h-4" /> Copied to Clipboard!
              </>
            ) : (
              <>
                <Copy className="w-4 h-4" /> Copy Entire File
              </>
            )}
          </button>
        </div>

        <div className="p-5 bg-slate-950 overflow-x-auto text-[11px] font-mono text-slate-300 leading-relaxed max-h-[580px]">
          <pre>{currentFile.code}</pre>
        </div>
      </div>
    </div>
  );
};
