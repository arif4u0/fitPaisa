import test from 'node:test';
import assert from 'node:assert/strict';
import {
  calculateStepConversion,
  simulateLegacyMultiBatchConversion,
  generateDeltaSourceId,
  validateStepCadence,
  validateDateBoundary,
} from './rewardCalculator';

test('calculateStepConversion computes coins and deltas accurately', () => {
  // 5,420 steps with 10,000 cap and 0 credited
  const res1 = calculateStepConversion({
    observedSteps: 5420,
    dailyCap: 10000,
    stepsPerCoin: 100,
    alreadyCreditedSteps: 0,
  });
  assert.equal(res1.eligibleTotal, 5420);
  assert.equal(res1.newSteps, 5420);
  assert.equal(res1.coinsEarned, 54);
  assert.equal(res1.remainingDailyCapSteps, 4580);
  assert.equal(res1.totalCreditedAfter, 5420);

  // Second conversion in same day: now at 9,000 steps with 5,420 already credited
  const res2 = calculateStepConversion({
    observedSteps: 9000,
    dailyCap: 10000,
    stepsPerCoin: 100,
    alreadyCreditedSteps: 5420,
  });
  assert.equal(res2.eligibleTotal, 9000);
  assert.equal(res2.newSteps, 3580);
  assert.equal(res2.coinsEarned, 35);
  assert.equal(res2.remainingDailyCapSteps, 1000);
  assert.equal(res2.totalCreditedAfter, 9000);

  // Third conversion exceeding daily cap of 10,000: user claims 15,000 steps
  const res3 = calculateStepConversion({
    observedSteps: 15000,
    dailyCap: 10000,
    stepsPerCoin: 100,
    alreadyCreditedSteps: 9000,
  });
  assert.equal(res3.eligibleTotal, 10000);
  assert.equal(res3.newSteps, 1000);
  assert.equal(res3.coinsEarned, 10);
  assert.equal(res3.remainingDailyCapSteps, 0);
  assert.equal(res3.totalCreditedAfter, 10000);
});

test('calculateStepConversion excludes manual step entries', () => {
  const res = calculateStepConversion({
    observedSteps: 8000,
    dailyCap: 10000,
    stepsPerCoin: 100,
    alreadyCreditedSteps: 0,
    isManualEntry: true,
  });
  assert.equal(res.coinsEarned, 0);
  assert.equal(res.newSteps, 0);
  assert.equal(res.eligibleTotal, 0);
});

test('simulateLegacyMultiBatchConversion highlights the Postgres 23505 bug', () => {
  const existingBatches: string[] = [];
  const b1 = simulateLegacyMultiBatchConversion('u1', '2026-10-25', 2, existingBatches);
  assert.equal(b1.allowed, true);
  existingBatches.push(b1.sourceId);

  // Afternoon attempt on same day with legacy logic
  const b2 = simulateLegacyMultiBatchConversion('u1', '2026-10-25', 2, existingBatches);
  assert.equal(b2.allowed, false);
  assert.match(b2.error!, /23505/);

  // Proposed delta source ID fixes this completely
  const deltaKey1 = generateDeltaSourceId('u1', '2026-10-25', 1, 0, 4000);
  const deltaKey2 = generateDeltaSourceId('u1', '2026-10-25', 2, 4000, 8000);
  assert.notEqual(deltaKey1, deltaKey2);
});

test('validateStepCadence detects abnormal sensor injection', () => {
  // 1,000 steps in 10 minutes (100 steps/min) = Normal walking
  const normal = validateStepCadence(1000, 600);
  assert.equal(normal.isValid, true);
  assert.equal(normal.riskLevel, 'LOW');

  // 10,000 steps in 2 minutes (5,000 steps/min) = Impossible injection / bot
  const bot = validateStepCadence(10000, 120);
  assert.equal(bot.isValid, false);
  assert.equal(bot.riskLevel, 'CRITICAL');
});

test('validateDateBoundary rejects past or future forged dates', () => {
  const mockServerTime = new Date('2026-10-25T12:00:00Z');

  // Valid today
  const valid = validateDateBoundary('2026-10-25', mockServerTime);
  assert.equal(valid.isValid, true);

  // Attack: requesting rewards for 6 months ago to farm coins
  const forgedPast = validateDateBoundary('2026-04-10', mockServerTime);
  assert.equal(forgedPast.isValid, false);
  assert.match(forgedPast.message, /Date forging rejected/);
});
