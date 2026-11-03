import React, { useState, useEffect } from 'react';
import {
  Flame,
  Tv,
  Coins,
  Footprints,
  RotateCw,
  Sparkles,
  Trophy,
  Play,
  Square,
  Zap,
  CheckCircle2,
  Clock,
  ArrowRight,
  TrendingUp,
} from 'lucide-react';
import { useFitPaisa } from '../../context/FitPaisaContext';
import { LuckyWheelModal } from './LuckyWheelModal';

export const HomeTab: React.FC = () => {
  const { user, addSteps, triggerConvert, triggerDailyAd } = useFitPaisa();
  const [showSpinModal, setShowSpinModal] = useState(false);
  const [autoWalkActive, setAutoWalkActive] = useState(false);

  // Auto-walk simulator (simulates 2 steps per second = 120 steps/min human walking cadence)
  useEffect(() => {
    if (!autoWalkActive) return;
    const interval = setInterval(() => {
      addSteps(4);
    }, 1000);
    return () => clearInterval(interval);
  }, [autoWalkActive, addSteps]);

  const eligibleSteps = Math.min(user.todaySteps, user.dailyStepCap);
  const uncreditedSteps = Math.max(0, eligibleSteps - user.creditedTodaySteps);
  const coinsToEarn = Math.floor(uncreditedSteps / 100);
  const progressPercent = Math.min(100, (user.todaySteps / user.dailyStepCap) * 100);

  return (
    <div className="space-y-6">
      {/* Top User Greeting & Streak Bar */}
      <div className="flex items-center justify-between p-4 bg-slate-900/80 border border-slate-800 rounded-3xl">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-emerald-500 to-teal-400 flex items-center justify-center text-slate-950 font-black text-xl shadow-lg shadow-emerald-500/20">
            {user.name[0]}
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-base font-extrabold text-white">Hello, {user.name}!</h2>
              <span className="text-[11px] px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-semibold">
                Level {user.level} {user.levelName}
              </span>
            </div>
            <p className="text-xs text-slate-400">Walk today, earn tomorrow • 100 steps = 1 coin</p>
          </div>
        </div>

        <div className="flex items-center gap-2 px-3 py-1.5 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-amber-400">
          <Flame className="w-4 h-4 fill-amber-400 text-amber-400 animate-pulse" />
          <div className="text-right">
            <span className="text-xs font-bold font-mono">{user.streakDays}-Day</span>
            <span className="text-[10px] text-amber-300/80 block">Streak</span>
          </div>
        </div>
      </div>

      {/* Main Step Counter Display */}
      <div className="relative overflow-hidden p-6 sm:p-8 bg-gradient-to-b from-slate-900 to-slate-950 border border-slate-800 rounded-3xl shadow-xl flex flex-col items-center text-center">
        {/* Step Circular Visualizer */}
        <div className="relative w-56 h-56 flex items-center justify-center my-2">
          <svg viewBox="0 0 100 100" className="w-full h-full -rotate-90">
            {/* Background Ring */}
            <circle
              cx="50"
              cy="50"
              r="42"
              fill="transparent"
              stroke="#1e293b"
              strokeWidth="7"
            />
            {/* Progress Fill */}
            <circle
              cx="50"
              cy="50"
              r="42"
              fill="transparent"
              stroke="url(#emeraldGradient)"
              strokeWidth="7"
              strokeDasharray={264}
              strokeDashoffset={264 - (264 * progressPercent) / 100}
              strokeLinecap="round"
              className="transition-all duration-700 ease-out"
            />
            <defs>
              <linearGradient id="emeraldGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#10b981" />
                <stop offset="100%" stopColor="#06b6d4" />
              </linearGradient>
            </defs>
          </svg>

          {/* Central Stats */}
          <div className="absolute inset-0 flex flex-col items-center justify-center">
            <Footprints className="w-6 h-6 text-emerald-400 mb-1" />
            <span className="text-4xl sm:text-5xl font-black text-white tracking-tight tabular-nums">
              {user.todaySteps.toLocaleString()}
            </span>
            <span className="text-xs text-slate-400 mt-0.5">
              of {user.dailyStepCap.toLocaleString()} daily cap
            </span>
            <div className="mt-1 text-[11px] font-semibold text-emerald-400">
              {progressPercent.toFixed(0)}% Completed
            </div>
          </div>
        </div>

        {/* Pedometer Simulator Controls */}
        <div className="flex flex-wrap items-center justify-center gap-2 mt-4 pt-4 border-t border-slate-800/80 w-full">
          <span className="text-[11px] text-slate-500 font-medium mr-1">Simulate Pedometer:</span>
          <button
            onClick={() => addSteps(100)}
            className="px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs rounded-lg transition-all cursor-pointer font-medium"
          >
            +100 Steps
          </button>
          <button
            onClick={() => addSteps(1000)}
            className="px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs rounded-lg transition-all cursor-pointer font-medium"
          >
            +1,000 Steps
          </button>
          <button
            onClick={() => setAutoWalkActive(!autoWalkActive)}
            className={`px-3 py-1 text-xs rounded-lg transition-all font-semibold flex items-center gap-1.5 cursor-pointer ${
              autoWalkActive
                ? 'bg-red-500/20 text-red-300 border border-red-500/30'
                : 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
            }`}
          >
            {autoWalkActive ? (
              <>
                <Square className="w-3 h-3 fill-current" /> Stop Auto-Walk
              </>
            ) : (
              <>
                <Play className="w-3 h-3 fill-current" /> Auto-Walk (120/min)
              </>
            )}
          </button>
        </div>

        {/* Convert CTA Card */}
        <div className="w-full mt-6 p-4 rounded-2xl bg-slate-950 border border-slate-800/80 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="text-center sm:text-left">
            <div className="flex items-center justify-center sm:justify-start gap-1.5 text-xs text-slate-400">
              <Zap className="w-3.5 h-3.5 text-amber-400" />
              <span>Unconverted Steps:</span>
              <strong className="text-white font-mono">{uncreditedSteps.toLocaleString()} steps</strong>
            </div>
            <div className="text-lg font-black text-white mt-0.5">
              Worth{' '}
              <span className="text-emerald-400">+{coinsToEarn} FitCoins</span>{' '}
              <span className="text-xs text-slate-400 font-normal">
                (Rs. {(coinsToEarn / 100).toFixed(2)})
              </span>
            </div>
          </div>

          <button
            disabled={coinsToEarn < 1}
            onClick={triggerConvert}
            className="w-full sm:w-auto px-6 py-3 bg-gradient-to-r from-emerald-500 to-teal-400 hover:from-emerald-400 hover:to-teal-300 text-slate-950 font-black text-sm rounded-xl shadow-lg shadow-emerald-500/20 transition-all cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            <Tv className="w-4 h-4" />
            <span>Watch Ad &amp; Convert (+{coinsToEarn} Coins)</span>
          </button>
        </div>
      </div>

      {/* 2-Column Action Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Lucky Spin Wheel Card */}
        <div className="p-5 bg-slate-900/80 border border-slate-800 rounded-3xl flex items-center justify-between">
          <div className="space-y-1">
            <div className="flex items-center gap-1.5 text-xs font-bold text-amber-400">
              <Sparkles className="w-3.5 h-3.5" />
              <span>Daily Lucky Draw</span>
            </div>
            <h4 className="text-base font-extrabold text-white">Spin the Wheel</h4>
            <p className="text-xs text-slate-400">Win up to 100 bonus FitCoins every day</p>
          </div>
          <button
            onClick={() => setShowSpinModal(true)}
            className="px-4 py-2 bg-amber-500 hover:bg-amber-400 text-slate-950 font-extrabold text-xs rounded-xl shadow-lg shadow-amber-500/20 transition-all cursor-pointer flex items-center gap-1.5"
          >
            <RotateCw className="w-3.5 h-3.5" />
            <span>{user.freeSpinAvailable ? 'Free Spin' : 'Spin Wheel'}</span>
          </button>
        </div>

        {/* Daily Ads Card */}
        <div className="p-5 bg-slate-900/80 border border-slate-800 rounded-3xl flex items-center justify-between">
          <div className="space-y-1">
            <div className="flex items-center gap-1.5 text-xs font-bold text-indigo-400">
              <Tv className="w-3.5 h-3.5" />
              <span>Daily Rewarded Ads</span>
            </div>
            <h4 className="text-base font-extrabold text-white">
              {user.dailyAdsWatched} / {user.dailyAdLimit} Watched
            </h4>
            <p className="text-xs text-slate-400">Earn +{user.dailyAdCoins} instant coins per ad</p>
          </div>
          <button
            disabled={user.dailyAdsWatched >= user.dailyAdLimit}
            onClick={triggerDailyAd}
            className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white font-extrabold text-xs rounded-xl shadow-lg shadow-indigo-600/20 transition-all cursor-pointer disabled:opacity-40 flex items-center gap-1.5"
          >
            <Play className="w-3.5 h-3.5 fill-current" />
            <span>Watch (+{user.dailyAdCoins})</span>
          </button>
        </div>
      </div>

      {/* Daily Challenge Card */}
      <div className="p-5 bg-slate-900/80 border border-slate-800 rounded-3xl">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Trophy className="w-4 h-4 text-emerald-400" />
            <h4 className="text-sm font-bold text-white">Daily Walking Challenge</h4>
          </div>
          <span className="text-xs font-bold text-emerald-400">+{user.dailyChallengeBonus} Coins Bonus</span>
        </div>

        <p className="text-xs text-slate-400 mb-2">
          Hit {user.dailyChallengeTarget.toLocaleString()} steps today to claim an instant bonus of 50 FitCoins.
        </p>

        <div className="w-full bg-slate-950 h-2 rounded-full overflow-hidden">
          <div
            className="h-full bg-emerald-500 rounded-full transition-all duration-500"
            style={{
              width: `${Math.min(100, (user.todaySteps / user.dailyChallengeTarget) * 100)}%`,
            }}
          ></div>
        </div>

        <div className="flex justify-between items-center text-[11px] text-slate-400 mt-2">
          <span>{user.todaySteps.toLocaleString()} / {user.dailyChallengeTarget.toLocaleString()} steps</span>
          {user.dailyChallengeClaimed ? (
            <span className="text-emerald-400 font-bold flex items-center gap-1">
              <CheckCircle2 className="w-3.5 h-3.5" /> Bonus Claimed!
            </span>
          ) : user.todaySteps >= user.dailyChallengeTarget ? (
            <span className="text-amber-400 font-bold">Ready to convert!</span>
          ) : (
            <span>{(user.dailyChallengeTarget - user.todaySteps).toLocaleString()} steps left</span>
          )}
        </div>
      </div>

      {/* Modals */}
      {showSpinModal && <LuckyWheelModal onClose={() => setShowSpinModal(false)} />}
    </div>
  );
};
