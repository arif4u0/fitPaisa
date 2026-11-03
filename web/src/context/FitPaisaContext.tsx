import React, { createContext, useContext, useState, useEffect } from 'react';

export interface ProductItem {
  id: string;
  name: string;
  category: 'Gear' | 'Audio' | 'Fitness' | 'Vouchers';
  description: string;
  coinPrice: number;
  marketPricePkr: number;
  stock: number;
  badge?: string;
  specs: string[];
}

export interface CashoutItem {
  id: string;
  userId: string;
  userEmail: string;
  coins: number;
  rupees: number;
  method: 'easypaisa' | 'jazzcash' | 'bank';
  accountTitle: string;
  accountNumber: string;
  status: 'pending' | 'paid' | 'rejected';
  providerReference?: string;
  createdAt: string;
  decidedAt?: string;
}

export interface RedemptionItem {
  id: string;
  productId: string;
  productName: string;
  coinsSpent: number;
  status: 'pending' | 'shipped' | 'delivered' | 'cancelled';
  recipientName: string;
  recipientPhone: string;
  recipientAddress: string;
  trackingNumber?: string;
  createdAt: string;
}

export interface UserProfile {
  id: string;
  name: string;
  email: string;
  level: number;
  levelName: string;
  lifetimeSteps: number;
  todaySteps: number;
  creditedTodaySteps: number;
  dailyStepCap: number;
  availableCoins: number;
  maturingCoins: number;
  streakDays: number;
  lastStreakDate: string;
  dailyAdsWatched: number;
  dailyAdLimit: number;
  dailyAdCoins: number;
  freeSpinAvailable: boolean;
  dailyChallengeTarget: number;
  dailyChallengeBonus: number;
  dailyChallengeClaimed: boolean;
}

interface FitPaisaContextType {
  user: UserProfile;
  products: ProductItem[];
  cashouts: CashoutItem[];
  redemptions: RedemptionItem[];
  isAdPlaying: boolean;
  adContext: { type: 'convert' | 'daily_ad' | 'spin'; payload?: unknown } | null;
  adWatchCountdown: number;
  // Actions
  addSteps: (steps: number) => void;
  triggerConvert: () => void;
  triggerDailyAd: () => void;
  triggerSpinAd: () => void;
  closeAdModal: (completed: boolean) => void;
  spinWheel: (isFree: boolean) => number;
  requestCashout: (coins: number, method: 'easypaisa' | 'jazzcash' | 'bank', accountTitle: string, accountNumber: string) => { success: boolean; message: string };
  redeemProduct: (productId: string, name: string, phone: string, address: string) => { success: boolean; message: string };
  // Admin actions
  adminApproveCashout: (id: string, trxId: string) => void;
  adminRejectCashout: (id: string) => void;
  adminFulfillRedemption: (id: string, trackingNumber: string) => void;
  resetToDemo: () => void;
}

const DEFAULT_PRODUCTS: ProductItem[] = [
  {
    id: 'prod_1',
    name: 'FitPaisa Smart Band 2',
    category: 'Fitness',
    description: 'Real-time pedometer, heart rate monitor, AMOLED display, and 14-day battery life.',
    coinPrice: 35000,
    marketPricePkr: 3500,
    stock: 12,
    badge: 'Popular',
    specs: ['AMOLED Screen', 'Water Resistant 50m', 'Step Sensor Sync', 'Sleep Tracking'],
  },
  {
    id: 'prod_2',
    name: 'Stainless Steel Insulated Sports Bottle',
    category: 'Gear',
    description: 'Double-walled vacuum insulated bottle (750ml) keeps water ice cold for 24 hours.',
    coinPrice: 12000,
    marketPricePkr: 1200,
    stock: 25,
    badge: 'Best Value',
    specs: ['750ml Capacity', 'BPA-Free Steel', 'Leak-Proof Cap', 'Cold for 24h'],
  },
  {
    id: 'prod_3',
    name: 'Pro Wireless Sports Earbuds',
    category: 'Audio',
    description: 'IPX7 sweatproof Bluetooth 5.3 earbuds with ergonomic earhooks and deep bass.',
    coinPrice: 48000,
    marketPricePkr: 4800,
    stock: 8,
    badge: 'Limited Stock',
    specs: ['Bluetooth 5.3', 'IPX7 Sweatproof', '32h Total Playtime', 'Built-in Mic'],
  },
  {
    id: 'prod_4',
    name: 'Athletic Lightweight Running Cap',
    category: 'Gear',
    description: 'Quick-dry breathable fabric with reflective night-running stripes and adjustable strap.',
    coinPrice: 8000,
    marketPricePkr: 800,
    stock: 40,
    specs: ['UV Sun Protection', 'Laser Perforated', 'Reflective Trim', 'Featherlight 45g'],
  },
  {
    id: 'prod_5',
    name: 'Rs. 500 TCS / Daraz Shopping Voucher',
    category: 'Vouchers',
    description: 'Digital gift code delivered to your registered email address within 24 hours.',
    coinPrice: 50000,
    marketPricePkr: 500,
    stock: 50,
    badge: 'Instant Code',
    specs: ['Valid for 6 Months', 'No Minimum Spend', 'Instant Email Delivery'],
  },
];

