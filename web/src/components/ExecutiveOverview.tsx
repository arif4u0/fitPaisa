import React from 'react';
import {
  ShieldAlert,
  Tv,
  Bug,
  DollarSign,
  ArrowRight,
  CheckCircle2,
  AlertTriangle,
  Flame,
  FileCode,
  Lock,
  Layers,
} from 'lucide-react';
import { ActiveTab } from './Navbar';

interface ExecutiveOverviewProps {
  onNavigate: (tab: ActiveTab) => void;
}

export const ExecutiveOverview: React.FC<ExecutiveOverviewProps> = ({ onNavigate }) => {
  return (
    <div className="space-y-8">
      {/* Hero Banner */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-slate-900 via-slate-900 to-emerald-950/40 border border-slate-800 p-8 sm:p-10 shadow-2xl">
        <div className="relative z-10 max-w-3xl">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold mb-4">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
            Repository Audit: arif4u0/fitPaisa (Kotlin + Supabase)
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight leading-tight">
            FitPaisa Security Audit, Reward Engine &amp; Unity Ads Analysis
          </h2>
          <p className="text-sm sm:text-base text-slate-300 mt-4 leading-relaxed">
            We conducted a thorough technical audit across your Android Kotlin app (<code className="text-emerald-300">app/src/main/...</code>)
            and Supabase backend (<code className="text-emerald-300">supabase/functions</code> &amp; <code className="text-emerald-300">migrations</code>).
            Below is the complete executive diagnosis with interactive simulators, live diagnostics, and copy-paste ready code patches.
          </p>
        </div>
      </div>

      {/* 3 Core Inquiries Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Issue 1: Unity Ads */}
        <div className="p-6 rounded-2xl bg-slate-900/80 border border-slate-800 flex flex-col justify-between hover:border-slate-700 transition-all">
          <div>
            <div className="w-12 h-12 rounded-xl bg-red-500/15 border border-red-500/30 flex items-center justify-center text-red-400 mb-4">
              <Tv className="w-6 h-6" />
            </div>
            <div className="text-xs font-mono font-bold text-red-400 uppercase tracking-wider">
              Issue 1: Ad System Failure
            </div>
            <h3 className="text-lg font-bold text-white mt-1">Why Unity Ads Aren't Showing</h3>
            <p className="text-xs text-slate-400 mt-2 leading-relaxed">
              Found 5 specific causes: Ad Unit ID is set to <code className="text-red-300">BP_Rewarded_Android</code> (mismatched with Unity's standard <code className="text-emerald-300">Rewarded_Android</code>), race condition causing instant <code className="text-red-300">NOT_READY</code> fast-fail without awaiting load, zero-retry deadlock, and no Logcat output.
            </p>
          </div>
          <button
            onClick={() => onNavigate('unity-ads')}
            className="mt-6 flex items-center justify-between text-xs font-bold text-emerald-400 hover:text-emerald-300 transition-colors group cursor-pointer"
          >
            <span>Inspect Unity Ads Fixes</span>
            <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
          </button>
        </div>

        {/* Issue 2: Reward Calculation */}
        <div className="p-6 rounded-2xl bg-slate-900/80 border border-slate-800 flex flex-col justify-between hover:border-slate-700 transition-all">
          <div>
            <div className="w-12 h-12 rounded-xl bg-amber-500/15 border border-amber-500/30 flex items-center justify-center text-amber-400 mb-4">
              <Bug className="w-6 h-6" />
            </div>
            <div className="text-xs font-mono font-bold text-amber-400 uppercase tracking-wider">
              Issue 2: Lost Progress &amp; Anti-Cheat
            </div>
            <h3 className="text-lg font-bold text-white mt-1">Reward Calculation &amp; Postgres 23505</h3>
            <p className="text-xs text-slate-400 mt-2 leading-relaxed">
              Discovered a severe bug in <code className="text-amber-300">sync-steps/index.ts</code>: <code className="text-amber-300">source_id</code> uses a static day key. When users convert a second time in the afternoon, Postgres throws error 23505 and credits <strong>ZERO coins</strong>! Plus missing date spoofing guards.
            </p>
          </div>
          <button
            onClick={() => onNavigate('reward-calc')}
            className="mt-6 flex items-center justify-between text-xs font-bold text-amber-400 hover:text-amber-300 transition-colors group cursor-pointer"
          >
            <span>Review Reward Engine Fix</span>
            <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
          </button>
        </div>

        {/* Issue 3: Wallet Security */}
        <div className="p-6 rounded-2xl bg-slate-900/80 border border-slate-800 flex flex-col justify-between hover:border-slate-700 transition-all">
          <div>
            <div className="w-12 h-12 rounded-xl bg-emerald-500/15 border border-emerald-500/30 flex items-center justify-center text-emerald-400 mb-4">
              <Lock className="w-6 h-6" />
            </div>
            <div className="text-xs font-mono font-bold text-emerald-400 uppercase tracking-wider">
              Issue 3: Real Money Safeguards
            </div>
            <h3 className="text-lg font-bold text-white mt-1">Wallet Security &amp; Sybil Fraud</h3>
            <p className="text-xs text-slate-400 mt-2 leading-relaxed">
              When sending real cash from your Easypaisa/JazzCash wallet, you face: concurrent race condition overdrafts (missing <code className="text-emerald-300">pg_advisory_xact_lock</code>) and Sybil fraud (one scammer routing 10 bot accounts to the same phone number).
            </p>
          </div>
          <button
            onClick={() => onNavigate('wallet-security')}
            className="mt-6 flex items-center justify-between text-xs font-bold text-emerald-400 hover:text-emerald-300 transition-colors group cursor-pointer"
          >
            <span>View Wallet Vault Fixes</span>
            <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
          </button>
        </div>
      </div>

      {/* Critical Economic Warning */}
      <div className="p-6 rounded-2xl bg-slate-900/80 border border-slate-800 flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="flex items-start gap-4">
          <div className="p-3 bg-indigo-500/20 text-indigo-400 rounded-xl">
            <DollarSign className="w-6 h-6" />
          </div>
          <div>
            <h4 className="text-base font-bold text-white">Commercial Unit Economics Warning: Are You Losing Money?</h4>
            <p className="text-xs text-slate-400 mt-1 max-w-2xl">
              In Pakistan, 1 Rewarded Video ad earns ~$0.0012 USD (~Rs 0.34). However, converting 10,000 steps grants 100 coins worth Rs 1.00 in real money. Requiring only 1 ad per conversion burns Rs 0.66 per user from your personal bank account. Check our P&amp;L model for the break-even energy bar solution.
            </p>
          </div>
        </div>
        <button
          onClick={() => onNavigate('economics')}
          className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold rounded-xl transition-all whitespace-nowrap cursor-pointer shrink-0"
        >
          Open Economic Calculator
        </button>
      </div>

      {/* Implementation Checklist */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-6">
        <h4 className="text-base font-bold text-white pb-4 border-b border-slate-800 flex items-center gap-2">
          <CheckCircle2 className="w-5 h-5 text-emerald-400" />
          Recommended 5-Step Action Checklist for FitPaisa
        </h4>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4 text-xs text-slate-300">
          <div className="flex items-start gap-3 p-3 rounded-xl bg-slate-950 border border-slate-800">
            <span className="w-5 h-5 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center font-bold text-[11px] shrink-0">1</span>
            <div>
              <strong className="text-white block">Update AdsConfig.kt &amp; AdManager.kt</strong>
              Replace placement ID with <code className="text-emerald-300">Rewarded_Android</code> and add coroutine wait + exponential retry.
            </div>
          </div>

          <div className="flex items-start gap-3 p-3 rounded-xl bg-slate-950 border border-slate-800">
            <span className="w-5 h-5 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center font-bold text-[11px] shrink-0">2</span>
            <div>
              <strong className="text-white block">Deploy Updated sync-steps/index.ts</strong>
              Replaces static source_id with sequential delta keys so afternoon walk conversions don't throw 23505 and credit 0 coins.
            </div>
          </div>

          <div className="flex items-start gap-3 p-3 rounded-xl bg-slate-950 border border-slate-800">
            <span className="w-5 h-5 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center font-bold text-[11px] shrink-0">3</span>
            <div>
              <strong className="text-white block">Run 015_wallet_security.sql Migration</strong>
              Adds transaction advisory lock <code className="text-emerald-300">pg_advisory_xact_lock</code> to prevent concurrent cashout overdrafts.
            </div>
          </div>

          <div className="flex items-start gap-3 p-3 rounded-xl bg-slate-950 border border-slate-800">
            <span className="w-5 h-5 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center font-bold text-[11px] shrink-0">4</span>
            <div>
              <strong className="text-white block">Check View Sybil Alerts Before Payouts</strong>
              Query <code className="text-emerald-300">view_sybil_wallet_alerts</code> in Supabase to flag any Easypaisa number linked to multiple accounts.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
