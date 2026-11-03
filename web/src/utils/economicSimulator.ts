/**
 * Economic Sustainability & Profit/Loss Simulation Model
 * Compares Ad Revenue (Unity/AdMob in Pakistan) with Outbound Real Money Payouts (Easypaisa/JazzCash).
 */

export interface EconomicModelInputs {
  /** Ad network eCPM in USD (typical for Pakistan rewarded video is $0.80 - $2.50) */
  ecpmUsd: number;
  /** PKR per 1 USD exchange rate (e.g. 280 PKR/USD) */
  usdToPkrRate: number;
  /** Steps required for 1 FitCoin (default 100) */
  stepsPerCoin: number;
  /** Cash value per coin in Paisa (100 Paisa = 1 PKR) */
  coinValuePaisa: number;
  /** Daily step cap (e.g. 10,000 steps) */
  dailyStepCap: number;
  /** Number of rewarded ads required per conversion (currently 1 in fitPaisa) */
  adsWatchedPerConvert: number;
  /** Optional bonus coins awarded (daily challenge, spins, streak) */
  dailyBonusCoins: number;
  /** Number of active users per day */
  activeUsers: number;
}

export interface EconomicModelOutputs {
  /** Revenue generated per single ad view in PKR */
  adRevenuePerViewPkr: number;
  /** Total ad revenue generated per daily active user in PKR */
  adRevenuePerUserPkr: number;
  /** Maximum coins a user earns from steps in a day */
  stepCoinsPerDay: number;
  /** Total coins user earns per day (steps + bonuses) */
  totalCoinsPerDay: number;
  /** Real money liability incurred per user per day in PKR */
  payoutCostPerUserPkr: number;
  /** Net profit or loss per user per day in PKR */
  netProfitLossPerUserPkr: number;
  /** Total monthly profit or loss across all active users in PKR */
  monthlyNetProfitLossPkr: number;
  /** Minimum number of ads required per 10k steps to reach financial break-even */
  breakEvenAdsRequired: number;
  /** Status indicating whether the current configuration is solvent or losing money */
  solvencyStatus: 'HEALTHY_PROFIT' | 'BREAK_EVEN' | 'CRITICAL_DEFICIT';
}

export function calculateEconomics(inputs: EconomicModelInputs): EconomicModelOutputs {
  const {
    ecpmUsd,
    usdToPkrRate,
    stepsPerCoin,
    coinValuePaisa,
    dailyStepCap,
    adsWatchedPerConvert,
    dailyBonusCoins,
    activeUsers,
  } = inputs;

  // 1 eCPM = revenue for 1000 impressions in USD
  const revenuePerAdUsd = ecpmUsd / 1000;
  const adRevenuePerViewPkr = revenuePerAdUsd * usdToPkrRate;
  const adRevenuePerUserPkr = adRevenuePerViewPkr * adsWatchedPerConvert;

  const stepCoinsPerDay = Math.floor(dailyStepCap / stepsPerCoin);
  const totalCoinsPerDay = stepCoinsPerDay + dailyBonusCoins;

  // 100 paisa = 1 PKR
  const payoutCostPerUserPkr = (totalCoinsPerDay * coinValuePaisa) / 100;
  const netProfitLossPerUserPkr = adRevenuePerUserPkr - payoutCostPerUserPkr;
  const monthlyNetProfitLossPkr = netProfitLossPerUserPkr * activeUsers * 30;

  // Break-even ads: how many ads needed to pay for the coins
  const breakEvenAdsRequired = adRevenuePerViewPkr > 0
    ? Math.ceil(payoutCostPerUserPkr / adRevenuePerViewPkr)
    : Infinity;

  let solvencyStatus: 'HEALTHY_PROFIT' | 'BREAK_EVEN' | 'CRITICAL_DEFICIT';
  if (netProfitLossPerUserPkr > 0.05) {
    solvencyStatus = 'HEALTHY_PROFIT';
  } else if (netProfitLossPerUserPkr >= -0.05) {
    solvencyStatus = 'BREAK_EVEN';
  } else {
    solvencyStatus = 'CRITICAL_DEFICIT';
  }

  return {
    adRevenuePerViewPkr,
    adRevenuePerUserPkr,
    stepCoinsPerDay,
    totalCoinsPerDay,
    payoutCostPerUserPkr,
    netProfitLossPerUserPkr,
    monthlyNetProfitLossPkr,
    breakEvenAdsRequired,
    solvencyStatus,
  };
}
