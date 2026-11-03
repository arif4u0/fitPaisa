/**
 * Reward Calculation & Anti-Abuse Verification Engine
 * Models both the legacy (buggy) FitPaisa logic and the proposed robust enterprise logic.
 */

export interface StepConversionParams {
  observedSteps: number;
  dailyCap: number;
  stepsPerCoin: number;
  alreadyCreditedSteps: number;
  isManualEntry?: boolean;
}

export interface StepConversionResult {
  eligibleTotal: number;
  newSteps: number;
  coinsEarned: number;
  remainingDailyCapSteps: number;
  totalCreditedAfter: number;
}

/**
 * Calculates coins strictly according to daily cap and delta steps
 */
export function calculateStepConversion(params: StepConversionParams): StepConversionResult {
  const {
    observedSteps,
    dailyCap,
    stepsPerCoin = 100,
    alreadyCreditedSteps = 0,
    isManualEntry = false,
  } = params;

  if (isManualEntry) {
    return {
      eligibleTotal: 0,
      newSteps: 0,
      coinsEarned: 0,
      remainingDailyCapSteps: dailyCap,
      totalCreditedAfter: alreadyCreditedSteps,
    };
  }

  const eligibleTotal = Math.min(Math.max(0, observedSteps), dailyCap);
  const newSteps = Math.max(0, eligibleTotal - alreadyCreditedSteps);
  const coinsEarned = Math.floor(newSteps / stepsPerCoin);
  const totalCreditedAfter = alreadyCreditedSteps + newSteps;
  const remainingDailyCapSteps = Math.max(0, dailyCap - totalCreditedAfter);

  return {
    eligibleTotal,
    newSteps,
    coinsEarned,
    remainingDailyCapSteps,
    totalCreditedAfter,
  };
}

/**
 * Demonstrates the Unique Key constraint failure in legacy sync-steps
 */
export function simulateLegacyMultiBatchConversion(
  userId: string,
  date: string,
  ruleVersion: number,
  existingBatches: string[]
): { allowed: boolean; sourceId: string; error?: string } {
  // Legacy logic used a static source_id per day:
  const legacySourceId = `steps:${userId}:${date}:v${ruleVersion}`;
  if (existingBatches.includes(legacySourceId)) {
    return {
      allowed: false,
      sourceId: legacySourceId,
      error: 'Postgres Error 23505: Unique constraint violation on source_type, source_id. Coins credited: 0',
    };
  }
  return {
    allowed: true,
    sourceId: legacySourceId,
  };
}

/**
 * Generates an immutable delta source_id for multiple safe conversions in a single day
 */
export function generateDeltaSourceId(
  userId: string,
  date: string,
  batchIndex: number,
  fromSteps: number,
  toSteps: number
): string {
  return `steps:${userId}:${date}:b${batchIndex}:${fromSteps}_${toSteps}`;
}

/**
 * Validates step cadence (steps per minute)
 * Human physiology:
 * - Leisure walking: 80 - 100 steps/min
 * - Fast walking: 110 - 130 steps/min
 * - Running: 150 - 180 steps/min
 * - Olympic sprint: 240 - 280 steps/min
 * > 300 steps/min without pause indicates phone shaker / swing / rooted memory injection.
 */
export function validateStepCadence(stepDelta: number, timeDeltaSeconds: number): {
  isValid: boolean;
  stepsPerMinute: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  reason: string;
} {
  if (timeDeltaSeconds <= 0) {
    return {
      isValid: false,
      stepsPerMinute: Infinity,
      riskLevel: 'CRITICAL',
      reason: 'Zero or negative time delta detected (instant injection).',
    };
  }

  const minutes = timeDeltaSeconds / 60;
  const stepsPerMinute = Math.round(stepDelta / minutes);

  if (stepsPerMinute <= 160) {
    return {
      isValid: true,
      stepsPerMinute,
      riskLevel: 'LOW',
      reason: 'Normal human walking/jogging cadence.',
    };
  } else if (stepsPerMinute <= 250) {
    return {
      isValid: true,
      stepsPerMinute,
      riskLevel: 'MEDIUM',
      reason: 'Vigorous running cadence. Plausible for athletic activity.',
    };
  } else if (stepsPerMinute <= 350) {
    return {
      isValid: false,
      stepsPerMinute,
      riskLevel: 'HIGH',
      reason: 'Exceeds sustainable human sprint cadence. Potential mechanical shaker or cycle.',
    };
  } else {
    return {
      isValid: false,
      stepsPerMinute,
      riskLevel: 'CRITICAL',
      reason: `Impossible cadence (${stepsPerMinute} steps/min). Automated script, emulator injection, or pendulum rocker.`,
    };
  }
}

/**
 * Validates local_date against server UTC date to prevent historic or future date forgery
 */
export function validateDateBoundary(
  clientDateStr: string,
  serverDate: Date = new Date(),
  maxAllowedHoursDrift = 28
): { isValid: boolean; hoursDrift: number; message: string } {
  const clientDate = new Date(`${clientDateStr}T12:00:00Z`);
  if (isNaN(clientDate.getTime())) {
    return { isValid: false, hoursDrift: NaN, message: 'Malformed date format. Must be YYYY-MM-DD.' };
  }

  const diffMs = clientDate.getTime() - serverDate.getTime();
  const diffHours = Math.abs(diffMs / (1000 * 60 * 60));

  if (diffHours > maxAllowedHoursDrift) {
    return {
      isValid: false,
      hoursDrift: Math.round(diffHours),
      message: `Date is ${Math.round(diffHours / 24)} days out of sync with server. Date forging rejected.`,
    };
  }

  return {
    isValid: true,
    hoursDrift: Math.round(diffHours),
    message: 'Date within acceptable timezone window.',
  };
}
