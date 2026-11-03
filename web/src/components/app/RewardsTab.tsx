import React, { useState } from 'react';
import {
  ShoppingBag,
  Coins,
  PackageCheck,
  CheckCircle2,
  Clock,
  Sparkles,
  Truck,
  ExternalLink,
} from 'lucide-react';
import { useFitPaisa, ProductItem } from '../../context/FitPaisaContext';
import { RedeemModal } from './RedeemModal';

export const RewardsTab: React.FC = () => {
  const { user, products, redemptions } = useFitPaisa();
  const [selectedCategory, setSelectedCategory] = useState<string>('All');
  const [selectedProduct, setSelectedProduct] = useState<ProductItem | null>(null);

  const categories = ['All', 'Fitness', 'Gear', 'Audio', 'Vouchers'];

  const filteredProducts =
    selectedCategory === 'All'
      ? products
      : products.filter((p) => p.category === selectedCategory);

  return (
    <div className="space-y-8">
      {/* Header Banner */}
      <div className="p-6 bg-gradient-to-br from-slate-900 to-slate-950 border border-slate-800 rounded-3xl flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-semibold mb-2">
            <ShoppingBag className="w-3.5 h-3.5" />
            <span>FitPaisa Reward Store</span>
          </div>
          <h2 className="text-xl sm:text-2xl font-black text-white">Redeem Products with FitCoins</h2>
          <p className="text-xs text-slate-400 mt-1 max-w-xl">
            Order authentic fitness trackers, water bottles, and accessories. Delivered nationwide in Pakistan via TCS Courier with zero delivery charges.
          </p>
        </div>

        <div className="p-4 bg-slate-950/80 border border-slate-800 rounded-2xl flex items-center gap-4 shrink-0">
          <div>
            <span className="text-[10px] text-slate-400 block uppercase font-mono tracking-wider">
              Your Coins
            </span>
            <span className="text-xl font-black text-amber-400 tabular-nums">
              {user.availableCoins.toLocaleString()}
            </span>
          </div>
          <div className="w-px h-8 bg-slate-800"></div>
          <div>
            <span className="text-[10px] text-slate-400 block uppercase font-mono tracking-wider">
              Cash Value
            </span>
            <span className="text-base font-bold text-white tabular-nums">
              Rs. {(user.availableCoins / 100).toFixed(0)}
            </span>
          </div>
        </div>
      </div>

      {/* Category Filter Pills */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1">
        {categories.map((cat) => (
          <button
            key={cat}
            onClick={() => setSelectedCategory(cat)}
            className={`px-4 py-2 rounded-xl text-xs font-semibold transition-all whitespace-nowrap cursor-pointer ${
              selectedCategory === cat
                ? 'bg-emerald-500 text-slate-950 font-bold shadow-md shadow-emerald-500/20'
                : 'bg-slate-900 border border-slate-800 text-slate-400 hover:text-white hover:bg-slate-800'
            }`}
          >
            {cat}
          </button>
        ))}
      </div>

      {/* Product Catalog Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filteredProducts.map((product) => {
          const canAfford = user.availableCoins >= product.coinPrice;
          return (
            <div
              key={product.id}
              className="bg-slate-900/90 border border-slate-800 rounded-3xl overflow-hidden shadow-lg flex flex-col justify-between hover:border-slate-700 transition-all"
            >
              {/* Product Visual Area */}
              <div className="relative h-44 bg-gradient-to-br from-slate-950 via-slate-900 to-emerald-950/40 p-6 flex flex-col justify-between">
                <div className="flex items-center justify-between">
                  <span className="text-[10px] uppercase font-mono px-2 py-0.5 rounded bg-slate-900 text-slate-300 border border-slate-800">
                    {product.category}
                  </span>
                  {product.badge && (
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-emerald-500 text-slate-950">
                      {product.badge}
                    </span>
                  )}
                </div>

                <div className="flex items-center justify-center py-2">
                  <div className="w-16 h-16 rounded-2xl bg-slate-800/80 border border-slate-700/60 flex items-center justify-center text-emerald-400 shadow-xl">
                    <ShoppingBag className="w-8 h-8" />
                  </div>
                </div>

                <div className="flex items-center justify-between text-xs">
                  <span className="text-slate-400 text-[11px]">
                    Stock: <strong className="text-white">{product.stock} units</strong>
                  </span>
                  <span className="text-slate-400 text-[11px]">
                    M.R.P: <strong className="text-slate-200">Rs. {product.marketPricePkr}</strong>
                  </span>
                </div>
              </div>

              {/* Product Content */}
              <div className="p-5 flex-1 flex flex-col justify-between space-y-4">
                <div>
                  <h4 className="text-base font-bold text-white line-clamp-1">{product.name}</h4>
                  <p className="text-xs text-slate-400 mt-1 line-clamp-2 leading-relaxed">
                    {product.description}
                  </p>

                  {/* Specs */}
                  <div className="flex flex-wrap gap-1.5 mt-3">
                    {product.specs.map((spec, sIdx) => (
                      <span
                        key={sIdx}
                        className="text-[10px] px-2 py-0.5 rounded-lg bg-slate-950 border border-slate-800/80 text-slate-300"
                      >
                        {spec}
                      </span>
                    ))}
                  </div>
                </div>

                {/* Price & Redeem Button */}
                <div className="pt-3 border-t border-slate-800 flex items-center justify-between gap-3">
                  <div>
                    <span className="text-[10px] text-slate-500 block uppercase font-mono">
                      Price in FitCoins
                    </span>
                    <span className="text-lg font-black text-amber-400 tabular-nums">
                      {product.coinPrice.toLocaleString()} <span className="text-xs text-amber-300 font-normal">Coins</span>
                    </span>
                  </div>

                  <button
                    disabled={!canAfford || product.stock <= 0}
                    onClick={() => setSelectedProduct(product)}
                    className="px-4 py-2 bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-xs rounded-xl shadow-md shadow-emerald-500/20 transition-all cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
                  >
                    {!canAfford ? 'Need More Coins' : 'Redeem Now'}
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Orders History Section */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6">
        <div className="flex items-center gap-2 mb-4">
          <Truck className="w-5 h-5 text-emerald-400" />
          <h3 className="text-base font-bold text-white">Your Orders &amp; Deliveries</h3>
        </div>

        {redemptions.length === 0 ? (
          <div className="p-8 rounded-2xl bg-slate-950 border border-slate-800 text-center text-xs text-slate-400">
            You haven't ordered any physical products yet. Keep walking to earn coins!
          </div>
        ) : (
          <div className="space-y-3">
            {redemptions.map((order) => (
              <div
                key={order.id}
                className="p-4 bg-slate-950 border border-slate-800 rounded-2xl flex flex-col sm:flex-row sm:items-center justify-between gap-3"
              >
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-white text-sm">{order.productName}</span>
                    <span
                      className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded ${
                        order.status === 'delivered'
                          ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                          : order.status === 'shipped'
                          ? 'bg-indigo-500/10 text-indigo-400 border border-indigo-500/20'
                          : 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                      }`}
                    >
                      {order.status.toUpperCase()}
                    </span>
                  </div>
                  <p className="text-xs text-slate-400 mt-1">
                    Ship to: {order.recipientName} ({order.recipientPhone}) • {order.recipientAddress}
                  </p>
                  {order.trackingNumber && (
                    <div className="mt-1 text-xs text-indigo-300 font-mono flex items-center gap-1.5">
                      <Truck className="w-3.5 h-3.5" />
                      <span>Tracking: {order.trackingNumber}</span>
                    </div>
                  )}
                </div>

                <div className="text-right shrink-0">
                  <span className="text-xs text-slate-400 block">{order.createdAt}</span>
                  <span className="text-sm font-bold text-amber-400">
                    -{order.coinsSpent.toLocaleString()} Coins
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Redeem Modal */}
      {selectedProduct && (
        <RedeemModal product={selectedProduct} onClose={() => setSelectedProduct(null)} />
      )}
    </div>
  );
};
