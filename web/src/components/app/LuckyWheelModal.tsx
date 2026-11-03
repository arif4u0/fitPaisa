import React, { useState } from 'react';
import { X, Sparkles, Coins, RotateCw, CheckCircle2 } from 'lucide-react';
import { useFitPaisa } from '../../context/FitPaisaContext';

interface LuckyWheelModalProps {
  onClose: () => void;
}

export const LuckyWheelModal: React.FC<LuckyWheelModalProps> = ({ onClose }) => {
  const { user, spinWheel, triggerSpinAd } = useFitPaisa();
  const [isSpinning, setIsSpinning] = useState(false);
  const [rotation, setRotation] = useState(0);
  const [wonPrize, setWonPrize] = useState<number | null>(null);

  const prizes = [5, 10, 15, 25, 50, 100];
  const sliceColors = [
    '#059669', // Emerald
    '#0284c7', // Sky
    '#7c3aed', // Violet
    '#d97706', // Amber
    '#dc2626', // Red
    '#eab308', // Gold
  ];

  const handleSpin = (isFree: boolean) => {
    if (isSpinning) return;
    setIsSpinning(true);
    setWonPrize(null);

    // Pick prize via context (weighted server-side logic)
    const prize = spinWheel(isFree);

    // Find the prize slice index to calculate landing angle
    const prizeIndex = prizes.indexOf(prize);
    const sliceAngle = 360 / prizes.length;
    // Calculate final angle (extra spins + slice offset)
    const extraSpins = 5 * 360;
    const targetAngle = extraSpins + (360 - (prizeIndex * sliceAngle + sliceAngle / 2));

    setRotation((prev) => prev + targetAngle);

    setTimeout(() => {
      setIsSpinning(false);
      setWonPrize(prize);
    }, 4000);
  };

  const handleWatchAdSpin = () => {
    onClose();
    triggerSpinAd();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
      <div className="relative w-full max-w-sm bg-slate-900 border border-slate-800 rounded-3xl p-6 shadow-2xl flex flex-col items-center">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-1.5 rounded-full text-slate-400 hover:text-white bg-slate-800/80 cursor-pointer"
        >
          <X className="w-4 h-4" />
        </button>

        <div className="text-center mb-4">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-500/10 border border-amber-500/20 text-amber-400 text-xs font-semibold mb-2">
            <Sparkles className="w-3.5 h-3.5" />
            <span>Lucky Spin Wheel</span>
          </div>
          <h3 className="text-lg font-extrabold text-white">Daily Lucky Draw</h3>
          <p className="text-xs text-slate-400 mt-1">Spin to win instant FitCoins straight to your wallet!</p>
        </div>

        {/* Wheel Canvas / SVG */}
        <div className="relative w-64 h-64 my-4 flex items-center justify-center">
          {/* Wheel Pointer Pin */}
          <div className="absolute top-0 left-1/2 -translate-x-1/2 -translate-y-2 z-20 w-0 h-0 border-x-8 border-x-transparent border-t-[18px] border-t-amber-400 filter drop-shadow"></div>

          {/* Rotating Wheel Container */}
          <div
            className="w-full h-full rounded-full border-4 border-amber-400/80 shadow-xl overflow-hidden relative transition-transform duration-[4000ms] ease-[cubic-bezier(0.15,0.9,0.2,1)]"
            style={{ transform: `rotate(${rotation}deg)` }}
          >
            <svg viewBox="0 0 100 100" className="w-full h-full">
              {prizes.map((prize, idx) => {
                const angle = 360 / prizes.length;
                const startAngle = idx * angle;
                const endAngle = (idx + 1) * angle;

                const x1 = 50 + 50 * Math.cos((Math.PI * (startAngle - 90)) / 180);
                const y1 = 50 + 50 * Math.sin((Math.PI * (startAngle - 90)) / 180);
                const x2 = 50 + 50 * Math.cos((Math.PI * (endAngle - 90)) / 180);
                const y2 = 50 + 50 * Math.sin((Math.PI * (endAngle - 90)) / 180);

                const pathData = `M 50 50 L ${x1} ${y1} A 50 50 0 0 1 ${x2} ${y2} Z`;
                const textAngle = startAngle + angle / 2;

                return (
                  <g key={idx}>
                    <path d={pathData} fill={sliceColors[idx]} stroke="#0f172a" strokeWidth="0.5" />
                    <text
                      x="50"
                      y="18"
                      fill="#ffffff"
                      fontSize="6.5"
                      fontWeight="bold"
                      textAnchor="middle"
                      transform={`rotate(${textAngle}, 50, 50)`}
                    >
                      {prize}
                    </text>
                  </g>
                );
              })}
            </svg>

            {/* Wheel Center Hub */}
            <div className="absolute inset-0 m-auto w-12 h-12 rounded-full bg-slate-900 border-2 border-amber-400 flex items-center justify-center shadow-lg">
              <Coins className="w-5 h-5 text-amber-400" />
            </div>
          </div>
        </div>

        {/* Won Prize Celebration */}
        {wonPrize !== null && !isSpinning && (
          <div className="w-full p-3.5 mb-4 bg-emerald-500/15 border border-emerald-500/30 rounded-2xl text-center">
            <span className="text-xs font-bold text-emerald-400 flex items-center justify-center gap-1">
              <CheckCircle2 className="w-4 h-4" /> Congratulations!
            </span>
            <div className="text-xl font-black text-white mt-0.5">+{wonPrize} FitCoins Won!</div>
            <span className="text-[11px] text-slate-300">Added directly to your spendable balance.</span>
          </div>
        )}

        {/* Action Buttons */}
        <div className="w-full space-y-2 mt-2">
          {user.freeSpinAvailable ? (
            <button
              disabled={isSpinning}
              onClick={() => handleSpin(true)}
              className="w-full py-3 bg-gradient-to-r from-amber-500 to-amber-400 hover:from-amber-400 hover:to-amber-300 text-slate-950 font-extrabold text-sm rounded-xl shadow-lg shadow-amber-500/20 transition-all cursor-pointer disabled:opacity-50 flex items-center justify-center gap-2"
            >
              <RotateCw className={`w-4 h-4 ${isSpinning ? 'animate-spin' : ''}`} />
              {isSpinning ? 'Spinning Wheel...' : 'Use Free Daily Spin'}
            </button>
          ) : (
            <button
              disabled={isSpinning}
              onClick={handleWatchAdSpin}
              className="w-full py-3 bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-extrabold text-sm rounded-xl shadow-lg shadow-emerald-500/20 transition-all cursor-pointer disabled:opacity-50 flex items-center justify-center gap-2"
            >
              <RotateCw className="w-4 h-4" />
              Watch Rewarded Ad for Extra Spin
            </button>
          )}

          <p className="text-[11px] text-slate-500 text-center">
            {user.freeSpinAvailable
              ? '1 Free spin every 24 hours. Extra spins with rewarded ads.'
              : 'Free spin used today. Watch an ad to spin again!'}
          </p>
        </div>
      </div>
    </div>
  );
};
