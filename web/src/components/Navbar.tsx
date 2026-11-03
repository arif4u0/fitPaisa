import React from 'react';
import { ShieldCheck, AlertTriangle, Bug, DollarSign, Tv, Code2 } from 'lucide-react';

export type ActiveTab = 'overview' | 'unity-ads' | 'reward-calc' | 'wallet-security' | 'economics' | 'code-patches';

interface NavbarProps {
  activeTab: ActiveTab;
  setActiveTab: (tab: ActiveTab) => void;
}

export const Navbar: React.FC<NavbarProps> = ({ activeTab, setActiveTab }) => {
  const tabs = [
    { id: 'overview', label: 'Executive Audit', icon: ShieldCheck, badge: 'Overview' },
    { id: 'unity-ads', label: 'Unity Ads', icon: Tv, badge: '5 Bugs' },
    { id: 'reward-calc', label: 'Reward Engine', icon: Bug, badge: '23505 Bug' },
    { id: 'wallet-security', label: 'Wallet Vault', icon: ShieldCheck, badge: 'Anti-Fraud' },
    { id: 'economics', label: 'Economics', icon: DollarSign, badge: 'P&L Model' },
    { id: 'code-patches', label: 'Code Patches', icon: Code2, badge: '5 Files' },
  ] as const;

  return (
    <header className="sticky top-0 z-50 bg-slate-900/90 backdrop-blur-md border-b border-slate-800">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center gap-3 shrink-0">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-emerald-500 to-teal-400 flex items-center justify-center shadow-lg shadow-emerald-500/20 text-slate-950 font-bold text-lg">
              FP
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-extrabold text-white text-base tracking-tight">FitPaisa</span>
                <span className="text-[10px] px-1.5 py-0.5 rounded font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                  Audit v1.1.5
                </span>
              </div>
              <p className="text-[11px] text-slate-400 hidden sm:block">
                Repo: <code className="text-emerald-300">arif4u0/fitPaisa</code>
              </p>
            </div>
          </div>

          <nav className="flex items-center gap-1 sm:gap-1.5 overflow-x-auto py-1">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              const isActive = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id as ActiveTab)}
                  className={`flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs font-medium transition-all whitespace-nowrap cursor-pointer ${
                    isActive
                      ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 shadow-sm shadow-emerald-500/10'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                  }`}
                >
                  <Icon className="w-3.5 h-3.5 shrink-0" />
                  <span>{tab.label}</span>
                  {tab.badge && (
                    <span
                      className={`text-[9px] px-1.5 py-0.2 rounded font-mono ${
                        isActive
                          ? 'bg-emerald-400 text-slate-950 font-bold'
                          : 'bg-slate-800 text-slate-400'
                      }`}
                    >
                      {tab.badge}
                    </span>
                  )}
                </button>
              );
            })}
          </nav>
        </div>
      </div>
    </header>
  );
};
