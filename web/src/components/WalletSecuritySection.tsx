import React, { useState } from 'react';
import {
  ShieldAlert,
  ShieldCheck,
  Lock,
  Users,
  Copy,
  Check,
  AlertTriangle,
  FileCheck,
  KeyRound,
  RotateCw,
  Zap,
} from 'lucide-react';
import {
  detectSybilAccounts,
  simulateConcurrentCashouts,
  CashoutRequest,
} from '../utils/walletSecurity';

export const WalletSecuritySection: React.FC = () => {
  // Concurrency Simulation state
  const [balance, setBalance] = useState<number>(10000); // 10,000 coins = Rs 100
  const [withdrawal, setWithdrawal] = useState<number>(10000);
  const [parallelRequests, setParallelRequests] = useState<number>(5);
  const [useAdvisoryLock, setUseAdvisoryLock] = useState<boolean>(false);
  const [copiedFile, setCopiedFile] = useState<string | null>(null);

  const concurrencySim = simulateConcurrentCashouts(
    balance,
    withdrawal,
    parallelRequests,
    useAdvisoryLock
  );

  // Sybil Sample Data
  const [sybilList] = useState<CashoutRequest[]>([
    {
      userId: 'usr_001',
      userEmail: 'honest.ali@gmail.com',
      coins: 10000,
      method: 'easypaisa',
      accountTitle: 'Ali Rehman',
      accountNumber: '03001234567',
      timestamp: Date.now() - 3600000,
    },
    {
      userId: 'usr_002',
      userEmail: 'tariq.gym@gmail.com',
      coins: 15000,
      method: 'jazzcash',
      accountTitle: 'Tariq Mehmood',
      accountNumber: '03019876543',
      timestamp: Date.now() - 2500000,
    },
    {
      userId: 'usr_003',
      userEmail: 'farhan_gamer99@gmail.com',
      coins: 10000,
      method: 'easypaisa',
      accountTitle: 'Babar Azam',
      accountNumber: '03458889999', // Shared!
      timestamp: Date.now() - 1800000,
    },
    {
      userId: 'usr_004',
      userEmail: 'free_coins_bot1@gmail.com',
      coins: 10000,
      method: 'easypaisa',
      accountTitle: 'Babar A.',
      accountNumber: '0345-888-9999', // Shared!
      timestamp: Date.now() - 1200000,
    },
    {
      userId: 'usr_005',
      userEmail: 'crypto_walk2@gmail.com',
      coins: 10000,
      method: 'easypaisa',
      accountTitle: 'Babar',
      accountNumber: '03458889999', // Shared!
      timestamp: Date.now() - 600000,
    },
  ]);

  const sybilDetected = detectSybilAccounts(sybilList);

  const handleCopy = (filename: string, code: string) => {
    navigator.clipboard.writeText(code);
    setCopiedFile(filename);
    setTimeout(() => setCopiedFile(null), 2500);
  };

  const sqlSecurityPatch = `-- ============================================================================
-- 015_wallet_security.sql — Enterprise Wallet Security & Anti-Fraud Hardening
-- Run in Supabase SQL Editor.
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
  -- CRITICAL SECURITY FIX 1: Transaction-level advisory lock on user UUID!
  -- Serializes all coin movements for this user across concurrent threads.
  perform pg_advisory_xact_lock(hashtext(p_user_id::text));

  -- Minimum coins check
  if p_coins < v_min_coins then
    raise exception 'below_minimum';
  end if;

  -- Method validation
  if p_method not in ('easypaisa', 'jazzcash', 'bank') then
    raise exception 'bad_method';
  end if;

  -- CRITICAL SECURITY FIX 2: Check for existing pending cashout
  select count(*) into v_pending_count
  from public.cashout_requests
  where user_id = p_user_id and status = 'pending';

  if v_pending_count > 0 then
    raise exception 'pending_request_exists';
  end if;

  -- Calculate spendable balance inside locked transaction
  select coalesce(sum(amount_coins), 0)::integer into v_available
  from public.ledger_entries
  where user_id = p_user_id and state = 'available';

  if v_available < p_coins then
    raise exception 'insufficient_coins';
  end if;

  v_paisa := p_coins;
  v_cleaned_acc := regexp_replace(p_account_number, '[^0-9]', '', 'g');

  -- Reserve the coins atomically
  insert into public.ledger_entries (user_id, amount_coins, state, source_type, source_id)
  values (
    p_user_id,
    -p_coins,
    'available',
    'cashout',
    'cashout:' || p_user_id::text || ':' || extract(epoch from now())::bigint
  );

  -- Insert cashout request with cleaned account number
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

-- 4. Fraud Detection View: Detects multiple user IDs targeting the same payout number
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
order by linked_user_count desc;

comment on view public.view_sybil_wallet_alerts is 'Flags when one Easypaisa/JazzCash account is used by multiple profiles.';`;

  return (
    <div className="space-y-8">
      {/* Top Banner Alert */}
      <div className="bg-red-500/10 border border-red-500/30 rounded-2xl p-6 text-slate-200">
        <div className="flex items-start gap-4">
          <div className="p-3 bg-red-500/20 text-red-400 rounded-xl">
            <ShieldAlert className="w-6 h-6" />
          </div>
          <div>
            <h3 className="text-lg font-bold text-white flex items-center gap-2">
              Founder Wallet Safety: Two Major Financial Drain Risks Identified
              <span className="text-xs px-2.5 py-0.5 rounded-full bg-red-500/20 text-red-300 border border-red-500/30 font-mono">
                Real Money Payouts
              </span>
            </h3>
            <p className="text-sm text-slate-300 mt-2 leading-relaxed">
              Because you send cash manually from your personal Easypaisa or JazzCash wallet, attackers target two vulnerabilities to drain your money:
            </p>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4 text-xs">
              <div className="p-3 bg-slate-900/80 rounded-xl border border-red-500/20">
                <span className="font-bold text-red-300 block mb-1">1. Concurrency Double-Spend Race Condition:</span>
                Without transaction-level locking (<code className="text-emerald-300">pg_advisory_xact_lock</code>), an attacker with 10,000 coins fires 5 parallel cashout calls. All 5 read balance = 10,000 and pass! The founder sees 5 valid requests totaling 50,000 coins and pays out Rs 500 instead of Rs 100!
              </div>
              <div className="p-3 bg-slate-900/80 rounded-xl border border-red-500/20">
                <span className="font-bold text-red-300 block mb-1">2. Sybil Multi-Account Attack (One Wallet, Many Users):</span>
                A fraudster creates 10 different Google accounts on their phone or PC emulator, bots 10k steps on all of them, and routes all cashouts to the same personal Easypaisa number (<code className="text-emerald-300">0345-xxxxxxx</code>). The founder sends money 10 times to the same scammer.
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Concurrency Simulator */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-slate-800">
          <div>
            <h4 className="text-base font-bold text-white flex items-center gap-2">
              <Lock className="w-5 h-5 text-indigo-400" />
              Interactive Concurrency &amp; Race Condition Simulator
            </h4>
            <p className="text-xs text-slate-400 mt-1">
              See what happens when multiple withdrawal requests are triggered simultaneously with vs without PostgreSQL Advisory Locking.
            </p>
          </div>
          <button
            onClick={() => setUseAdvisoryLock(!useAdvisoryLock)}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-bold border transition-all cursor-pointer ${
              useAdvisoryLock
                ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40'
                : 'bg-red-500/20 text-red-300 border-red-500/40'
            }`}
          >
            Advisory Lock: {useAdvisoryLock ? 'ENABLED (Protected)' : 'DISABLED (Current Repo Bug)'}
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-6">
          <div>
            <label className="text-xs text-slate-300 block mb-1">User Balance (FitCoins)</label>
            <input
              type="number"
              value={balance}
              onChange={(e) => setBalance(Number(e.target.value))}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-lg text-sm text-white font-mono focus:outline-none"
            />
            <span className="text-[11px] text-slate-500 mt-1 block">10,000 coins = Rs 100</span>
          </div>

          <div>
            <label className="text-xs text-slate-300 block mb-1">Cashout Amount per Request</label>
            <input
              type="number"
              value={withdrawal}
              onChange={(e) => setWithdrawal(Number(e.target.value))}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-lg text-sm text-white font-mono focus:outline-none"
            />
            <span className="text-[11px] text-slate-500 mt-1 block">Minimum allowed is 10,000</span>
          </div>

          <div>
            <label className="text-xs text-slate-300 block mb-1">Simultaneous Parallel Requests</label>
            <input
              type="number"
              value={parallelRequests}
              onChange={(e) => setParallelRequests(Number(e.target.value))}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-lg text-sm text-white font-mono focus:outline-none"
            />
            <span className="text-[11px] text-slate-500 mt-1 block">Fired in the exact same millisecond</span>
          </div>
        </div>

        {/* Results Card */}
        <div className="grid grid-cols-1 sm:grid-cols-4 gap-4 mt-6">
          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800">
            <span className="text-xs text-slate-400">Approved Requests</span>
            <div
              className={`text-2xl font-black mt-1 ${
                concurrencySim.approvedRequests > 1 && !useAdvisoryLock
                  ? 'text-red-400'
                  : 'text-emerald-400'
              }`}
            >
              {concurrencySim.approvedRequests} / {parallelRequests}
            </div>
            <span className="text-[11px] text-slate-500">
              {concurrencySim.approvedRequests > 1 && !useAdvisoryLock
                ? 'Multiple requests approved on single balance!'
                : '1 request approved, others rejected'}
            </span>
          </div>

          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800">
            <span className="text-xs text-slate-400">Ledger Overdraft</span>
            <div
              className={`text-2xl font-black mt-1 ${
                concurrencySim.overdraftAmount > 0 ? 'text-red-400' : 'text-slate-200'
              }`}
            >
              -{concurrencySim.overdraftAmount.toLocaleString()} Coins
            </div>
            <span className="text-[11px] text-slate-500">
              {concurrencySim.overdraftAmount > 0 ? 'Ghost coins created' : 'Zero balance leakage'}
            </span>
          </div>

          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800">
            <span className="text-xs text-slate-400">Founder Financial Loss</span>
            <div
              className={`text-2xl font-black mt-1 ${
                concurrencySim.overdraftAmount > 0 ? 'text-red-400' : 'text-emerald-400'
              }`}
            >
              Rs {(concurrencySim.overdraftAmount / 100).toFixed(2)}
            </div>
            <span className="text-[11px] text-slate-500">Unearned money sent from wallet</span>
          </div>

          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800">
            <span className="text-xs text-slate-400">Race Condition Status</span>
            <div
              className={`text-sm font-bold mt-2 ${
                concurrencySim.raceConditionPrevented ? 'text-emerald-400' : 'text-red-400'
              }`}
            >
              {concurrencySim.raceConditionPrevented ? '🛡️ Prevented by Lock' : '🚨 VULNERABLE TO EXPLOIT'}
            </div>
            <span className="text-[11px] text-slate-500">
              {concurrencySim.raceConditionPrevented ? 'Atomically serialized' : 'Needs pg_advisory_xact_lock'}
            </span>
          </div>
        </div>
      </div>

      {/* Sybil Multi-Account Detector */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-6">
        <div className="flex items-center justify-between pb-6 border-b border-slate-800">
          <div>
            <h4 className="text-base font-bold text-white flex items-center gap-2">
              <Users className="w-5 h-5 text-indigo-400" />
              Sybil Account &amp; Cloned Device Detection in Cashouts
            </h4>
            <p className="text-xs text-slate-400 mt-1">
              Demonstrates automatic cross-checking of Easypaisa/JazzCash phone numbers across different user accounts.
            </p>
          </div>
          <span className="text-xs px-2.5 py-1 rounded-full bg-red-500/20 text-red-300 font-mono">
            {sybilDetected.size} Fraud Clusters Detected
          </span>
        </div>

        <div className="mt-6 space-y-3">
          {Array.from(sybilDetected.entries()).map(([account, data], idx) => (
            <div
              key={idx}
              className="p-4 rounded-xl bg-red-500/10 border border-red-500/30 flex flex-col md:flex-row md:items-center justify-between gap-4"
            >
              <div>
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-red-300 uppercase tracking-wider">
                    🚨 High-Risk Sybil Cluster
                  </span>
                  <span className="text-xs bg-slate-900 text-slate-200 px-2 py-0.5 rounded font-mono">
                    Account: {account}
                  </span>
                </div>
                <div className="text-xs text-slate-300 mt-1">
                  Shared across <span className="text-red-300 font-bold">{data.userIds.length} distinct user accounts</span>:
                </div>
                <div className="flex flex-wrap gap-1.5 mt-2">
                  {data.emails.map((email, eIdx) => (
                    <span
                      key={eIdx}
                      className="text-[11px] px-2 py-0.5 rounded bg-slate-900 border border-slate-800 text-slate-300 font-mono"
                    >
                      {email}
                    </span>
                  ))}
                </div>
              </div>

              <div className="text-right">
                <span className="text-xs text-slate-400">Total Claimed</span>
                <div className="text-lg font-black text-red-400">
                  {data.totalCoinsRequested.toLocaleString()} Coins (Rs {data.totalCoinsRequested / 100})
                </div>
                <div className="text-[11px] text-amber-300 font-medium mt-1">
                  ⚠️ Action: Block payout &amp; freeze accounts
                </div>
              </div>
            </div>
          ))}

          {/* Genuine sample */}
          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-between">
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xs font-bold text-emerald-400">✅ Clean Verification</span>
                <span className="text-xs bg-slate-900 text-slate-300 px-2 py-0.5 rounded font-mono">
                  03001234567
                </span>
              </div>
              <div className="text-xs text-slate-400 mt-1">
                Linked exclusively to 1 verified account (<code className="text-slate-300">honest.ali@gmail.com</code>).
              </div>
            </div>
            <div className="text-right">
              <span className="text-xs text-slate-400">Amount</span>
              <div className="text-sm font-bold text-white">10,000 Coins (Rs 100)</div>
              <div className="text-[11px] text-emerald-400 font-medium">Safe to transfer</div>
            </div>
          </div>
        </div>
      </div>

      {/* SQL Migration Code Patch */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-2xl overflow-hidden">
        <div className="px-5 py-3.5 bg-slate-800/60 border-b border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-400"></span>
            <span className="font-mono text-xs font-bold text-slate-200">
              supabase/migrations/015_wallet_security.sql (Postgres Advisory Lock &amp; Sybil View)
            </span>
          </div>
          <button
            onClick={() => handleCopy('015_wallet_security.sql', sqlSecurityPatch)}
            className="flex items-center gap-1.5 text-xs text-emerald-400 hover:text-emerald-300 transition-colors cursor-pointer"
          >
            {copiedFile === '015_wallet_security.sql' ? (
              <>
                <Check className="w-3.5 h-3.5" /> Copied!
              </>
            ) : (
              <>
                <Copy className="w-3.5 h-3.5" /> Copy SQL Migration
              </>
            )}
          </button>
        </div>
        <div className="p-4 bg-slate-950 overflow-x-auto text-[11px] font-mono text-slate-300 leading-relaxed max-h-[420px]">
          <pre>{sqlSecurityPatch}</pre>
        </div>
      </div>
    </div>
  );
};