const INITIAL_USER: UserProfile = {
  id: 'usr_me_001',
  name: 'Arif',
  email: 'arif@fitpaisa.com',
  level: 2,
  levelName: 'Walker',
  lifetimeSteps: 64200,
  todaySteps: 6850,
  creditedTodaySteps: 2000,
  dailyStepCap: 12000,
  availableCoins: 14500,
  maturingCoins: 4850,
  streakDays: 4,
  lastStreakDate: '2026-10-24',
  dailyAdsWatched: 2,
  dailyAdLimit: 6,
  dailyAdCoins: 11,
  freeSpinAvailable: true,
  dailyChallengeTarget: 8000,
  dailyChallengeBonus: 50,
  dailyChallengeClaimed: false,
};

const INITIAL_CASHOUTS: CashoutItem[] = [
  {
    id: 'cash_001',
    userId: 'usr_me_001',
    userEmail: 'arif@fitpaisa.com',
    coins: 10000,
    rupees: 100,
    method: 'easypaisa',
    accountTitle: 'Arif H.',
    accountNumber: '03451234567',
    status: 'paid',
    providerReference: 'EP-984219412',
    createdAt: '2026-10-21 14:30',
    decidedAt: '2026-10-21 16:15',
  },
  {
    id: 'cash_002',
    userId: 'usr_fraud_002',
    userEmail: 'bot_runner_1@gmail.com',
    coins: 20000,
    rupees: 200,
    method: 'easypaisa',
    accountTitle: 'Zubair',
    accountNumber: '03009998888', // Duplicate!
    status: 'pending',
    createdAt: '2026-10-25 08:20',
  },
  {
    id: 'cash_003',
    userId: 'usr_fraud_003',
    userEmail: 'bot_runner_2@gmail.com',
    coins: 15000,
    rupees: 150,
    method: 'easypaisa',
    accountTitle: 'Zubair K.',
    accountNumber: '03009998888', // Duplicate!
    status: 'pending',
    createdAt: '2026-10-25 09:12',
  },
];

const INITIAL_REDEMPTIONS: RedemptionItem[] = [
  {
    id: 'red_001',
    productId: 'prod_2',
    productName: 'Stainless Steel Insulated Sports Bottle',
    coinsSpent: 12000,
    status: 'shipped',
    recipientName: 'Arif H.',
    recipientPhone: '03451234567',
    recipientAddress: 'House 42, Sector F-8/2, Islamabad',
    trackingNumber: 'TCS-8921849182',
    createdAt: '2026-10-22 11:20',
  },
];

const FitPaisaContext = createContext<FitPaisaContextType | undefined>(undefined);

