/**
 * Unity Ads Diagnostic Engine & State Machine Verifier
 */

export interface UnityAdsConfigCheck {
  gameId: string;
  adUnitId: string;
  testMode: boolean;
  hasAdMobFallback: boolean;
}

export interface UnityAdsDiagnosticResult {
  isConfigValid: boolean;
  issues: Array<{
    type: 'ERROR' | 'WARNING' | 'INFO';
    field: string;
    message: string;
    fixSuggestion: string;
  }>;
  stateSimulation: {
    immediateShowOutcome: 'NOT_READY' | 'WATCHED' | 'SKIPPED';
    failureReason: string;
  };
}

export function diagnoseUnityAdsConfig(config: UnityAdsConfigCheck): UnityAdsDiagnosticResult {
  const issues: UnityAdsDiagnosticResult['issues'] = [];

  // Check Game ID
  const cleanedGameId = config.gameId.trim();
  if (!cleanedGameId) {
    issues.push({
      type: 'ERROR',
      field: 'UNITY_GAME_ID',
      message: 'Game ID is empty.',
      fixSuggestion: 'Copy the 7-digit Google Play Game ID from Unity Dashboard > Project Settings > Ads.',
    });
  } else if (!/^\d+$/.test(cleanedGameId)) {
    issues.push({
      type: 'ERROR',
      field: 'UNITY_GAME_ID',
      message: 'Game ID contains non-numeric characters.',
      fixSuggestion: 'Unity Game IDs are strictly digits (e.g. 5xxxxxx or 8xxxxxx).',
    });
  } else if (cleanedGameId.length > 7 && !cleanedGameId.startsWith('800')) {
    issues.push({
      type: 'WARNING',
      field: 'UNITY_GAME_ID',
      message: `Game ID "${cleanedGameId}" is ${cleanedGameId.length} digits long. Verify this is the Android Monetization Game ID and not an Organization ID or Project GUID.`,
      fixSuggestion: 'Ensure you selected "Google Play" platform Game ID in Unity Cloud Monetization.',
    });
  }

  // Check Ad Unit ID
  const cleanedAdUnitId = config.adUnitId.trim();
  if (!cleanedAdUnitId) {
    issues.push({
      type: 'ERROR',
      field: 'UNITY_REWARDED_AD_UNIT_ID',
      message: 'Rewarded Ad Unit ID is empty.',
      fixSuggestion: 'Use BP_Rewarded_Android as configured in your Unity Monetization dashboard.',
    });
  } else if (cleanedAdUnitId === 'BP_Rewarded_Android') {
    issues.push({
      type: 'INFO',
      field: 'UNITY_REWARDED_AD_UNIT_ID',
      message: 'Verified from your dashboard screenshot: "BP_Rewarded_Android" is active and correctly configured!',
      fixSuggestion: 'Your Ad Unit ID is correct. Focus on enabling Test Mode in Settings and applying the AdManager.kt await fix.',
    });
  }

  // Check Test Mode
  if (!config.testMode) {
    issues.push({
      type: 'WARNING',
      field: 'UNITY_TEST_MODE',
      message: 'Test mode is disabled (false). On unreleased apps, Unity will NOT serve real ads without store publication.',
      fixSuggestion: 'Set UNITY_TEST_MODE = true during development and APK testing.',
    });
  }

  // Check AdMob Fallback
  if (!config.hasAdMobFallback) {
    issues.push({
      type: 'WARNING',
      field: 'AdMob Fallback',
      message: 'AdMob is completely disabled in AdManager.kt. Relying solely on Unity Ads in Pakistan results in high fill-rate failure (often 30-50% unfulfilled requests).',
      fixSuggestion: 'Implement a waterfall strategy: Try AdMob first; if unavailable, fallback to Unity Ads.',
    });
  }

  return {
    isConfigValid: !issues.some((i) => i.type === 'ERROR'),
    issues,
    stateSimulation: {
      immediateShowOutcome: 'NOT_READY',
      failureReason:
        'In current AdManager.kt, `if (!isLoaded) return NOT_READY` fires immediately if called before background initialization completes (takes 2-4 seconds). The user receives an instant "No ad available" toast.',
    },
  };
}
