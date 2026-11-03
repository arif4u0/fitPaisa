import React, { useState, useEffect } from 'react';
import { Volume2, VolumeX, X, Tv, ShieldCheck, Sparkles, CheckCircle2 } from 'lucide-react';
import { useFitPaisa } from '../../context/FitPaisaContext';

export const RewardedAdModal: React.FC = () => {
  const { isAdPlaying, adContext, closeAdModal } = useFitPaisa();
  const [secondsRemaining, setSecondsRemaining] = useState(10);
  const [isMuted, setIsMuted] = useState(false);
  const [showSkipWarning, setShowSkipWarning] = useState(false);

  useEffect(() => {
    if (!isAdPlaying) return;
    setSecondsRemaining(10);
    setShowSkipWarning(false);

    const timer = setInterval(() => {
      setSecondsRemaining((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [isAdPlaying]);

  if (!isAdPlaying || !adContext) return null;

  const isCompleted = secondsRemaining === 0;

  const handleCloseAttempt = () => {
    if (isCompleted) {
      closeAdModal(true);
    } else {
      setShowSkipWarning(true);
    }
  };

  const handleConfirmSkip = () => {
    setShowSkipWarning(false);
    closeAdModal(false);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md">
      <div className="relative w-full max-w-lg bg-slate-900 border border-slate-800 rounded-3xl overflow-hidden shadow-2xl flex flex-col">
        {/* Ad Header Bar */}
        <div className="px-5 py-3.5 bg-slate-950/90 border-b border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-6 h-6 rounded-lg bg-indigo-500/20 text-indigo-400 flex items-center justify-center">
              <Tv className="w-3.5 h-3.5" />
            </div>
            <div>
              <span className="text-xs font-bold text-white tracking-wide uppercase font-mono">
                Unity Ads Rewarded Player
              </span>
              <span className="text-[10px] text-slate-400 block font-mono">
                Placement: Rewarded_Android • Test Mode
              </span>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setIsMuted(!isMuted)}
              className="p-1.5 rounded-lg text-slate-400 hover:text-white bg-slate-800/80 cursor-pointer"
            >
              {isMuted ? <VolumeX className="w-4 h-4" /> : <Volume2 className="w-4 h-4" />}
            </button>
            <button
              onClick={handleCloseAttempt}
              className="p-1.5 rounded-lg text-slate-400 hover:text-white bg-slate-800/80 cursor-pointer"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Video Canvas Simulation */}
        <div className="relative h-64 sm:h-72 bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950/60 flex flex-col items-center justify-center p-6 text-center overflow-hidden">
          {/* Animated background glow */}
          <div className="absolute w-56 h-56 bg-emerald-500/10 rounded-full blur-3xl -top-10 -right-10 pointer-events-none"></div>
          <div className="absolute w-56 h-56 bg-indigo-500/10 rounded-full blur-3xl -bottom-10 -left-10 pointer-events-none"></div>

          {/* Ad Sponsor Showcase */}
          <div className="relative z-10 flex flex-col items-center max-w-xs">
            <div className="w-16 h-16 rounded-2xl bg-gradient-to-tr from-emerald-500 to-teal-400 flex items-center justify-center text-slate-950 shadow-lg shadow-emerald-500/30 font-black text-2xl mb-3">
              FP
            </div>
            <h4 className="text-lg font-extrabold text-white">FitPaisa Walk-to-Earn</h4>
            <p className="text-xs text-slate-300 mt-1 leading-relaxed">
              Step tracker that turns your daily physical activity into genuine mobile wallet cash &amp; fitness rewards.
            </p>

            <div className="mt-4 inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-500/15 border border-emerald-500/30 text-emerald-300 text-xs font-semibold">
              <ShieldCheck className="w-3.5 h-3.5" />
              <span>Official Rewarded Video Stream</span>
            </div>
          </div>

          {/* Countdown Overlay */}
          <div className="absolute top-4 left-4 z-20">
            {isCompleted ? (
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-emerald-500 text-slate-950 text-xs font-bold">
                <CheckCircle2 className="w-3.5 h-3.5" /> Reward Unlocked!
              </span>
            ) : (
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-black/60 border border-slate-700 text-slate-200 text-xs font-mono font-bold backdrop-blur-sm">
                Reward in {secondsRemaining}s
              </span>
            )}
          </div>
        </div>

        {/* Progress Bar */}
        <div className="w-full bg-slate-950 h-1.5">
          <div
            className="h-full bg-gradient-to-r from-emerald-500 to-teal-400 transition-all duration-1000 ease-linear"
            style={{ width: `${((10 - secondsRemaining) / 10) * 100}%` }}
          ></div>
        </div>

        {/* Bottom Action Footer */}
        <div className="p-5 bg-slate-950 flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="text-xs text-slate-400 text-center sm:text-left">
            {isCompleted ? (
              <span className="text-emerald-400 font-semibold flex items-center gap-1">
                <Sparkles className="w-3.5 h-3.5" /> Ad completed! Tap below to claim your coins.
              </span>
            ) : (
              <span>Watch to the end to confirm coin credit on the server.</span>
            )}
          </div>

          {isCompleted ? (
            <button
              onClick={() => closeAdModal(true)}
              className="w-full sm:w-auto px-6 py-2.5 bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-xs rounded-xl shadow-lg shadow-emerald-500/25 transition-all cursor-pointer"
            >
              Claim Coins &amp; Finish
            </button>
          ) : (
            <button
              onClick={handleCloseAttempt}
              className="text-xs text-slate-400 hover:text-slate-200 transition-colors cursor-pointer"
            >
              Close Ad Early
            </button>
          )}
        </div>

        {/* Skip Warning Modal Overlay */}
        {showSkipWarning && (
          <div className="absolute inset-0 bg-slate-950/95 z-30 flex flex-col items-center justify-center p-6 text-center">
            <h5 className="text-base font-bold text-white">Leave without your reward?</h5>
            <p className="text-xs text-slate-300 mt-2 max-w-xs leading-relaxed">
              If you close this ad now, no coins will be credited for your conversion. Only {secondsRemaining} seconds left!
            </p>
            <div className="flex gap-3 mt-5">
              <button
                onClick={() => setShowSkipWarning(false)}
                className="px-4 py-2 bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-xs rounded-xl cursor-pointer"
              >
                Keep Watching
              </button>
              <button
                onClick={handleConfirmSkip}
                className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs rounded-xl cursor-pointer"
              >
                Skip &amp; Forfeit
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
