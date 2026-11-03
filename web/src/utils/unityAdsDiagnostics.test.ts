import test from 'node:test';
import assert from 'node:assert/strict';
import { diagnoseUnityAdsConfig } from './unityAdsDiagnostics';

test('diagnoseUnityAdsConfig verifies user dashboard configuration', () => {
  // Verified FitPaisa configuration from the user screenshot:
  const fitPaisaCurrentConfig = {
    gameId: '800410563',
    adUnitId: 'BP_Rewarded_Android',
    testMode: true,
    hasAdMobFallback: true,
  };

  const result = diagnoseUnityAdsConfig(fitPaisaCurrentConfig);
  assert.equal(result.isConfigValid, true);
  const bpInfo = result.issues.find((i) => i.field === 'UNITY_REWARDED_AD_UNIT_ID');
  assert.ok(bpInfo);
  assert.equal(bpInfo?.type, 'INFO');
  assert.match(bpInfo?.message || '', /Verified from your dashboard screenshot/);
});
