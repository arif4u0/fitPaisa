import React, { useState } from 'react';
import {
  TrendingUp,
  TrendingDown,
  DollarSign,
  AlertCircle,
  HelpCircle,
  Coins,
  CheckCircle2,
  Sparkles,
  PieChart,
  Layers,
} from 'lucide-react';
import { calculateEconomics } from '../utils/economicSimulator';

export const EconomicsSection: React.FC = () => {
  const [ecpmUsd, setEcpmUsd] = useState<number>(1.2);
  const [usdToPkrRate, setUsdToPkrRate] = useState<number>(280);
  const [stepsPerCoin, setStepsPerCoin] = useState<number>(100);
  const [coinValuePaisa, setCoinValuePaisa] = useState<number>(1);
  const [dailyStepCap, setDailyStepCap] = useState<number>(10000);
  const [adsWatchedPerConvert, setAdsWatchedPerConvert] = useState<number>(1);
  const [dailyBonusCoins, setDailyBonusCoins] = useState<number>(10);
  const [activeUsers, setActiveUsers] = useState<number>(1000);

  const econ = calculateEconomics({
    ecpmUsd,
    usdToPkrRate,
    stepsPerCoin,
    coinValuePaisa,
    dailyStepCap,
    adsWatchedPerConvert,
    dailyBonusCoins,
    activeUsers,
  });

  return (
    <div className="space-y-8">
      {/* Financial Reality Check Banner */}
      <div
        className={`p-6 rounded-2xl border ${
          econ.solvencyStatus === 'CRITICAL_DEFICIT'
            ? 'bg-red-500/10 border-red-500/30'
            : econ.solvencyStatus === 'BREAK_EVEN'
            ? 'bg-amber-500/10 border-amber-500/30'
            : 'bg-emerald-500/10 border-emerald-500/30'
        }`}
      >
        <div className="flex items-start gap-4">
          <div
            className={`p-3 rounded-xl ${
              econ.solvencyStatus === 'CRITICAL_DEFICIT'
                ? 'bg-red-500/20 text-red-400'
                : 'bg-emerald-500/20 text-emerald-400'
            }`}
          >
            {econ.solvencyStatus === 'CRITICAL_DEFICIT' ? (
              <TrendingDown className="w-6 h-6" />
            ) : (
              <TrendingUp className="w-6 h-6" />
            )}
          </div>
          <div>
            <h3 className="text-lg font-bold text-white flex items-center gap-2">
              Economic Viability Audit: Ad Revenue vs Outbound Cash Payout
              <span
                className={`text-xs px-2.5 py-0.5 rounded-full font-mono font-bold ${
                  econ.solvencyStatus === 'CRITICAL_DEFICIT'
                    ? 'bg-red-500/20 text-red-300 border border-red-500/30'
                    : 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                }`}
              >
                {econ.solvencyStatus === 'CRITICAL_DEFICIT'
                  ? 'CRITICAL DEFICIT (FOUNDER LOSES MONEY)'
                  : econ.solvencyStatus === 'BREAK_EVEN'
                  ? 'BREAK-EVEN'
                  : 'HEALTHY PROFIT MARGIN'}
              </span>
            </h3>
            <p className="text-sm text-slate-300 mt-2 leading-relaxed">
              In Pakistan, ad networks (Unity Ads &amp; AdMob) pay an average rewarded video eCPM of{' '}
              <strong className="text-white">$0.80 to $2.00 USD</strong> per 1,000 views.
              <br />
              At 1 ad per conversion, you make approx{' '}
              <strong className="text-emerald-400">Rs 0.34</strong> per user, but you credit 100 coins worth{' '}
              <strong className="text-red-400">Rs 1.00</strong> in real money.
              {econ.solvencyStatus === 'CRITICAL_DEFICIT' && (
                <span className="block mt-1 font-semibold text-red-300">
                  ⚠️ With 1,000 active users, you are projected to lose approx Rs{' '}
                  {Math.abs(Math.round(econ.monthlyNetProfitLossPkr)).toLocaleString()} per month out of your personal wallet!
                </span>
              )}
            </p>
          </div>
        </div>
      </div>

      {/* Interactive Financial Calculator */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-6">
        <h4 className="text-base font-bold text-white pb-4 border-b border-slate-800 flex items-center gap-2">
          <DollarSign className="w-5 h-5 text-emerald-400" />
          Interactive Unit Economics &amp; Break-Even Simulator
        </h4>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mt-6">
          {/* eCPM Slider */}
          <div>
            <div className="flex justify-between text-xs text-slate-300 mb-1">
              <span>Pakistan Ad eCPM (USD)</span>
              <span className="font-mono font-bold text-emerald-400">${ecpmUsd.toFixed(2)}</span>
            </div>
            <input
              type="range"
              min="0.5"
              max="3.0"
              step="0.1"
              value={ecpmUsd}
              onChange={(e) => setEcpmUsd(Number(e.target.value))}
              className="w-full accent-emerald-500 cursor-pointer"
            />
            <span className="text-[11px] text-slate-500 mt-1 block">Pakistan avg: $0.80 - $2.00</span>
          </div>

          {/* Ads Required */}
          <div>
            <div className="flex justify-between text-xs text-slate-300 mb-1">
              <span>Ads Watched per Convert</span>
              <span className="font-mono font-bold text-indigo-400">{adsWatchedPerConvert} ad(s)</span>
            </div>
            <input
              type="range"
              min="1"
              max="5"
              step="1"
              value={adsWatchedPerConvert}
              onChange={(e) => setAdsWatchedPerConvert(Number(e.target.value))}
              className="w-full accent-indigo-500 cursor-pointer"
            />
            <span className="text-[11px] text-slate-500 mt-1 block">Energy bar / multi-stage</span>
          </div>

          {/* Steps per Coin */}
          <div>
            <div className="flex justify-between text-xs text-slate-300 mb-1">
              <span>Steps per 1 FitCoin</span>
              <span className="font-mono font-bold text-white">{stepsPerCoin} steps</span>
            </div>
            <input
              type="range"
              min="50"
              max="300"
              step="25"
              value={stepsPerCoin}
              onChange={(e) => setStepsPerCoin(Number(e.target.value))}
              className="w-full accent-emerald-500 cursor-pointer"
            />
            <span className="text-[11px] text-slate-500 mt-1 block">Current: 100 steps/coin</span>
          </div>

          {/* Active Users */}
          <div>
            <div className="flex justify-between text-xs text-slate-300 mb-1">
              <span>Daily Active Users (DAU)</span>
              <span className="font-mono font-bold text-white">{activeUsers.toLocaleString()}</span>
            </div>
            <input
              type="range"
              min="100"
              max="10000"
              step="100"
              value={activeUsers}
              onChange={(e) => setActiveUsers(Number(e.target.value))}
              className="w-full accent-slate-400 cursor-pointer"
            />
            <span className="text-[11px] text-slate-500 mt-1 block">Scaling capacity</span>
          </div>
        </div>

        {/* Live Metrics Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mt-8">
          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800">
            <span className="text-xs text-slate-400">Ad Revenue / User / Day</span>
            <div className="text-2xl font-black text-emerald-400 mt-1">
              Rs {econ.adRevenuePerUserPkr.toFixed(2)}
            </div>
            <span className="text-[11px] text-slate-500">
              From {adsWatchedPerConvert} ad view(s)
            </span>
          </div>

          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800">
            <span className="text-xs text-slate-400">Real Cash Liability / User</span>
            <div className="text-2xl font-black text-amber-400 mt-1">
              Rs {econ.payoutCostPerUserPkr.toFixed(2)}
            </div>
            <span className="text-[11px] text-slate-500">
              For {econ.totalCoinsPerDay} coins minted
            </span>
          </div>

          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800">
            <span className="text-xs text-slate-400">Net Profit/Loss per User</span>
            <div
              className={`text-2xl font-black mt-1 ${
                econ.netProfitLossPerUserPkr >= 0 ? 'text-emerald-400' : 'text-red-400'
              }`}
            >
              {econ.netProfitLossPerUserPkr >= 0 ? '+' : ''}
              Rs {econ.netProfitLossPerUserPkr.toFixed(2)}
            </div>
            <span className="text-[11px] text-slate-500">
              {econ.netProfitLossPerUserPkr >= 0 ? 'Surplus per user' : 'Out-of-pocket loss'}
            </span>
          </div>

          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800">
            <span className="text-xs text-slate-400">Monthly P&amp;L ({activeUsers} DAU)</span>
            <div
              className={`text-2xl font-black mt-1 ${
                econ.monthlyNetProfitLossPkr >= 0 ? 'text-emerald-400' : 'text-red-400'
              }`}
            >
              {econ.monthlyNetProfitLossPkr >= 0 ? '+' : ''}
              Rs {Math.round(econ.monthlyNetProfitLossPkr).toLocaleString()}
            </div>
            <span className="text-[11px] text-slate-500">
              {econ.monthlyNetProfitLossPkr >= 0 ? 'Sustainable business' : 'Requires founder subsidy'}
            </span>
          </div>
        </div>

        {/* Break Even Tip */}
        <div className="mt-6 p-4 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-between text-xs">
          <div className="text-slate-300">
            <span className="font-bold text-white">Break-Even Threshold:</span> At current eCPM of ${ecpmUsd.toFixed(2)}, you must require at least{' '}
            <strong className="text-emerald-400">{econ.breakEvenAdsRequired} rewarded ads</strong> per 10k steps conversion to avoid losing money!
          </div>
          <button
            onClick={() => setAdsWatchedPerConvert(econ.breakEvenAdsRequired)}
            className="px-3 py-1 bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-300 border border-emerald-500/30 rounded-lg font-semibold cursor-pointer shrink-0 ml-4"
          >
            Auto-Fix to Break-Even ({econ.breakEvenAdsRequired} Ads)
          </button>
        </div>
      </div>

      {/* Recommended Solutions */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-6">
        <h4 className="text-base font-bold text-white pb-4 border-b border-slate-800 flex items-center gap-2">
          <Sparkles className="w-5 h-5 text-amber-400" />
          3 Sustainable Solutions to Protect the Founder's Personal Wallet
        </h4>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-6">
          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800">
            <div className="text-emerald-400 font-bold text-sm mb-2 flex items-center gap-1.5">
              <Layers className="w-4 h-4" />
              1. Step Energy / Chunking Model
            </div>
            <p className="text-xs text-slate-300 leading-relaxed">
              Instead of converting all 10,000 steps with a single ad, convert in chunks:
              <br />
              - 1 Ad = converts 2,500 steps (25 coins = Rs 0.25).
              <br />
              - 4 Ads per day to convert full 10,000 steps.
              <br />
              Now 4 ads generate ~Rs 1.36 revenue, while paying out Rs 1.00. You earn a net profit of{' '}
              <strong className="text-emerald-400">+Rs 0.36</strong> per user!
            </p>
          </div>

          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800">
            <div className="text-emerald-400 font-bold text-sm mb-2 flex items-center gap-1.5">
              <Coins className="w-4 h-4" />
              2. Product-First Redemption Catalog
            </div>
            <p className="text-xs text-slate-300 leading-relaxed">
              Nudge users toward physical merchandise (water bottles, fitness bands, earphones, discount coupons).
              <br />
              Merchandise has wholesale margins (e.g. buying earbuds for Rs 300 and pricing at 50,000 coins). Users love physical rewards, and fulfillment costs you less than hard cash transfers!
            </p>
          </div>

          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800">
            <div className="text-emerald-400 font-bold text-sm mb-2 flex items-center gap-1.5">
              <PieChart className="w-4 h-4" />
              3. Hybrid Ad Mediation Waterfall
            </div>
            <p className="text-xs text-slate-300 leading-relaxed">
              Don't rely solely on Unity Ads:
              <br />
              - Re-enable AdMob as Tier 1 (higher Pakistani fill and eCPM of $1.50 - $2.50).
              <br />
              - Use Unity Ads as Tier 2 fallback.
              <br />
              - Add interstitial ad between screens (e.g. on leaving Wallet or Leaderboard).
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
