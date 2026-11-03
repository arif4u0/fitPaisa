/**
 * FitPaisa — Official Web App & Architecture Suite
 * Complete Walk-to-Earn Application, Founder Payout Desk, and Engineering Audit Hub.
 */
import React, { useState } from 'react';
import {
  Footprints,
  ShoppingBag,
  Wallet,
  Trophy,
  ShieldCheck,
  Code2,
  Tv,
  Coins,
  Sparkles,
  Smartphone,
  Layers,
} from 'lucide-react';
import { FitPaisaProvider, useFitPaisa } from './context/FitPaisaContext';
import { HomeTab } from './components/app/HomeTab';
import { RewardsTab } from './components/app/RewardsTab';
import { WalletTab } from './components/app/WalletTab';
import { LevelsTab } from './components/app/LevelsTab';
import { RewardedAdModal } from './components/app/RewardedAdModal';
import { AdminPortal } from './components/admin/AdminPortal';
import { UnityAdsSection } from './components/UnityAdsSection';
import { RewardCalculationSection } from './components/RewardCalculationSection';
import { WalletSecuritySection } from './components/WalletSecuritySection';
import { EconomicsSection } from './components/EconomicsSection';
import { CodePatchesSection } from './components/CodePatchesSection';

type MainAppMode = 'app' | 'admin' | 'audit';
type AppTab = 'home' | 'rewards' | 'wallet' | 'levels';
type AuditTab = 'unity-ads' | 'reward-calc' | 'wallet-security' | 'economics' | 'code-patches';

