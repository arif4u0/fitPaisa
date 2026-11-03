import React, { useState } from 'react';
import { X, PackageCheck, AlertCircle, CheckCircle2, MapPin } from 'lucide-react';
import { useFitPaisa, ProductItem } from '../../context/FitPaisaContext';

interface RedeemModalProps {
  product: ProductItem;
  onClose: () => void;
}

export const RedeemModal: React.FC<RedeemModalProps> = ({ product, onClose }) => {
  const { user, redeemProduct } = useFitPaisa();
  const [name, setName] = useState('Arif Hassan');
  const [phone, setPhone] = useState('03451234567');
  const [address, setAddress] = useState('House 12, Street 4, Sector G-9/1, Islamabad');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setSuccessMsg(null);

    if (!name.trim() || !phone.trim() || !address.trim()) {
      setErrorMsg('Please fill in all shipping details.');
      return;
    }

    const res = redeemProduct(product.id, name, phone, address);
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
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/85 backdrop-blur-sm">
      <div className="relative w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl sm:rounded-3xl p-4 sm:p-6 shadow-2xl flex flex-col max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <div>
            <h3 className="text-base font-bold text-white">Order Physical Reward</h3>
            <p className="text-xs text-slate-400 mt-0.5">Delivered via TCS Courier anywhere in Pakistan</p>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-full text-slate-400 hover:text-white bg-slate-800/80 cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Selected Product Summary */}
        <div className="my-4 p-4 bg-slate-950 border border-slate-800 rounded-2xl flex items-center justify-between">
          <div>
            <span className="text-xs font-bold text-white block">{product.name}</span>
            <span className="text-[11px] text-slate-400">Standard Delivery • 3-5 business days</span>
          </div>
          <div className="text-right">
            <span className="text-sm font-extrabold text-amber-400 block">
              {product.coinPrice.toLocaleString()} Coins
            </span>
            <span className="text-[10px] text-slate-500">Worth Rs. {product.marketPricePkr}</span>
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
          <div>
            <label className="text-xs font-medium text-slate-300 block mb-1">
              Recipient Full Name
            </label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-sm text-white focus:outline-none focus:border-emerald-500"
            />
          </div>

          <div>
            <label className="text-xs font-medium text-slate-300 block mb-1">
              Contact Phone (For Courier Call)
            </label>
            <input
              type="tel"
              required
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-sm text-white font-mono focus:outline-none focus:border-emerald-500"
            />
          </div>

          <div>
            <label className="text-xs font-medium text-slate-300 block mb-1 flex items-center gap-1">
              <MapPin className="w-3.5 h-3.5 text-slate-400" />
              <span>Full Shipping Address</span>
            </label>
            <textarea
              required
              rows={2}
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-sm text-white focus:outline-none focus:border-emerald-500 resize-none"
            />
          </div>

          <div className="pt-2">
            <button
              type="submit"
              disabled={user.availableCoins < product.coinPrice || !!successMsg}
              className="w-full py-3 bg-gradient-to-r from-emerald-500 to-teal-400 hover:from-emerald-400 hover:to-teal-300 text-slate-950 font-bold text-sm rounded-xl shadow-lg shadow-emerald-500/25 transition-all cursor-pointer disabled:opacity-50 flex items-center justify-center gap-2"
            >
              <PackageCheck className="w-4 h-4" />
              <span>Confirm Order ({product.coinPrice.toLocaleString()} Coins)</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
