import React, { useState } from 'react';
import {
  Wallet,
  Coins,
  Clock,
  ArrowUpRight,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  HelpCircle,
  CreditCard,
  Building,
} from 'lucide-react';
import { useFitPaisa } from '../../context/FitPaisaContext';
import { CashoutModal } from './CashoutModal';

export const WalletTab: React.FC = () => {
  const { user, cashouts } = useFitPaisa();
  const [showCashoutModal, setShowCashoutModal] = useState(false);

  const rupeesAvailable = (user.availableCoins / 100).toFixed(2);
  const rupeesMaturing = (user.maturingCoins / 100).toFixed(2);

  return (
    <div className="space-y-4 sm:space-y-6">
      {/* Wallet Balance Hero Card */}
      <div className="p-5 sm:p-8 bg-gradient-to-br from-slate-900 via-slate-900 to-emerald-950/50 border border-slate-800 rounded-2xl sm:rounded-3xl shadow-xl flex flex-col md:flex-row md:items-center justify-between gap-5 sm:gap-6">
        <div className="space-y-2">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-semibold">
            <Wallet className="w-3.5 h-3.5" />
            <span>FitPaisa Central Wallet</span>
          </div>
          <div>
            <span className="text-xs text-slate-400 block font-medium">Available Spendable Balance</span>
            <div className="flex items-baseline gap-3 mt-1">
              <span className="text-3xl sm:text-4xl font-black text-white tracking-tight tabular-nums">
                {user.availableCoins.toLocaleString()}
              </span>
              <span className="text-emerald-400 font-extrabold text-lg">FitCoins</span>
            </div>
            <p className="text-sm text-slate-300 font-semibold mt-1">
              ≈ Rs. {rupeesAvailable} PKR <span className="text-xs text-slate-500 font-normal">(100 coins = Rs. 1.00)</span>
            </p>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row gap-3 shrink-0">
          <button
            onClick={() => setShowCashoutModal(true)}
            className="px-6 py-3.5 bg-gradient-to-r from-emerald-500 to-teal-400 hover:from-emerald-400 hover:to-teal-300 text-slate-950 font-black text-sm rounded-2xl shadow-lg shadow-emerald-500/25 transition-all cursor-pointer flex items-center justify-center gap-2"
          >
            <span>Cash Out to Wallet</span>
            <ArrowUpRight className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {/* Maturing Coins */}
        <div className="p-5 bg-slate-900/80 border border-slate-800 rounded-3xl">
          <div className="flex items-center justify-between text-slate-400 text-xs">
            <span>Maturing Coins</span>
            <Clock className="w-4 h-4 text-amber-400" />
          </div>
          <div className="text-2xl font-black text-amber-400 mt-2 tabular-nums">
            {user.maturingCoins.toLocaleString()}
          </div>
          <span className="text-[11px] text-slate-400 block mt-1">
            Worth Rs. {rupeesMaturing} • Matures in 7 days
          </span>
        </div>

        {/* Lifetime Steps */}
        <div className="p-5 bg-slate-900/80 border border-slate-800 rounded-3xl">
          <div className="flex items-center justify-between text-slate-400 text-xs">
            <span>Lifetime Steps</span>
            <Coins className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-2xl font-black text-white mt-2 tabular-nums">
            {user.lifetimeSteps.toLocaleString()}
          </div>
          <span className="text-[11px] text-slate-400 block mt-1">
            Across all verified walk sessions
          </span>
        </div>

        {/* Current Tier Perk */}
        <div className="p-5 bg-slate-900/80 border border-slate-800 rounded-3xl">
          <div className="flex items-center justify-between text-slate-400 text-xs">
            <span>Daily Step Cap</span>
            <ShieldCheck className="w-4 h-4 text-indigo-400" />
          </div>
          <div className="text-2xl font-black text-white mt-2 tabular-nums">
            {user.dailyStepCap.toLocaleString()}
          </div>
          <span className="text-[11px] text-slate-400 block mt-1">
            Level {user.level} {user.levelName} threshold
          </span>
        </div>
      </div>

      {/* Cashout Explainer */}
      <div className="p-5 bg-slate-900/80 border border-slate-800 rounded-3xl flex items-start gap-4">
        <div className="p-2.5 bg-emerald-500/10 text-emerald-400 rounded-2xl shrink-0">
          <ShieldCheck className="w-5 h-5" />
        </div>
        <div className="text-xs leading-relaxed text-slate-300">
          <strong className="text-white block mb-1">Founder Manual Cashout Safeguards</strong>
          Cashout requests are reviewed by the founder twice weekly (e.g. Mondays &amp; Thursdays). The founder sends real money from their Easypaisa, JazzCash, or bank account directly to your account and records the reference code. Minimum cashout is 10,000 FitCoins (Rs. 100).
        </div>
      </div>

      {/* Cashout History */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6">
        <h3 className="text-base font-bold text-white mb-4">Cashout Requests &amp; Payout History</h3>

        {cashouts.length === 0 ? (
          <div className="p-8 rounded-2xl bg-slate-950 border border-slate-800 text-center text-xs text-slate-400">
            No cashout requests yet. Accumulate 10,000 FitCoins to request your first withdrawal.
          </div>
        ) : (
          <div className="space-y-3">
            {cashouts.map((item) => (
              <div
                key={item.id}
                className="p-4 bg-slate-950 border border-slate-800 rounded-2xl flex flex-col sm:flex-row sm:items-center justify-between gap-3"
              >
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-white text-sm uppercase">
                      {item.method} Payout
                    </span>
                    <span
                      className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded ${
                        item.status === 'paid'
                          ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                          : item.status === 'rejected'
                          ? 'bg-red-500/10 text-red-400 border border-red-500/20'
                          : 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                      }`}
                    >
                      {item.status === 'paid'
                        ? 'PAID TO ACCOUNT'
                        : item.status === 'rejected'
                        ? 'REJECTED & REFUNDED'
                        : 'UNDER FOUNDER REVIEW'}
                    </span>
                  </div>

                  <p className="text-xs text-slate-400 mt-1 font-mono">
                    Account: {item.accountTitle} ({item.accountNumber})
                  </p>

                  {item.providerReference && (
                    <div className="mt-1 text-xs text-emerald-300 font-mono flex items-center gap-1.5">
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      <span>TRX ID: {item.providerReference}</span>
                    </div>
                  )}
                </div>

                <div className="text-right shrink-0">
                  <span className="text-base font-black text-emerald-400 tabular-nums">
                    Rs. {item.rupees.toFixed(2)}
                  </span>
                  <span className="text-xs text-slate-500 block font-mono">
                    {item.coins.toLocaleString()} Coins • {item.createdAt}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Cashout Modal */}
      {showCashoutModal && <CashoutModal onClose={() => setShowCashoutModal(false)} />}
    </div>
  );
};