export const FitPaisaProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserProfile>(() => {
    const saved = localStorage.getItem('fitpaisa_user_profile');
    return saved ? JSON.parse(saved) : INITIAL_USER;
  });

  const [products, setProducts] = useState<ProductItem[]>(DEFAULT_PRODUCTS);
  const [cashouts, setCashouts] = useState<CashoutItem[]>(() => {
    const saved = localStorage.getItem('fitpaisa_cashouts');
    return saved ? JSON.parse(saved) : INITIAL_CASHOUTS;
  });
  const [redemptions, setRedemptions] = useState<RedemptionItem[]>(() => {
    const saved = localStorage.getItem('fitpaisa_redemptions');
    return saved ? JSON.parse(saved) : INITIAL_REDEMPTIONS;
  });

  const [isAdPlaying, setIsAdPlaying] = useState(false);
  const [adContext, setAdContext] = useState<{ type: 'convert' | 'daily_ad' | 'spin'; payload?: unknown } | null>(null);
  const [adWatchCountdown, setAdWatchCountdown] = useState(15);

  useEffect(() => {
    localStorage.setItem('fitpaisa_user_profile', JSON.stringify(user));
  }, [user]);

  useEffect(() => {
    localStorage.setItem('fitpaisa_cashouts', JSON.stringify(cashouts));
  }, [cashouts]);

  useEffect(() => {
    localStorage.setItem('fitpaisa_redemptions', JSON.stringify(redemptions));
  }, [redemptions]);

  // Step Counter Simulator
  const addSteps = (steps: number) => {
    setUser((prev) => {
      const nextSteps = prev.todaySteps + steps;
      return {
        ...prev,
        todaySteps: nextSteps,
      };
    });
  };

  // Convert Steps tap: opens the rewarded ad player
  const triggerConvert = () => {
    const eligibleSteps = Math.min(user.todaySteps, user.dailyStepCap);
    const uncredited = Math.max(0, eligibleSteps - user.creditedTodaySteps);
    if (uncredited < 100) return;

    setAdContext({ type: 'convert', payload: { uncredited } });
    setAdWatchCountdown(10);
    setIsAdPlaying(true);
  };

  // Daily Ad tap
  const triggerDailyAd = () => {
    if (user.dailyAdsWatched >= user.dailyAdLimit) return;
    setAdContext({ type: 'daily_ad' });
    setAdWatchCountdown(10);
    setIsAdPlaying(true);
  };

  // Spin Ad tap
  const triggerSpinAd = () => {
    setAdContext({ type: 'spin' });
    setAdWatchCountdown(10);
    setIsAdPlaying(true);
  };

  const closeAdModal = (completed: boolean) => {
    setIsAdPlaying(false);
    if (!completed || !adContext) {
      setAdContext(null);
      return;
    }

    if (adContext.type === 'convert') {
      const eligibleSteps = Math.min(user.todaySteps, user.dailyStepCap);
      const uncredited = Math.max(0, eligibleSteps - user.creditedTodaySteps);
      const coinsToAdd = Math.floor(uncredited / 100);

      setUser((prev) => {
        const nextLifetime = prev.lifetimeSteps + uncredited;
        // Check daily challenge
        const isChallengeMet = eligibleSteps >= prev.dailyChallengeTarget && !prev.dailyChallengeClaimed;
        const challengeBonus = isChallengeMet ? prev.dailyChallengeBonus : 0;

        return {
          ...prev,
          creditedTodaySteps: eligibleSteps,
          lifetimeSteps: nextLifetime,
          maturingCoins: prev.maturingCoins + coinsToAdd,
          availableCoins: prev.availableCoins + challengeBonus,
          dailyChallengeClaimed: prev.dailyChallengeClaimed || isChallengeMet,
          streakDays: prev.streakDays === 0 ? 1 : prev.streakDays,
        };
      });
    } else if (adContext.type === 'daily_ad') {
      setUser((prev) => ({
        ...prev,
        dailyAdsWatched: prev.dailyAdsWatched + 1,
        availableCoins: prev.availableCoins + prev.dailyAdCoins,
      }));
    }

    setAdContext(null);
  };

  // Lucky Spin
  const spinWheel = (isFree: boolean): number => {
    const prizes = [5, 10, 15, 25, 50, 100];
    const weights = [30, 25, 20, 12, 8, 5];
    const totalWeight = weights.reduce((a, b) => a + b, 0);
    let rand = Math.random() * totalWeight;
    let prize = 5;

    for (let i = 0; i < prizes.length; i++) {
      rand -= weights[i];
      if (rand <= 0) {
        prize = prizes[i];
        break;
      }
    }

    setUser((prev) => ({
      ...prev,
      availableCoins: prev.availableCoins + prize,
      freeSpinAvailable: isFree ? false : prev.freeSpinAvailable,
    }));

    return prize;
  };

  // Request Cashout
  const requestCashout = (
    coins: number,
    method: 'easypaisa' | 'jazzcash' | 'bank',
    accountTitle: string,
    accountNumber: string
  ): { success: boolean; message: string } => {
    if (coins < 10000) {
      return { success: false, message: 'Minimum cashout is 10,000 FitCoins (Rs. 100).' };
    }
    if (user.availableCoins < coins) {
      return { success: false, message: 'Insufficient spendable coin balance.' };
    }

    // Single pending cashout rule check
    const existingPending = cashouts.some(
      (c) => c.userId === user.id && c.status === 'pending'
    );
    if (existingPending) {
      return {
        success: false,
        message: 'You already have a pending cashout in review. Please wait for the founder to complete it.',
      };
    }

    const cleanAcc = accountNumber.replace(/[^0-9]/g, '');
    if ((method === 'easypaisa' || method === 'jazzcash') && cleanAcc.length !== 11) {
      return {
        success: false,
        message: 'Pakistani wallet accounts (Easypaisa/JazzCash) must be exactly 11 digits (e.g. 03001234567).',
      };
    }

    // Deduct available balance
    setUser((prev) => ({
      ...prev,
      availableCoins: prev.availableCoins - coins,
    }));

    const newCashout: CashoutItem = {
      id: `cash_${Date.now()}`,
      userId: user.id,
      userEmail: user.email,
      coins,
      rupees: coins / 100,
      method,
      accountTitle,
      accountNumber: cleanAcc,
      status: 'pending',
      createdAt: new Date().toISOString().replace('T', ' ').slice(0, 16),
    };

    setCashouts((prev) => [newCashout, ...prev]);

    return {
      success: true,
      message: `Request created! Rs. ${coins / 100} will be sent from founder's wallet to your ${method.toUpperCase()} account after review.`,
    };
  };

  // Redeem Product
  const redeemProduct = (
    productId: string,
    name: string,
    phone: string,
    address: string
  ): { success: boolean; message: string } => {
    const prod = products.find((p) => p.id === productId);
    if (!prod) return { success: false, message: 'Product not found.' };
    if (prod.stock <= 0) return { success: false, message: 'Sorry, this product is out of stock.' };
    if (user.availableCoins < prod.coinPrice) {
      return { success: false, message: `Insufficient coins. You need ${prod.coinPrice.toLocaleString()} FitCoins.` };
    }

    // Deduct coins & decrement stock
    setUser((prev) => ({
      ...prev,
      availableCoins: prev.availableCoins - prod.coinPrice,
    }));

    setProducts((prev) =>
      prev.map((p) => (p.id === productId ? { ...p, stock: p.stock - 1 } : p))
    );

    const newRedemption: RedemptionItem = {
      id: `red_${Date.now()}`,
      productId,
      productName: prod.name,
      coinsSpent: prod.coinPrice,
      status: 'pending',
      recipientName: name,
      recipientPhone: phone,
      recipientAddress: address,
      createdAt: new Date().toISOString().replace('T', ' ').slice(0, 16),
    };

    setRedemptions((prev) => [newRedemption, ...prev]);

    return {
      success: true,
      message: `Order confirmed! We will ship your ${prod.name} to ${address} shortly.`,
    };
  };

  // Admin Actions
  const adminApproveCashout = (id: string, trxId: string) => {
    setCashouts((prev) =>
      prev.map((c) =>
        c.id === id
          ? {
              ...c,
              status: 'paid',
              providerReference: trxId,
              decidedAt: new Date().toISOString().replace('T', ' ').slice(0, 16),
            }
          : c
      )
    );
  };

  const adminRejectCashout = (id: string) => {
    const item = cashouts.find((c) => c.id === id);
    if (!item) return;

    // Refund coins to user
    if (item.userId === user.id) {
      setUser((prev) => ({
        ...prev,
        availableCoins: prev.availableCoins + item.coins,
      }));
    }

    setCashouts((prev) =>
      prev.map((c) =>
        c.id === id
          ? {
              ...c,
              status: 'rejected',
              decidedAt: new Date().toISOString().replace('T', ' ').slice(0, 16),
            }
          : c
      )
    );
  };

  const adminFulfillRedemption = (id: string, trackingNumber: string) => {
    setRedemptions((prev) =>
      prev.map((r) =>
        r.id === id ? { ...r, status: 'shipped', trackingNumber } : r
      )
    );
  };

  const resetToDemo = () => {
    setUser(INITIAL_USER);
    setProducts(DEFAULT_PRODUCTS);
    setCashouts(INITIAL_CASHOUTS);
    setRedemptions(INITIAL_REDEMPTIONS);
    localStorage.clear();
  };

  return (
    <FitPaisaContext.Provider
      value={{
        user,
        products,
        cashouts,
        redemptions,
        isAdPlaying,
        adContext,
        adWatchCountdown,
        addSteps,
        triggerConvert,
        triggerDailyAd,
        triggerSpinAd,
        closeAdModal,
        spinWheel,
        requestCashout,
        redeemProduct,
        adminApproveCashout,
        adminRejectCashout,
        adminFulfillRedemption,
        resetToDemo,
      }}
    >
      {children}
    </FitPaisaContext.Provider>
  );
};

export const useFitPaisa = () => {
  const context = useContext(FitPaisaContext);
  if (!context) {
    throw new Error('useFitPaisa must be used within a FitPaisaProvider');
  }
  return context;
};
