/**
 * Wallet Security & Concurrency Verification Engine
 * Audits cashout vulnerabilities, race conditions, sybil accounts, and ledger integrity.
 */

export interface CashoutRequest {
  userId: string;
  userEmail: string;
  coins: number;
  method: 'easypaisa' | 'jazzcash' | 'bank';
  accountTitle: string;
  accountNumber: string;
  timestamp: number;
}

export interface SecurityVulnerabilityAudit {
  id: string;
  category: 'CONCURRENCY' | 'SYBIL_FRAUD' | 'UNVERIFIED_ADS' | 'IDEMPOTENCY' | 'AUDIT_TRAIL';
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM';
  title: string;
  currentCodeStatus: string;
  riskDescription: string;
  remedyCode: string;
}

/**
 * Detects Sybil accounts where multiple registered users link to the exact same payout phone number
 */
export function detectSybilAccounts(
  requests: CashoutRequest[]
): Map<string, { count: number; userIds: string[]; emails: string[]; totalCoinsRequested: number }> {
  const accountMap = new Map<
    string,
    { count: number; userIds: string[]; emails: string[]; totalCoinsRequested: number }
  >();

  for (const req of requests) {
    const normalizedAcc = req.accountNumber.replace(/[\s-]/g, '').trim();
    const existing = accountMap.get(normalizedAcc) || {
      count: 0,
      userIds: [],
      emails: [],
      totalCoinsRequested: 0,
    };

    if (!existing.userIds.includes(req.userId)) {
      existing.userIds.push(req.userId);
      existing.emails.push(req.userEmail);
    }
    existing.count += 1;
    existing.totalCoinsRequested += req.coins;
    accountMap.set(normalizedAcc, existing);
  }

  // Filter to only those with multiple users sharing the same wallet account
  const suspiciousMap = new Map<
    string,
    { count: number; userIds: string[]; emails: string[]; totalCoinsRequested: number }
  >();
  accountMap.forEach((val, key) => {
    if (val.userIds.length > 1) {
      suspiciousMap.set(key, val);
    }
  });

  return suspiciousMap;
}

/**
 * Simulates concurrent cashout race condition without PostgreSQL Advisory Lock
 */
export function simulateConcurrentCashouts(
  startingBalance: number,
  withdrawalAmount: number,
  concurrentCalls: number,
  useAdvisoryLock: boolean
): {
  approvedRequests: number;
  rejectedRequests: number;
  finalBalance: number;
  overdraftAmount: number;
  raceConditionPrevented: boolean;
} {
  let simulatedBalance = startingBalance;
  let approved = 0;
  let rejected = 0;

  if (useAdvisoryLock) {
    // With advisory lock: requests are serialized atomic transactions
    for (let i = 0; i < concurrentCalls; i++) {
      if (simulatedBalance >= withdrawalAmount) {
        simulatedBalance -= withdrawalAmount;
        approved++;
      } else {
        rejected++;
      }
    }
  } else {
    // Without advisory lock or FOR UPDATE:
    // Read phase happens simultaneously across all threads!
    const reads = Array(concurrentCalls).fill(startingBalance);
    for (let i = 0; i < concurrentCalls; i++) {
      if (reads[i] >= withdrawalAmount) {
        // All threads think balance is sufficient!
        approved++;
        simulatedBalance -= withdrawalAmount;
      } else {
        rejected++;
      }
    }
  }

  const overdraftAmount = Math.max(0, -simulatedBalance);
  return {
    approvedRequests: approved,
    rejectedRequests: rejected,
    finalBalance: simulatedBalance,
    overdraftAmount,
    raceConditionPrevented: useAdvisoryLock || approved * withdrawalAmount <= startingBalance,
  };
}
