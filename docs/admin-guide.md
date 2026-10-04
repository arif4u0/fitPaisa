# fitPaisa Redemption Management Guide (for the founder)

How you handle every cash or product redemption, step by step. No code needed —
everything below is done in your Supabase dashboard (and your Easypaisa/JazzCash app).

## The golden rule

**Money (or a product) moves first, the "paid" button second.** Never mark a
request as paid from memory or a screenshot. The app's coin ledger follows what
YOU confirm — so your confirmation is the real payout.

---

## Part A — Cash redemption (Easypaisa / JazzCash / bank)

### What the user does (in the app)
1. In Wallet, the user saves a payout method: Easypaisa, JazzCash, or bank +
   account holder name + account/IBAN number. (Stored encrypted; you see a masked
   version like `0345••••123`.)
2. The user taps **Cash out**, enters coins (minimum **10,000 coins = Rs 100**),
   and confirms.
3. Immediately: those coins are **reserved** — they leave the user's spendable
   balance so they can't be spent twice. The request appears with a reference
   code like `FP-X7K2P9QA` and state `requested`.

### What you do
1. **See new requests:** Supabase dashboard → **Table Editor** → `payout_requests`
   → filter `state = requested`, newest first. Each row shows: user, coins,
   paisa (= rupees), reference code, and the payout method + masked account.
2. **Verify:** open the row's `payout_methods` (via `payout_method_id`) for the
   full account details. Check the account holder name looks genuine.
3. **Send the money:** open your Easypaisa/JazzCash/bank app and send the exact
   rupee amount to the user's account. **Save the transaction ID.**
4. **Mark paid:** Supabase dashboard → **Edge Functions** → `review-cashout` →
   **Invoke**, with header `x-admin-key: YOUR_ADMIN_KEY` and body:
   ```json
   {
     "request_id": "<paste the request id>",
     "decision": "paid",
     "provider_reference": "<Easypaisa/JazzCash transaction ID>",
     "note": "Paid via Easypaisa on 2026-10-05"
   }
   ```
5. Done. The user's reserved coins are settled, and the full history stays in
   the ledger forever (nothing is ever deleted).

### If something is wrong (fake account, suspended user, duplicate)
Invoke `review-cashout` with `"decision": "rejected"` and a note. The reserved
coins are **automatically returned** to the user's balance. You don't touch coins
manually — ever.

### Request states (in order)
`requested` → `under_review` → `approved` → `paid`
Side exits: `rejected`, `failed`, `cancelled` (all return the coins to the user).

### Practical routine (recommended)
- Process redemptions in **batches twice a week** (e.g. Monday + Thursday).
- Minimum exists so you're not sending Rs 5 transfers all day.
- Keep a simple spreadsheet: reference code → transaction ID → date. (The
  `provider_reference` field in the app already stores the txn ID per request.)

### One-time setup (do this before enabling cashouts)
1. Supabase dashboard → **Edge Functions** → **Secrets** → add:
   `ADMIN_API_KEY` = a long random password you invent (e.g. 32 random
   characters). **This key is your master stamp — never share it, never put it
   in the app.**
2. When you're ready for real money: run in the SQL editor:
   `update public.app_config set value = 'true'::jsonb where key = 'cashout_enabled';`
   Until then, the Cash out button stays safely disabled in the app.

---

## Part B — Product redemption (coming next)

You publish products (e.g. a water bottle for 5,000 coins, earbuds for 20,000).
The flow mirrors cash:

1. **You publish:** product name, photo, coin price, stock. (Admin tool coming next.)
2. **User redeems:** in the app's Rewards tab → taps Redeem → coins reserved →
   order created with the user's delivery name, address, and phone.
3. **You ship:** pack the product, book TCS/leopard courier, note the tracking number.
4. **You confirm:** mark the order **delivered** with the tracking number.
   Rejecting returns the coins to the user, same as cash.

**Pricing advice:** price products so the coin cost is *slightly worse* value than
cash (e.g. a Rs 500 item for 55,000 coins instead of 50,000). This nudges users
toward cash (cheaper for you to fulfill) while products feel like a treat.

---

## Anti-fraud checklist (before every batch)

- [ ] The request is still in `requested`/`under_review` (not already paid).
- [ ] The user account is `active` (not suspended) — check `profiles.status`.
- [ ] The coin amount matches the displayed rate (100 coins = Rs 1).
- [ ] No duplicate payout destination across many accounts (one person, many accounts).
- [ ] Step coins were held 7 days before becoming spendable (automatic — the app
      enforces the maturing window; anyone cashing out on day 1 is impossible).

## What you NEVER do
- Never edit `ledger_entries` by hand. All coin movements go through the functions.
- Never mark `paid` before the money actually left your account.
- Never share `ADMIN_API_KEY` or the service-role key with anyone.
