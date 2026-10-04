# fitPaisa — Google Play Store Launch Checklist

Everything needed to go from "works on my phone" to "anyone can download it".

## 1. Sign the app (do once, back up forever)
1. Android Studio → **Build → Generate Signed Bundle / APK** → **Android App Bundle**.
2. **Create new** keystore: pick a folder OUTSIDE the project, set a strong password.
   - ⚠️ **Back up this .jks file + passwords in two places.** If you lose it, you can
     never update the app again — Google won't accept a new key.
3. Build type: **release**. This produces `app-release.aab` (minified, per our config).

## 2. Host the privacy policy (required)
Google rejects apps requesting activity recognition without a public privacy policy.
- `docs/privacy-policy.html` in this repo is your draft — replace `[YOUR SUPPORT EMAIL]`.
- Put it on a free public host. Easiest: **Google Sites** (3 clicks, free):
  Sites → blank site → paste the policy text → Publish → copy the URL.
- That URL goes into Play Console → App content → Privacy policy.

## 3. Play Console setup (play.google.com/console, $25 one-time fee)
1. **Create app** → name: fitPaisa, language, free app.
2. **Store listing:** short + full description, app icon (we have it), **feature
   graphic** (`docs/fitpaisa-feature-graphic.png`), 2–8 phone screenshots
   (take them from your phone on the Home/Wallet/Levels screens).
3. **App content:**
   - **Privacy policy:** the URL from step 2.
   - **Data safety form:** declare — Physical activity (step counts), Account
     (name/email), Financial info (payout account, only if user redeems).
     Purposes: app functionality + fraud prevention. No data sold, no ads profiling.
   - **Content rating:** fill the questionnaire honestly (it's a fitness rewards app, 18+).
   - **Target audience:** 18+.
4. **Permissions declaration:** for `ACTIVITY_RECOGNITION`, Play asks why — answer:
   *"Step counts are the core of the app: they are converted into FitCoin rewards."*
   Our in-app permission card + privacy policy already cover the prominent disclosure.
5. **App access:** no login needed for reviewers beyond Google sign-in (it's the only login).

## 4. Earning-app policy notes (read carefully)
- **No misleading earnings:** never promise fixed income ("earn Rs 500 daily!").
  Our wording ("up to ~X coins", "100 steps = 1 coin") is factual — keep it that way.
- **18+ only:** enforced at onboarding and in the rating questionnaire.
- **Real payouts:** only enable `cashout_enabled` when you can actually pay.
  Reviews check that advertised rewards are real.
- **No gambling mechanics:** we have none (no luck draws, no paid entries). Keep it so.

## 5. Release tracks (recommended order)
1. **Internal testing** track first (you + a few friends) — install via the Play link,
   confirm sign-in, steps, and Convert work on real devices.
2. **Closed testing** (optional, wider group).
3. **Production** — the public listing.

## 6. Before every production release
- [ ] Bump `versionCode` (+1) in `app/build.gradle.kts` — Play rejects re-uploads.
- [ ] Update `versionName` (e.g. 1.0.1).
- [ ] Pull latest `master`, build the **release** AAB (not debug APK).
- [ ] Smoke test the release build on your phone first (install the AAB via
      `bundletool` or the internal-testing track) — minification can hide issues
      debug builds don't show.

## 7. After launch
- Watch **Android vitals** (crashes, ANRs) in Play Console weekly.
- Keep an eye on the `payout_requests` table as redemptions start.
- Tune economics in `EconomyConfig.kt` + `reward_rules` v3 if payouts run hot.
