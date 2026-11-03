import React from 'react';
import { ShieldCheck, Trophy, Sparkles, CheckCircle2, Lock, Flame } from 'lucide-react';
import { useFitPaisa } from '../../context/FitPaisaContext';

export const LevelsTab: React.FC = () => {
  const { user } = useFitPaisa();

  const levels = [
    {
      level: 1,
      name: 'Starter',
      threshold: 0,
      dailyCap: 10000,
      dailyAds: 5,
      multiplier: '1.0x',
      desc: 'Your starting journey. 10,000 daily step cap.',
    },
    {
      level: 2,
      name: 'Walker',
      threshold: 50000,
      dailyCap: 12000,
      dailyAds: 6,
      multiplier: '1.1x',
      desc: 'Bigger daily step cap (+2,000) and 10% ad coin bonus.',
    },
    {
      level: 3,
      name: 'Active',
      threshold: 150000,
      dailyCap: 15000,
      dailyAds: 8,
      multiplier: '1.25x',
      desc: '15,000 daily cap, 8 daily ads, 25% ad bonus multiplier.',
    },
    {
      level: 4,
      name: 'Athlete',
      threshold: 300000,
      dailyCap: 20000,
      dailyAds: 10,
      multiplier: '1.5x',
      desc: 'Higher limits for serious walkers. 20k steps cap, 50% bonus.',
    },
    {
      level: 5,
      name: 'Champion',
      threshold: 500000,
      dailyCap: 25000,
      dailyAds: 12,
      multiplier: '2.0x',
      desc: 'The pinnacle. 25,000 step cap, 12 daily ads, DOUBLE ad coins!',
    },
  ];

  const achievements = [
    {
      id: 'first_10k',
      title: 'First 10,000 Step Day',
      desc: 'Walked 10k steps in a single calendar day.',
      unlocked: user.todaySteps >= 10000,
    },
    {
      id: 'streak_7',
      title: '7-Day Walking Streak',
      desc: 'Converted steps 7 days in a row (+25 coin bonus).',
      unlocked: user.streakDays >= 7,
    },
    {
      id: 'streak_30',
      title: '30-Day Master Streak',
      desc: 'Converted steps 30 days in a row (+100 coin bonus).',
      unlocked: user.streakDays >= 30,
    },
    {
      id: 'lifetime_50k',
      title: '50,000 Lifetime Steps',
      desc: 'Advanced past the Starter rank to Walker tier.',
      unlocked: user.lifetimeSteps >= 50000,
    },
    {
      id: 'lifetime_100k',
      title: '100,000 Lifetime Steps',
      desc: 'One hundred thousand steps recorded and verified.',
      unlocked: user.lifetimeSteps >= 100000,
    },
    {
      id: 'challenge_champ',
      title: 'Daily Challenge Conquered',
      desc: 'Hit today’s 8,000 challenge target and claimed bonus.',
      unlocked: user.dailyChallengeClaimed,
    },
  ];

  return (
    <div className="space-y-4 sm:space-y-6">
      {/* Current Level Header */}
      <div className="p-4 sm:p-6 bg-gradient-to-br from-slate-900 to-slate-950 border border-slate-800 rounded-2xl sm:rounded-3xl flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-semibold mb-2">
            <Trophy className="w-3.5 h-3.5" />
            <span>Tier Status &amp; Achievements</span>
          </div>
          <h2 className="text-xl sm:text-2xl font-black text-white">
            Level {user.level}: {user.levelName}
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Lifetime Steps: <strong className="text-white font-mono">{user.lifetimeSteps.toLocaleString()}</strong> steps
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="p-3 bg-slate-950 border border-slate-800 rounded-2xl text-center">
            <span className="text-[10px] text-slate-400 uppercase font-mono block">Daily Cap</span>
            <span className="text-sm font-bold text-emerald-400">{user.dailyStepCap.toLocaleString()}</span>
          </div>
          <div className="p-3 bg-slate-950 border border-slate-800 rounded-2xl text-center">
            <span className="text-[10px] text-slate-400 uppercase font-mono block">Streak</span>
            <span className="text-sm font-bold text-amber-400 flex items-center justify-center gap-1">
              <Flame className="w-3.5 h-3.5 fill-current" /> {user.streakDays}d
            </span>
          </div>
        </div>
      </div>

      {/* Level Hierarchy Ladder */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6">
        <h3 className="text-base font-bold text-white mb-4">FitPaisa Tier Progression</h3>
        <div className="space-y-3">
          {levels.map((lvl) => {
            const isCurrent = user.level === lvl.level;
            const isUnlocked = user.level >= lvl.level;

            return (
              <div
                key={lvl.level}
                className={`p-4 rounded-2xl border transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-3 ${
                  isCurrent
                    ? 'bg-emerald-500/10 border-emerald-500/40 shadow-sm shadow-emerald-500/10'
                    : isUnlocked
                    ? 'bg-slate-950 border-slate-800/80'
                    : 'bg-slate-950/40 border-slate-900 opacity-60'
                }`}
              >
                <div className="flex items-start gap-3">
                  <div
                    className={`w-9 h-9 rounded-xl flex items-center justify-center font-bold text-sm shrink-0 ${
                      isCurrent
                        ? 'bg-emerald-500 text-slate-950'
                        : isUnlocked
                        ? 'bg-slate-800 text-emerald-400'
                        : 'bg-slate-900 text-slate-600'
                    }`}
                  >
                    {isUnlocked ? lvl.level : <Lock className="w-4 h-4" />}
                  </div>

                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-bold text-white">{lvl.name}</span>
                      {isCurrent && (
                        <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-500 text-slate-950 font-bold">
                          CURRENT TIER
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-slate-400 mt-0.5">{lvl.desc}</p>
                  </div>
                </div>

                <div className="flex items-center gap-4 text-xs font-mono shrink-0">
                  <div className="text-left sm:text-right">
                    <span className="text-slate-500 text-[10px] block">Threshold</span>
                    <span className="text-slate-300 font-semibold">{lvl.threshold.toLocaleString()} steps</span>
                  </div>
                  <div className="text-left sm:text-right">
                    <span className="text-slate-500 text-[10px] block">Daily Cap</span>
                    <span className="text-emerald-400 font-bold">{lvl.dailyCap.toLocaleString()}</span>
                  </div>
                  <div className="text-left sm:text-right">
                    <span className="text-slate-500 text-[10px] block">Bonus</span>
                    <span className="text-indigo-300 font-bold">{lvl.multiplier}</span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Achievements Badges Grid */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6">
        <h3 className="text-base font-bold text-white mb-4">Badges &amp; Milestone Achievements</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
          {achievements.map((ach) => (
            <div
              key={ach.id}
              className={`p-4 rounded-2xl border flex items-start gap-3 ${
                ach.unlocked
                  ? 'bg-slate-950 border-emerald-500/30'
                  : 'bg-slate-950/40 border-slate-800/60 opacity-50'
              }`}
            >
              <div
                className={`p-2.5 rounded-xl shrink-0 ${
                  ach.unlocked
                    ? 'bg-emerald-500/20 text-emerald-400'
                    : 'bg-slate-900 text-slate-600'
                }`}
              >
                {ach.unlocked ? (
                  <CheckCircle2 className="w-5 h-5" />
                ) : (
                  <Lock className="w-5 h-5" />
                )}
              </div>
              <div>
                <h4 className="text-xs font-bold text-white">{ach.title}</h4>
                <p className="text-[11px] text-slate-400 mt-0.5 leading-snug">{ach.desc}</p>
                <span
                  className={`text-[10px] font-mono mt-2 inline-block font-bold ${
                    ach.unlocked ? 'text-emerald-400' : 'text-slate-600'
                  }`}
                >
                  {ach.unlocked ? 'UNLOCKED' : 'LOCKED'}
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