const FitPaisaAppContent: React.FC = () => {
  const { user } = useFitPaisa();
  const [appMode, setAppMode] = useState<MainAppMode>('app');
  const [activeAppTab, setActiveAppTab] = useState<AppTab>('home');
  const [activeAuditTab, setActiveAuditTab] = useState<AuditTab>('unity-ads');

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 font-sans selection:bg-emerald-500 selection:text-slate-950 flex flex-col justify-between">
      {/* Rewarded Ad Player Modal (Global) */}
      <RewardedAdModal />

      {/* Top Main Navigation Header */}
      <header className="sticky top-0 z-40 bg-slate-900/95 backdrop-blur-md border-b border-slate-800">
        <div className="max-w-7xl mx-auto px-3 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-14 sm:h-16 gap-2">
            {/* Brand Logo */}
            <div className="flex items-center gap-2 sm:gap-3 shrink-0">
              <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-xl bg-gradient-to-tr from-emerald-500 to-teal-400 flex items-center justify-center shadow-lg shadow-emerald-500/20 text-slate-950 font-black text-base sm:text-xl">
                FP
              </div>
              <div>
                <div className="flex items-center gap-1.5 sm:gap-2">
                  <span className="font-extrabold text-white text-base sm:text-lg tracking-tight">FitPaisa</span>
                  <span className="text-[10px] sm:text-[11px] px-1.5 sm:px-2 py-0.5 rounded-full font-semibold bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 whitespace-nowrap">
                    Walk &amp; Earn
                  </span>
                </div>
                <p className="text-[10px] sm:text-[11px] text-slate-400 hidden md:block">
                  Steps to FitCoins • Real Money Cashouts
                </p>
              </div>
            </div>

            {/* Mode Switcher Pills (Mobile-optimized with compact labels) */}
            <div className="flex items-center gap-0.5 sm:gap-1 p-0.5 sm:p-1 bg-slate-950 border border-slate-800 rounded-xl sm:rounded-2xl shrink-0">
              <button
                onClick={() => setAppMode('app')}
                className={`flex items-center gap-1 sm:gap-1.5 px-2 sm:px-3 py-1.5 rounded-lg sm:rounded-xl text-[11px] sm:text-xs font-semibold transition-all cursor-pointer ${
                  appMode === 'app'
                    ? 'bg-emerald-500 text-slate-950 shadow-md shadow-emerald-500/20 font-bold'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                <Smartphone className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">App Experience</span>
                <span className="sm:hidden">App</span>
              </button>

              <button
                onClick={() => setAppMode('admin')}
                className={`flex items-center gap-1 sm:gap-1.5 px-2 sm:px-3 py-1.5 rounded-lg sm:rounded-xl text-[11px] sm:text-xs font-semibold transition-all cursor-pointer ${
                  appMode === 'admin'
                    ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/20 font-bold'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                <ShieldCheck className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">Founder Desk</span>
                <span className="sm:hidden">Founder</span>
              </button>

              <button
                onClick={() => setAppMode('audit')}
                className={`flex items-center gap-1 sm:gap-1.5 px-2 sm:px-3 py-1.5 rounded-lg sm:rounded-xl text-[11px] sm:text-xs font-semibold transition-all cursor-pointer ${
                  appMode === 'audit'
                    ? 'bg-amber-500 text-slate-950 font-bold shadow-md shadow-amber-500/20'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                <Code2 className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">Code &amp; Audit</span>
                <span className="sm:hidden">Audit</span>
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Body Content with bottom padding for mobile bottom bar */}
      <main className="max-w-5xl mx-auto w-full px-3 sm:px-6 lg:px-8 py-4 sm:py-6 flex-1 pb-28 md:pb-8">
        {/* MODE 1: APP EXPERIENCE */}
        {appMode === 'app' && (
          <div className="space-y-4 sm:space-y-6">
            {/* Desktop / Tablet Sub-Navigation Bar */}
            <div className="hidden md:flex items-center justify-start gap-1 p-1.5 bg-slate-900/80 border border-slate-800 rounded-2xl">
              <button
                onClick={() => setActiveAppTab('home')}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                  activeAppTab === 'home'
                    ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                <Footprints className="w-4 h-4" />
                <span>Walk &amp; Convert</span>
              </button>

              <button
                onClick={() => setActiveAppTab('rewards')}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                  activeAppTab === 'rewards'
                    ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                <ShoppingBag className="w-4 h-4" />
                <span>Product Store</span>
              </button>

              <button
                onClick={() => setActiveAppTab('wallet')}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                  activeAppTab === 'wallet'
                    ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                <Wallet className="w-4 h-4" />
                <span>Wallet &amp; Cashout</span>
              </button>

              <button
                onClick={() => setActiveAppTab('levels')}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                  activeAppTab === 'levels'
                    ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                <Trophy className="w-4 h-4" />
                <span>Tier Levels</span>
              </button>
            </div>

            {/* Render App Tab */}
            {activeAppTab === 'home' && <HomeTab />}
            {activeAppTab === 'rewards' && <RewardsTab />}
            {activeAppTab === 'wallet' && <WalletTab />}
            {activeAppTab === 'levels' && <LevelsTab />}

            {/* Mobile Native Bottom Navigation Bar */}
            <nav className="md:hidden fixed bottom-0 left-0 right-0 z-50 bg-slate-900/95 backdrop-blur-lg border-t border-slate-800/90 shadow-2xl safe-area-bottom">
              <div className="grid grid-cols-4 h-16 max-w-lg mx-auto px-2">
                <button
                  onClick={() => setActiveAppTab('home')}
                  className={`flex flex-col items-center justify-center gap-1 transition-all cursor-pointer ${
                    activeAppTab === 'home'
                      ? 'text-emerald-400 font-bold'
                      : 'text-slate-400 hover:text-slate-200 font-medium'
                  }`}
                >
                  <div className={`p-1 rounded-xl transition-all ${activeAppTab === 'home' ? 'bg-emerald-500/15' : ''}`}>
                    <Footprints className="w-5 h-5" />
                  </div>
                  <span className="text-[10px] tracking-tight">Walk</span>
                </button>

                <button
                  onClick={() => setActiveAppTab('rewards')}
                  className={`flex flex-col items-center justify-center gap-1 transition-all cursor-pointer ${
                    activeAppTab === 'rewards'
                      ? 'text-emerald-400 font-bold'
                      : 'text-slate-400 hover:text-slate-200 font-medium'
                  }`}
                >
                  <div className={`p-1 rounded-xl transition-all ${activeAppTab === 'rewards' ? 'bg-emerald-500/15' : ''}`}>
                    <ShoppingBag className="w-5 h-5" />
                  </div>
                  <span className="text-[10px] tracking-tight">Store</span>
                </button>

                <button
                  onClick={() => setActiveAppTab('wallet')}
                  className={`flex flex-col items-center justify-center gap-1 transition-all cursor-pointer ${
                    activeAppTab === 'wallet'
                      ? 'text-emerald-400 font-bold'
                      : 'text-slate-400 hover:text-slate-200 font-medium'
                  }`}
                >
                  <div className={`p-1 rounded-xl transition-all ${activeAppTab === 'wallet' ? 'bg-emerald-500/15' : ''}`}>
                    <Wallet className="w-5 h-5" />
                  </div>
                  <span className="text-[10px] tracking-tight">Wallet</span>
                </button>

                <button
                  onClick={() => setActiveAppTab('levels')}
                  className={`flex flex-col items-center justify-center gap-1 transition-all cursor-pointer ${
                    activeAppTab === 'levels'
                      ? 'text-emerald-400 font-bold'
                      : 'text-slate-400 hover:text-slate-200 font-medium'
                  }`}
                >
                  <div className={`p-1 rounded-xl transition-all ${activeAppTab === 'levels' ? 'bg-emerald-500/15' : ''}`}>
                    <Trophy className="w-5 h-5" />
                  </div>
                  <span className="text-[10px] tracking-tight">Tiers</span>
                </button>
              </div>
            </nav>
          </div>
        )}

        {/* MODE 2: FOUNDER DESK */}
        {appMode === 'admin' && <AdminPortal />}

        {/* MODE 3: ARCHITECTURE & CODE AUDIT */}
        {appMode === 'audit' && (
          <div className="space-y-6">
            <div className="flex items-center gap-2 overflow-x-auto pb-2 border-b border-slate-800">
              <button
                onClick={() => setActiveAuditTab('unity-ads')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 whitespace-nowrap ${
                  activeAuditTab === 'unity-ads'
                    ? 'bg-emerald-500 text-slate-950'
                    : 'bg-slate-900 text-slate-400 hover:text-white'
                }`}
              >
                <Tv className="w-3.5 h-3.5" />
                <span>Unity Ads Debugger</span>
              </button>

              <button
                onClick={() => setActiveAuditTab('reward-calc')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 whitespace-nowrap ${
                  activeAuditTab === 'reward-calc'
                    ? 'bg-emerald-500 text-slate-950'
                    : 'bg-slate-900 text-slate-400 hover:text-white'
                }`}
              >
                <Coins className="w-3.5 h-3.5" />
                <span>Reward Engine &amp; 23505 Bug</span>
              </button>

              <button
                onClick={() => setActiveAuditTab('wallet-security')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 whitespace-nowrap ${
                  activeAuditTab === 'wallet-security'
                    ? 'bg-emerald-500 text-slate-950'
                    : 'bg-slate-900 text-slate-400 hover:text-white'
                }`}
              >
                <ShieldCheck className="w-3.5 h-3.5" />
                <span>Wallet Security &amp; Concurrency</span>
              </button>

              <button
                onClick={() => setActiveAuditTab('economics')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 whitespace-nowrap ${
                  activeAuditTab === 'economics'
                    ? 'bg-emerald-500 text-slate-950'
                    : 'bg-slate-900 text-slate-400 hover:text-white'
                }`}
              >
                <Sparkles className="w-3.5 h-3.5" />
                <span>Pakistan eCPM Economics</span>
              </button>

              <button
                onClick={() => setActiveAuditTab('code-patches')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 whitespace-nowrap ${
                  activeAuditTab === 'code-patches'
                    ? 'bg-emerald-500 text-slate-950'
                    : 'bg-slate-900 text-slate-400 hover:text-white'
                }`}
              >
                <Code2 className="w-3.5 h-3.5" />
                <span>Ready Code Patches</span>
              </button>
            </div>

            {activeAuditTab === 'unity-ads' && <UnityAdsSection />}
            {activeAuditTab === 'reward-calc' && <RewardCalculationSection />}
            {activeAuditTab === 'wallet-security' && <WalletSecuritySection />}
            {activeAuditTab === 'economics' && <EconomicsSection />}
            {activeAuditTab === 'code-patches' && <CodePatchesSection />}
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="mt-12 border-t border-slate-900 bg-slate-950/80 py-6 text-center text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4">
          <p>
            FitPaisa • Official Walk-to-Earn Ecosystem • Built for Android (Kotlin) &amp; Supabase backend
          </p>
        </div>
      </footer>
    </div>
  );
};

export default function App() {
  return (
    <FitPaisaProvider>
      <FitPaisaAppContent />
    </FitPaisaProvider>
  );
}
