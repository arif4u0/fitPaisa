import test from 'node:test';
import assert from 'node:assert/strict';
import { calculateEconomics } from './economicSimulator';

test('calculateEconomics reveals deficit in fitPaisa 1-ad vs 10k-step conversion in Pakistan', () => {
  // Current app parameters:
  // eCPM: $1.20 in Pakistan
  // USD to PKR: 280
  // 10,000 steps = 100 coins = 1 PKR
  // 1 ad watched
  const currentModel = calculateEconomics({
    ecpmUsd: 1.2,
    usdToPkrRate: 280,
    stepsPerCoin: 100,
    coinValuePaisa: 1,
    dailyStepCap: 10000,
    adsWatchedPerConvert: 1,
    dailyBonusCoins: 0,
    activeUsers: 1000,
  });

  // Revenue per ad: (1.20 / 1000) * 280 = 0.336 PKR
  assert.equal(Math.round(currentModel.adRevenuePerUserPkr * 100) / 100, 0.34);
  // Payout liability: 100 coins * 1 paisa = 1.00 PKR
  assert.equal(currentModel.payoutCostPerUserPkr, 1.0);
  // Net loss per user per day: 0.336 - 1.00 = -0.664 PKR
  assert.equal(currentModel.solvencyStatus, 'CRITICAL_DEFICIT');
  assert.equal(currentModel.breakEvenAdsRequired, 3); // 3 ads needed to cover 1 PKR!
  // Monthly loss across 1,000 daily active users:
  assert.ok(currentModel.monthlyNetProfitLossPkr < -15000); // Losing ~19,920 PKR / month!
});

test('calculateEconomics verifies solvent sustainable configuration', () => {
  // If the developer requires 3 ads or adjusts steps/coin ratio:
  const sustainableModel = calculateEconomics({
    ecpmUsd: 1.5,
    usdToPkrRate: 280,
    stepsPerCoin: 100,
    coinValuePaisa: 1,
    dailyStepCap: 10000,
    adsWatchedPerConvert: 3, // Multi-ad unlock or energy bar
    dailyBonusCoins: 0,
    activeUsers: 1000,
  });

  // Revenue per user: 3 * (1.5 / 1000 * 280) = 1.26 PKR
  // Payout cost: 1.00 PKR
  // Profit: +0.26 PKR per user
  assert.equal(sustainableModel.solvencyStatus, 'HEALTHY_PROFIT');
  assert.ok(sustainableModel.monthlyNetProfitLossPkr > 0);
});
