import React, { useState } from 'react';
import { X, ShieldCheck, AlertCircle, CheckCircle2, ArrowRight } from 'lucide-react';
import { useFitPaisa } from '../../context/FitPaisaContext';

interface CashoutModalProps {
  onClose: () => void;
}

export const CashoutModal: React.FC<CashoutModalProps> = ({ onClose }) => {
  const { user, requestCashout } = useFitPaisa();
  const [method, setMethod] = useState<'easypaisa' | 'jazzcash' | 'bank'>('easypaisa');
  const [accountTitle, setAccountTitle] = useState('');
  const [accountNumber, setAccountNumber] = useState('');
  const [coins, setCoins] = useState<number>(10000);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const rupees = coins / 100;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setSuccessMsg(null);

    if (!accountTitle.trim()) {
      setErrorMsg('Please enter your full account title.');
      return;
    }

    const cleanNum = accountNumber.replace(/[^0-9]/g, '');
    if ((method === 'easypaisa' || method === 'jazzcash') && cleanNum.length !== 11) {
      setErrorMsg('Pakistani mobile wallet accounts must be 11 digits (e.g. 03451234567).');
      return;
    }

    const res = requestCashout(coins, method, accountTitle, cleanNum);
    if (res.success) {
      setSuccessMsg(res.message);
      setTimeout(() => {
        onClose();
      }, 2500);
    } else {
      setErrorMsg(res.message);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
      <div className="relative w-full max-w-md bg-slate-900 border border-slate-800 rounded-3xl p-6 shadow-2xl flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <div>
            <h3 className="text-base font-bold text-white">Cash Out FitCoins</h3>
            <p className="text-xs text-slate-400 mt-0.5">Real money transfer from founder's wallet</p>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-full text-slate-400 hover:text-white bg-slate-800/80 cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Balance Snapshot */}
        <div className="my-4 p-3.5 bg-slate-950 border border-slate-800 rounded-2xl flex items-center justify-between">
          <div>
            <span className="text-[11px] text-slate-400 block">Spendable Balance</span>
            <span className="text-base font-extrabold text-white">
              {user.availableCoins.toLocaleString()} <span className="text-emerald-400 text-xs">FitCoins</span>
            </span>
          </div>
          <div className="text-right">
            <span className="text-[11px] text-slate-400 block">Rupee Value</span>
            <span className="text-base font-extrabold text-emerald-400">
              Rs. {(user.availableCoins / 100).toFixed(2)}
            </span>
          </div>
        </div>

        {errorMsg && (
          <div className="mb-4 p-3 bg-red-500/10 border border-red-500/30 text-red-200 text-xs rounded-xl flex items-start gap-2">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{errorMsg}</span>
          </div>
        )}

        {successMsg && (
          <div className="mb-4 p-3 bg-emerald-500/10 border border-emerald-500/30 text-emerald-200 text-xs rounded-xl flex items-start gap-2">
            <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{successMsg}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Method Selection */}
          <div>
            <label className="text-xs font-medium text-slate-300 block mb-1.5">Payout Method</label>
            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                onClick={() => setMethod('easypaisa')}
                className={`py-2 px-3 rounded-xl text-xs font-semibold border transition-all cursor-pointer ${
                  method === 'easypaisa'
                    ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/50'
                    : 'bg-slate-950 text-slate-400 border-slate-800 hover:bg-slate-800'
                }`}
              >
                Easypaisa
              </button>
              <button
                type="button"
                onClick={() => setMethod('jazzcash')}
                className={`py-2 px-3 rounded-xl text-xs font-semibold border transition-all cursor-pointer ${
                  method === 'jazzcash'
                    ? 'bg-red-500/20 text-red-300 border-red-500/50'
                    : 'bg-slate-950 text-slate-400 border-slate-800 hover:bg-slate-800'
                }`}
              >
                JazzCash
              </button>
              <button
                type="button"
                onClick={() => setMethod('bank')}
                className={`py-2 px-3 rounded-xl text-xs font-semibold border transition-all cursor-pointer ${
                  method === 'bank'
                    ? 'bg-indigo-500/20 text-indigo-300 border-indigo-500/50'
                    : 'bg-slate-950 text-slate-400 border-slate-800 hover:bg-slate-800'
                }`}
              >
                Bank Transfer
              </button>
            </div>
          </div>

          {/* Account Title */}
          <div>
            <label className="text-xs font-medium text-slate-300 block mb-1">
              Account Holder Name
            </label>
            <input
              type="text"
              required
              value={accountTitle}
              onChange={(e) => setAccountTitle(e.target.value)}
              placeholder="e.g. Arif Hassan"
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-sm text-white focus:outline-none focus:border-emerald-500"
            />
          </div>

          {/* Account Number */}
          <div>
            <label className="text-xs font-medium text-slate-300 block mb-1">
              {method === 'bank' ? 'Bank Account / IBAN' : `${method.toUpperCase()} Mobile Number`}
            </label>
            <input
              type="text"
              required
              value={accountNumber}
              onChange={(e) => setAccountNumber(e.target.value)}
              placeholder={method === 'bank' ? 'PK00MEZN0000000000000000' : '03451234567'}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-sm text-white font-mono focus:outline-none focus:border-emerald-500"
            />
            <span className="text-[10px] text-slate-500 mt-1 block">
              {method === 'bank' ? 'Any licensed Pakistani bank' : 'Must be 11 digits starting with 03'}
            </span>
          </div>

          {/* Coins to Cash Out */}
          <div>
            <div className="flex justify-between items-center mb-1">
              <label className="text-xs font-medium text-slate-300">Coins to Redeem</label>
              <span className="text-xs font-bold text-emerald-400">= Rs. {rupees.toFixed(2)} PKR</span>
            </div>
            <input
              type="number"
              min={10000}
              step={1000}
              max={user.availableCoins}
              value={coins}
              onChange={(e) => setCoins(Number(e.target.value))}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-sm text-white font-mono focus:outline-none focus:border-emerald-500"
            />
            <span className="text-[10px] text-slate-500 mt-1 block">
              Minimum cashout is 10,000 FitCoins (Rs. 100).
            </span>
          </div>

          <div className="pt-2">
            <button
              type="submit"
              disabled={user.availableCoins < 10000 || !!successMsg}
              className="w-full py-3 bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-sm rounded-xl shadow-lg shadow-emerald-500/25 transition-all cursor-pointer disabled:opacity-50 flex items-center justify-center gap-2"
            >
              <span>Submit Cashout Request</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </form>

        <div className="mt-4 pt-3 border-t border-slate-800/80 flex items-center gap-2 text-[11px] text-slate-500">
          <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0" />
          <span>
            Coins are reserved atomically. Founder verifies transactions twice weekly before sending money.
          </span>
        </div>
      </div>
    </div>
  );
};
