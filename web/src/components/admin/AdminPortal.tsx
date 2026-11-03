import React, { useState } from 'react';
import {
  ShieldAlert,
  ShieldCheck,
  Check,
  X,
  CreditCard,
  Truck,
  TrendingDown,
  TrendingUp,
  AlertTriangle,
  RotateCcw,
  ExternalLink,
  Users,
} from 'lucide-react';
import { useFitPaisa } from '../../context/FitPaisaContext';

export const AdminPortal: React.FC = () => {
  const {
    cashouts,
    redemptions,
    adminApproveCashout,
    adminRejectCashout,
    adminFulfillRedemption,
    resetToDemo,
  } = useFitPaisa();

  const [activeAdminTab, setActiveAdminTab] = useState<'cashouts' | 'orders' | 'fraud'>('cashouts');
  const [trxInputMap, setTrxInputMap] = useState<Record<string, string>>({});
  const [trackingInputMap, setTrackingInputMap] = useState<Record<string, string>>({});

  const pendingCashouts = cashouts.filter((c) => c.status === 'pending');
  const decidedCashouts = cashouts.filter((c) => c.status !== 'pending');

  const pendingOrders = redemptions.filter((r) => r.status === 'pending');
  const shippedOrders = redemptions.filter((r) => r.status !== 'pending');

  // Detect Sybil account clusters
  const accountClusters: Record<string, typeof cashouts> = {};
  cashouts.forEach((c) => {
    const acc = c.accountNumber;
    if (!accountClusters[acc]) accountClusters[acc] = [];
    accountClusters[acc].push(c);
  });

  const sybilClusters = Object.entries(accountClusters).filter(([_, list]) => {
    const uniqueUsers = new Set(list.map((item) => item.userId));
    return uniqueUsers.size > 1;
  });

  const handleApprove = (id: string) => {
    const trx = trxInputMap[id]?.trim() || `EP-${Math.floor(100000000 + Math.random() * 900000000)}`;
    adminApproveCashout(id, trx);
  };

  const handleFulfillOrder = (id: string) => {
    const tracking = trackingInputMap[id]?.trim() || `TCS-${Math.floor(1000000000 + Math.random() * 9000000000)}`;
    adminFulfillRedemption(id, tracking);
  };

  return (
    <div className="space-y-8">
      {/* Founder Admin Header */}
      <div className="p-6 sm:p-8 bg-gradient-to-br from-slate-900 to-indigo-950/40 border border-slate-800 rounded-3xl flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 text-xs font-semibold mb-2">
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Founder &amp; Cashout Operations Desk</span>
          </div>
          <h2 className="text-xl sm:text-2xl font-black text-white">Manual Payouts &amp; Order Fulfillment</h2>
          <p className="text-xs text-slate-400 mt-1 max-w-xl">
            "Money moves first from your Easypaisa/JazzCash app, the paid button second." Review requests, detect fraud, and confirm settlements.
          </p>
        </div>

        <button
          onClick={resetToDemo}
          className="px-3.5 py-2 bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-300 text-xs font-medium rounded-xl flex items-center gap-1.5 cursor-pointer shrink-0"
        >
          <RotateCcw className="w-3.5 h-3.5" />
          <span>Reset Sample Orders &amp; Cashouts</span>
        </button>
      </div>

      {/* Admin Tab Pills */}
      <div className="flex items-center gap-2 border-b border-slate-800 pb-3">
        <button
          onClick={() => setActiveAdminTab('cashouts')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold transition-all cursor-pointer flex items-center gap-2 ${
            activeAdminTab === 'cashouts'
              ? 'bg-emerald-500 text-slate-950 font-bold'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          <CreditCard className="w-4 h-4" />
          <span>Cashout Requests ({pendingCashouts.length})</span>
        </button>

        <button
          onClick={() => setActiveAdminTab('orders')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold transition-all cursor-pointer flex items-center gap-2 ${
            activeAdminTab === 'orders'
              ? 'bg-emerald-500 text-slate-950 font-bold'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          <Truck className="w-4 h-4" />
          <span>Product Orders ({pendingOrders.length})</span>
        </button>

        <button
          onClick={() => setActiveAdminTab('fraud')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold transition-all cursor-pointer flex items-center gap-2 ${
            activeAdminTab === 'fraud'
              ? 'bg-red-500 text-white font-bold'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          <ShieldAlert className="w-4 h-4" />
          <span>Sybil &amp; Multi-Account Alerts ({sybilClusters.length})</span>
        </button>
      </div>

      {/* TAB 1: CASHOUT REQUESTS */}
      {activeAdminTab === 'cashouts' && (
        <div className="space-y-6">
          <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6">
            <h3 className="text-base font-bold text-white mb-4">Pending Requests for Founder Payout</h3>

            {pendingCashouts.length === 0 ? (
              <div className="p-8 text-center text-xs text-slate-500 bg-slate-950 rounded-2xl">
                No pending cashouts. All user withdrawal requests have been reviewed.
              </div>
            ) : (
              <div className="space-y-4">
                {pendingCashouts.map((req) => {
                  const isShared = sybilClusters.some(([acc]) => acc === req.accountNumber);
                  return (
                    <div
                      key={req.id}
                      className={`p-5 rounded-2xl border flex flex-col lg:flex-row lg:items-center justify-between gap-4 ${
                        isShared ? 'bg-red-500/5 border-red-500/30' : 'bg-slate-950 border-slate-800'
                      }`}
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-extrabold text-white">
                            Rs. {req.rupees.toFixed(2)} PKR
                          </span>
                          <span className="text-xs font-mono px-2 py-0.5 rounded bg-slate-900 text-slate-300 border border-slate-800 uppercase font-bold">
                            {req.method}
                          </span>
                          {isShared && (
                            <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-red-500/20 text-red-300 border border-red-500/30">
                              🚨 SHARED WALLET FRAUD ALERT
                            </span>
                          )}
                        </div>

                        <div className="text-xs text-slate-300">
                          Account Title: <strong className="text-white">{req.accountTitle}</strong> • Number:{' '}
                          <code className="text-emerald-400 font-mono font-bold">{req.accountNumber}</code>
                        </div>

                        <div className="text-[11px] text-slate-500 font-mono">
                          User ID: {req.userId} ({req.userEmail}) • {req.coins.toLocaleString()} Coins • {req.createdAt}
                        </div>
                      </div>

                      {/* Founder Settlement Actions */}
                      <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 shrink-0">
                        <input
                          type="text"
                          placeholder="Easypaisa/JazzCash TRX ID"
                          value={trxInputMap[req.id] || ''}
                          onChange={(e) =>
                            setTrxInputMap({ ...trxInputMap, [req.id]: e.target.value })
                          }
                          className="px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white font-mono focus:outline-none focus:border-emerald-500 min-w-[200px]"
                        />

                        <button
                          onClick={() => handleApprove(req.id)}
                          className="px-4 py-2 bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-xs rounded-xl shadow-md transition-all cursor-pointer flex items-center justify-center gap-1.5"
                        >
                          <Check className="w-3.5 h-3.5" />
                          <span>Mark Paid</span>
                        </button>

                        <button
                          onClick={() => adminRejectCashout(req.id)}
                          className="px-3.5 py-2 bg-red-500/15 hover:bg-red-500/25 border border-red-500/30 text-red-300 text-xs font-semibold rounded-xl transition-all cursor-pointer flex items-center justify-center gap-1.5"
                        >
                          <X className="w-3.5 h-3.5" />
                          <span>Reject &amp; Refund</span>
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Settled History */}
          <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6">
            <h3 className="text-base font-bold text-white mb-4">Past Settled Transactions</h3>
            <div className="space-y-3">
              {decidedCashouts.map((item) => (
                <div
                  key={item.id}
                  className="p-4 bg-slate-950 border border-slate-800 rounded-2xl flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs"
                >
                  <div>
                    <div className="flex items-center gap-2">
                      <strong className="text-white">Rs. {item.rupees} ({item.method.toUpperCase()})</strong>
                      <span
                        className={`text-[10px] font-mono px-2 py-0.5 rounded ${
                          item.status === 'paid'
                            ? 'bg-emerald-500/10 text-emerald-400'
                            : 'bg-red-500/10 text-red-400'
                        }`}
                      >
                        {item.status.toUpperCase()}
                      </span>
                    </div>
                    <p className="text-slate-400 mt-0.5 font-mono">
                      {item.accountTitle} ({item.accountNumber}) • TRX: {item.providerReference || 'N/A'}
                    </p>
                  </div>
                  <div className="text-right text-slate-500 font-mono">{item.decidedAt || item.createdAt}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: PRODUCT ORDERS */}
      {activeAdminTab === 'orders' && (
        <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6 space-y-4">
          <h3 className="text-base font-bold text-white mb-2">Pending Product Shipments</h3>
          {pendingOrders.length === 0 ? (
            <div className="p-8 text-center text-xs text-slate-500 bg-slate-950 rounded-2xl">
              No orders waiting for shipment.
            </div>
          ) : (
            pendingOrders.map((order) => (
              <div
                key={order.id}
                className="p-5 bg-slate-950 border border-slate-800 rounded-2xl flex flex-col lg:flex-row lg:items-center justify-between gap-4"
              >
                <div>
                  <h4 className="text-sm font-bold text-white">{order.productName}</h4>
                  <p className="text-xs text-slate-300 mt-1">
                    Ship to: {order.recipientName} ({order.recipientPhone}) • {order.recipientAddress}
                  </p>
                  <span className="text-[11px] text-amber-400 font-mono mt-1 block">
                    {order.coinsSpent.toLocaleString()} FitCoins Spent • {order.createdAt}
                  </span>
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  <input
                    type="text"
                    placeholder="TCS / Leopard Tracking #"
                    value={trackingInputMap[order.id] || ''}
                    onChange={(e) =>
                      setTrackingInputMap({ ...trackingInputMap, [order.id]: e.target.value })
                    }
                    className="px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white font-mono focus:outline-none focus:border-emerald-500 min-w-[200px]"
                  />
                  <button
                    onClick={() => handleFulfillOrder(order.id)}
                    className="px-4 py-2 bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-xs rounded-xl shadow-md transition-all cursor-pointer"
                  >
                    Mark Dispatched
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {/* TAB 3: FRAUD ALERTS */}
      {activeAdminTab === 'fraud' && (
        <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6 space-y-4">
          <div className="flex items-center gap-2 mb-2">
            <Users className="w-5 h-5 text-red-400" />
            <h3 className="text-base font-bold text-white">Sybil Cloned Account Clusters</h3>
          </div>
          <p className="text-xs text-slate-400">
            These payout numbers are requested by multiple distinct user accounts. Do NOT send money without manual verification!
          </p>

          {sybilClusters.length === 0 ? (
            <div className="p-8 text-center text-xs text-emerald-400 bg-slate-950 rounded-2xl">
              Zero Sybil clusters detected. All accounts have unique payment destinations.
            </div>
          ) : (
            sybilClusters.map(([accNumber, requests], idx) => (
              <div
                key={idx}
                className="p-5 bg-red-500/10 border border-red-500/30 rounded-2xl space-y-3"
              >
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-red-300 font-mono">
                    Flagged Number: {accNumber}
                  </span>
                  <span className="text-xs font-bold text-red-400">
                    {requests.length} Requests Linked
                  </span>
                </div>
                <div className="space-y-1 text-xs text-slate-300">
                  {requests.map((r, rIdx) => (
                    <div key={rIdx} className="font-mono text-[11px] bg-slate-950/60 p-2 rounded-lg">
                      User: {r.userEmail} ({r.accountTitle}) • Rs. {r.rupees} ({r.coins.toLocaleString()} Coins) • Status: {r.status}
                    </div>
                  ))}
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
};
