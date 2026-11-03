import test from 'node:test';
import assert from 'node:assert/strict';
import { detectSybilAccounts, simulateConcurrentCashouts, CashoutRequest } from './walletSecurity';

test('detectSybilAccounts flags multiple distinct users using the same Easypaisa account', () => {
  const requests: CashoutRequest[] = [
    {
      userId: 'user_1',
      userEmail: 'honest@gmail.com',
      coins: 10000,
      method: 'easypaisa',
      accountTitle: 'Ali Khan',
      accountNumber: '03001234567',
      timestamp: Date.now(),
    },
    {
      userId: 'user_2', // Fraudster clone 1
      userEmail: 'bot1@gmail.com',
      coins: 10000,
      method: 'easypaisa',
      accountTitle: 'Zubair',
      accountNumber: '03459998888',
      timestamp: Date.now(),
    },
    {
      userId: 'user_3', // Fraudster clone 2 with SAME phone number
      userEmail: 'bot2@gmail.com',
      coins: 10000,
      method: 'easypaisa',
      accountTitle: 'Zubair K',
      accountNumber: '0345-999-8888',
      timestamp: Date.now(),
    },
  ];

  const sybils = detectSybilAccounts(requests);
  assert.equal(sybils.size, 1);
  const flagged = sybils.get('03459998888');
  assert.ok(flagged);
  assert.equal(flagged.userIds.length, 2);
  assert.equal(flagged.totalCoinsRequested, 20000);
});

test('simulateConcurrentCashouts demonstrates race condition vulnerability without advisory lock', () => {
  // User has exactly 10,000 coins (= Rs 100)
  // Attacker fires 5 parallel requests each withdrawing 10,000 coins
  const vulnerable = simulateConcurrentCashouts(10000, 10000, 5, false);
  // Without lock, all 5 read balance = 10,000 and pass the check!
  assert.equal(vulnerable.approvedRequests, 5);
  assert.equal(vulnerable.overdraftAmount, 40000); // 40,000 coins stolen from founder!
  assert.equal(vulnerable.raceConditionPrevented, false);

  // With PostgreSQL advisory lock:
  const protectedRun = simulateConcurrentCashouts(10000, 10000, 5, true);
  assert.equal(protectedRun.approvedRequests, 1);
  assert.equal(protectedRun.rejectedRequests, 4);
  assert.equal(protectedRun.finalBalance, 0);
  assert.equal(protectedRun.overdraftAmount, 0);
  assert.equal(protectedRun.raceConditionPrevented, true);
});
