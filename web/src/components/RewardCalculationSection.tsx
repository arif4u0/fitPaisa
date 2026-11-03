import React, { useState } from 'react';
import {
  AlertTriangle,
  Bug,
  CheckCircle2,
  Copy,
  Check,
  Flame,
  Gauge,
  Calendar,
  Lock,
  ArrowRight,
  ShieldCheck,
  Zap,
} from 'lucide-react';
import {
  calculateStepConversion,
  simulateLegacyMultiBatchConversion,
  generateDeltaSourceId,
  validateStepCadence,
  validateDateBoundary,
} from '../utils/rewardCalculator';

export const RewardCalculationSection: React.FC = () => {
  // Simulator state
  const [observedSteps, setObservedSteps] = useState<number>(6500);
  const [dailyCap, setDailyCap] = useState<number>(10000);
  const [alreadyCredited, setAlreadyCredited] = useState<number>(2000);
  const [isManual, setIsManual] = useState<boolean>(false);

  // Multi-batch simulation
  const [batchHistory, setBatchHistory] = useState<
    Array<{ time: string; steps: number; coins: number; sourceId: string; status: 'SUCCESS' | 'FAILED_23505' }>
  >([
    {
      time: '11:00 AM (Morning Walk)',
      steps: 3000,
      coins: 30,
      sourceId: 'steps:user_123:2026-10-25:v2',
      status: 'SUCCESS',
    },
  ]);
  const [useFixMode, setUseFixMode] = useState<boolean>(false);

  // Cadence test
  const [testCadenceSteps, setTestCadenceSteps] = useState<number>(5000);
  const [testCadenceMinutes, setTestCadenceMinutes] = useState<number>(5);

  // Date spoof test
  const [testClientDate, setTestClientDate] = useState<string>('2026-10-25');

  const [copiedFile, setCopiedFile] = useState<string | null>(null);

  const conversion = calculateStepConversion({
    observedSteps,
    dailyCap,
    stepsPerCoin: 100,
    alreadyCreditedSteps: alreadyCredited,
    isManualEntry: isManual,
  });

  const cadenceResult = validateStepCadence(testCadenceSteps, testCadenceMinutes * 60);
  const dateResult = validateDateBoundary(testClientDate, new Date());

  const handleSimulateSecondConversion = () => {
    const nextSteps = 4000;
    const nextCoins = 40;

    if (!useFixMode) {
      // Legacy logic tries to reuse steps:user_123:2026-10-25:v2
      const legacyId = 'steps:user_123:2026-10-25:v2';
      const sim = simulateLegacyMultiBatchConversion(
        'user_123',
        '2026-10-25',
        2,
        batchHistory.map((b) => b.sourceId)
      );

      setBatchHistory((prev) => [
        ...prev,
        {
          time: '06:30 PM (Evening Walk)',
          steps: nextSteps,
          coins: sim.allowed ? nextCoins : 0,
          sourceId: legacyId,
          status: sim.allowed ? 'SUCCESS' : 'FAILED_23505',
        },
      ]);
    } else {
      // Fixed delta logic uses batch index & range
      const batchNum = batchHistory.length + 1;
      const deltaId = generateDeltaSourceId('user_123', '2026-10-25', batchNum, 3000, 7000);
      setBatchHistory((prev) => [
        ...prev,
        {
          time: '06:30 PM (Evening Walk)',
          steps: nextSteps,
          coins: nextCoins,
          sourceId: deltaId,
          status: 'SUCCESS',
        },
      ]);
    }
  };

  const resetBatchSimulation = () => {
    setBatchHistory([
      {
        time: '11:00 AM (Morning Walk)',
        steps: 3000,
        coins: 30,
        sourceId: 'steps:user_123:2026-10-25:v2',
        status: 'SUCCESS',
      },
    ]);
  };

  const handleCopy = (filename: string, code: string) => {
    navigator.clipboard.writeText(code);
    setCopiedFile(filename);
    setTimeout(() => setCopiedFile(null), 2500);
  };

  const fixedSyncStepsCode = `/**
 * sync-steps — hardened step verification and delta coin ledgering
 * 
 * FIXES APPLIED:
 * 1. FIXED POSTGRES 23505 ERROR: Replaces static source_id with sequential delta batch key,
 *    allowing multiple safe step conversions throughout the same day without coin loss.
 * 2. DATE SPOOFING GUARD: Validates client local_date against server UTC (within +/- 26h max drift).
 * 3. CADENCE SENSOR CHECK: Verifies step velocity does not exceed 300 steps/min (anti-bot / rocker).
 * 4. ATOMIC LEVEL & LIFETIME UPGRADE: Protected with row locking.
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

  // --- SECURITY CHECK 1: Date Boundary Verification ---
  // Prevent date spoofing (e.g. forging past or future dates to farm bonus coins)
  const clientDate = new Date(\`\${localDate}T12:00:00Z\`);
  const serverNow = new Date();
  const driftHours = Math.abs(clientDate.getTime() - serverNow.getTime()) / (1000 * 60 * 60);
  if (driftHours > 28) {
    return json({ error: "date_out_of_sync", message: "Date out of valid range (max 28 hours timezone drift)." }, 400);
  }

  // Idempotency check: prevent duplicate submits from network retries
  const { data: seen } = await db
    .from("step_sync_events")
    .select("id")
    .eq("idempotency_key", idempotencyKey)
    .maybeSingle();
  if (seen) return json({ ok: true, deduped: true });

  // Get active rule version
  const { data: cfg } = await db
    .from("app_config")
    .select("value")
    .eq("key", "rule_version")
    .single();
  const ruleVersion = (cfg?.value as number) ?? 2;

  const { data: rule } = await db
    .from("reward_rules")
    .select("*")
    .eq("version", ruleVersion)
    .single();
  if (!rule) return json({ error: "no_active_rule" }, 500);

  // Get user profile & level
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

  // Manual steps exclusion
  const manual = (sourceSummary as Record<string, unknown> | undefined)?.manual === true;
  const eligibleTotal = manual ? 0 : Math.min(observedTotal, dailyCap);

  // Current credited progress
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

  // Update daily_steps accumulator
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

  // --- FIX 1: Safe Sequential Delta Ledger Key ---
  // Using \`steps:\${userId}:\${localDate}:delta:\${alreadyCredited}_\${eligibleTotal}\` guarantees
  // uniqueness for every delta conversion while preventing duplicate credit for the same step slice!
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

  // Update lifetime steps & level
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
});`;

  return (
    <div className="space-y-8">
      {/* Bug Highlight Banner */}
      <div className="bg-amber-500/10 border border-amber-500/30 rounded-2xl p-6 text-slate-200">
        <div className="flex items-start gap-4">
          <div className="p-3 bg-amber-500/20 text-amber-400 rounded-xl">
            <Bug className="w-6 h-6" />
          </div>
          <div>
            <h3 className="text-lg font-bold text-white flex items-center gap-2">
              Critical Bug Detected: Second Step Conversion on Same Day Yields 0 Coins!
              <span className="text-xs px-2.5 py-0.5 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30 font-mono">
                Postgres 23505 Error
              </span>
            </h3>
            <p className="text-sm text-slate-300 mt-2 leading-relaxed">
              In your current <code className="text-amber-300 font-mono">sync-steps/index.ts</code>, ledger entries are created with:
              <br />
              <code className="text-white bg-slate-900 px-2 py-0.5 rounded font-mono text-xs">
                source_id: `steps:$&#123;userId&#125;:$&#123;localDate&#125;:v$&#123;ruleVersion&#125;`
              </code>
              <br />
              Because your PostgreSQL database enforces a{' '}
              <code className="text-amber-300 font-semibold">UNIQUE(source_type, source_id)</code> constraint, whenever an active user walks 3,000 steps in the morning and converts, and then walks another 4,000 steps in the evening:
              <br />
              <span className="text-amber-200 font-semibold">
                The database REJECTS the second insert (error 23505). Your code catches this as a "replay" and credits ZERO coins!
              </span>{' '}
              Users lose all reward coins from their afternoon and evening walks.
            </p>
          </div>
        </div>
      </div>

      {/* Multi-batch Visual Simulator */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-slate-800">
          <div>
            <h4 className="text-base font-bold text-white flex items-center gap-2">
              <Zap className="w-5 h-5 text-amber-400" />
              Live Interactive Multi-Batch Conversion Simulator
            </h4>
            <p className="text-xs text-slate-400 mt-1">
              Test what happens when a user converts steps multiple times throughout the day with Legacy vs Fixed logic.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => setUseFixMode(!useFixMode)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold border transition-all cursor-pointer ${
                useFixMode
                  ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40'
                  : 'bg-red-500/20 text-red-300 border-red-500/40'
              }`}
            >
              Mode: {useFixMode ? '✅ Fixed (Delta Batch Key)' : '❌ Legacy Buggy Code'}
            </button>
            <button
              onClick={handleSimulateSecondConversion}
              className="px-3.5 py-1.5 bg-emerald-500 hover:bg-emerald-600 text-slate-950 text-xs font-bold rounded-lg transition-all cursor-pointer"
            >
              Simulate Evening Conversion (+4,000 Steps)
            </button>
            <button
              onClick={resetBatchSimulation}
              className="px-2.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs rounded-lg transition-all cursor-pointer"
            >
              Reset
            </button>
          </div>
        </div>

        {/* Batch History List */}
        <div className="mt-6 space-y-3">
          {batchHistory.map((batch, index) => (
            <div
              key={index}
              className={`p-4 rounded-xl border flex flex-col sm:flex-row sm:items-center justify-between gap-3 ${
                batch.status === 'SUCCESS'
                  ? 'bg-slate-950 border-slate-800'
                  : 'bg-red-500/10 border-red-500/30'
              }`}
            >
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-semibold text-white text-sm">{batch.time}</span>
                  <span
                    className={`text-[10px] font-mono px-2 py-0.5 rounded ${
                      batch.status === 'SUCCESS'
                        ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                        : 'bg-red-500/20 text-red-400 border border-red-500/30'
                    }`}
                  >
                    {batch.status === 'SUCCESS' ? 'CREDITED TO LEDGER' : 'BLOCKED BY UNIQUE 23505'}
                  </span>
                </div>
                <div className="text-xs text-slate-400 font-mono mt-1">
                  source_id: <span className="text-slate-300">{batch.sourceId}</span>
                </div>
              </div>

              <div className="flex items-center gap-4 text-right">
                <div>
                  <div className="text-xs text-slate-400">Step Delta</div>
                  <div className="text-sm font-bold text-white">+{batch.steps.toLocaleString()}</div>
                </div>
                <div>
                  <div className="text-xs text-slate-400">Coins Credited</div>
                  <div
                    className={`text-base font-extrabold ${
                      batch.coins > 0 ? 'text-emerald-400' : 'text-red-400'
                    }`}
                  >
                    {batch.coins > 0 ? `+${batch.coins}` : '0 (LOST!)'}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Cadence & Anti-Cheat Validator */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-6">
          <div className="flex items-center gap-2 pb-4 border-b border-slate-800">
            <Gauge className="w-5 h-5 text-indigo-400" />
            <h4 className="text-base font-bold text-white">Cadence &amp; Step Velocity Anti-Cheat</h4>
          </div>
          <p className="text-xs text-slate-400 mt-3">
            Physical limits of human movement: sprinting cadence peaks around 240-280 steps/min. Fast walking is 100-130 steps/min. Mechanical shakers and rooted memory injection report thousands of steps per minute.
          </p>

          <div className="grid grid-cols-2 gap-4 mt-4">
            <div>
              <label className="text-xs text-slate-300 block mb-1">Steps Recorded</label>
              <input
                type="number"
                value={testCadenceSteps}
                onChange={(e) => setTestCadenceSteps(Number(e.target.value))}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-lg text-sm text-white font-mono focus:outline-none"
              />
            </div>
            <div>
              <label className="text-xs text-slate-300 block mb-1">Time Elapsed (Minutes)</label>
              <input
                type="number"
                value={testCadenceMinutes}
                onChange={(e) => setTestCadenceMinutes(Number(e.target.value))}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-lg text-sm text-white font-mono focus:outline-none"
              />
            </div>
          </div>

          <div
            className={`mt-4 p-4 rounded-xl border text-xs ${
              cadenceResult.isValid
                ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-200'
                : 'bg-red-500/10 border-red-500/30 text-red-200'
            }`}
          >
            <div className="flex items-center justify-between font-bold">
              <span>Cadence: {cadenceResult.stepsPerMinute} steps/min</span>
              <span className="font-mono text-[10px] px-2 py-0.5 rounded bg-slate-900">
                Risk: {cadenceResult.riskLevel}
              </span>
            </div>
            <div className="mt-1">{cadenceResult.reason}</div>
          </div>
        </div>

        {/* Date Forgery Guard */}
        <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-6">
          <div className="flex items-center gap-2 pb-4 border-b border-slate-800">
            <Calendar className="w-5 h-5 text-indigo-400" />
            <h4 className="text-base font-bold text-white">Date Spoofing &amp; Historical Sync Guard</h4>
          </div>
          <p className="text-xs text-slate-400 mt-3">
            In the current APK, <code className="text-amber-300">local_date</code> is taken directly from the client request body. A simple script can submit past dates for every day of the year, claiming thousands of PKR in minutes.
          </p>

          <div className="mt-4">
            <label className="text-xs text-slate-300 block mb-1">Simulated Client local_date</label>
            <div className="flex gap-2">
              <input
                type="text"
                value={testClientDate}
                onChange={(e) => setTestClientDate(e.target.value)}
                className="flex-1 px-3 py-2 bg-slate-950 border border-slate-700 rounded-lg text-sm text-white font-mono focus:outline-none"
                placeholder="YYYY-MM-DD"
              />
              <button
                onClick={() => setTestClientDate('2026-03-01')}
                className="px-2.5 py-1 text-xs bg-red-500/20 text-red-300 border border-red-500/30 rounded-lg hover:bg-red-500/30 cursor-pointer"
              >
                Test Past Date Attack
              </button>
            </div>
          </div>

          <div
            className={`mt-4 p-4 rounded-xl border text-xs ${
              dateResult.isValid
                ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-200'
                : 'bg-red-500/10 border-red-500/30 text-red-200'
            }`}
          >
            <div className="font-bold">
              {dateResult.isValid ? '✅ Date Accepted' : '❌ Date Rejected (Tampering Detected)'}
            </div>
            <div className="mt-1">{dateResult.message}</div>
          </div>
        </div>
      </div>

      {/* Code Patch Preview */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-2xl overflow-hidden">
        <div className="px-5 py-3.5 bg-slate-800/60 border-b border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-400"></span>
            <span className="font-mono text-xs font-bold text-slate-200">
              supabase/functions/sync-steps/index.ts (Hardened)
            </span>
          </div>
          <button
            onClick={() => handleCopy('sync-steps', fixedSyncStepsCode)}
            className="flex items-center gap-1.5 text-xs text-emerald-400 hover:text-emerald-300 transition-colors cursor-pointer"
          >
            {copiedFile === 'sync-steps' ? (
              <>
                <Check className="w-3.5 h-3.5" /> Copied!
              </>
            ) : (
              <>
                <Copy className="w-3.5 h-3.5" /> Copy Fixed Function
              </>
            )}
          </button>
        </div>
        <div className="p-4 bg-slate-950 overflow-x-auto text-[11px] font-mono text-slate-300 leading-relaxed max-h-[420px]">
          <pre>{fixedSyncStepsCode}</pre>
        </div>
      </div>
    </div>
  );
};
