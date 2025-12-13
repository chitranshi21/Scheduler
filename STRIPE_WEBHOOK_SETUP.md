# Stripe Webhook Setup Guide

## Problem: Payment Status Shows as "PENDING" After Successful Payment

If you see "PENDING_PAYMENT" status in the business dashboard even after a successful payment, it's likely because Stripe webhooks are not configured or not reaching your backend.

## Solution 1: Configure Stripe Webhooks (Recommended for Production)

### Step 1: Get Your Webhook URL

Your webhook endpoint is:
```
http://localhost:8080/api/stripe/webhook
```

**For local development**, you'll need to use a tool like **ngrok** or **Stripe CLI** to expose your local server to the internet.

### Step 2: Using Stripe CLI (Easiest for Local Testing)

1. **Install Stripe CLI:**
   ```bash
   # macOS
   brew install stripe/stripe-cli/stripe
   
   # Or download from: https://stripe.com/docs/stripe-cli
   ```

2. **Login to Stripe:**
   ```bash
   stripe login
   ```

3. **Forward webhooks to your local server:**
   ```bash
   stripe listen --forward-to localhost:8080/api/stripe/webhook
   ```

4. **Copy the webhook signing secret** (starts with `whsec_`) and update your `application.properties`:
   ```properties
   stripe.webhook-secret=whsec_your_webhook_secret_here
   ```

5. **Restart your backend** to load the new webhook secret.

### Step 3: Using ngrok (Alternative for Local Testing)

1. **Install ngrok:**
   ```bash
   # macOS
   brew install ngrok
   ```

2. **Start your backend** on port 8080

3. **Expose your local server:**
   ```bash
   ngrok http 8080
   ```

4. **Copy the HTTPS URL** (e.g., `https://abc123.ngrok.io`)

5. **In Stripe Dashboard:**
   - Go to **Developers** → **Webhooks**
   - Click **Add endpoint**
   - Endpoint URL: `https://abc123.ngrok.io/api/stripe/webhook`
   - Select events: `checkout.session.completed`, `payment_intent.payment_failed`, `checkout.session.expired`
   - Click **Add endpoint**

6. **Copy the webhook signing secret** and update `application.properties` as above.

### Step 4: Configure Webhook in Stripe Dashboard (Production)

1. Go to [Stripe Dashboard](https://dashboard.stripe.com) → **Developers** → **Webhooks**
2. Click **Add endpoint**
3. Enter your production webhook URL: `https://yourdomain.com/api/stripe/webhook`
4. Select these events:
   - `checkout.session.completed`
   - `payment_intent.payment_failed`
   - `checkout.session.expired`
   - `payment_intent.canceled`
5. Click **Add endpoint**
6. Copy the **Signing secret** (starts with `whsec_`)
7. Update your production environment variables:
   ```bash
   STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret_here
   ```

## Solution 2: Automatic Payment Status Sync (Already Implemented)

The system now automatically syncs payment status from Stripe when you view bookings in the business dashboard. This happens in the background, so even if webhooks aren't configured, payment status will be updated when you:

1. View the bookings tab
2. Click the "Refresh" button

### How It Works

- When you load bookings, the system checks for any bookings with `PENDING_PAYMENT` status
- For each pending booking, it queries Stripe directly to check the payment status
- If payment is successful in Stripe, it updates the booking status to `CONFIRMED`
- Payment status is updated to `COMPLETED`

## Solution 3: Manual Payment Status Sync

You can also manually sync payment status for a specific booking using the API:

```bash
POST /api/stripe/sync-payment-status/{bookingId}
```

This endpoint:
- Queries Stripe for the checkout session status
- Updates payment and booking status accordingly
- Confirms the booking if payment is successful

## Testing Webhooks Locally

### Using Stripe CLI:

1. **Start webhook forwarding:**
   ```bash
   stripe listen --forward-to localhost:8080/api/stripe/webhook
   ```

2. **Trigger a test event:**
   ```bash
   stripe trigger checkout.session.completed
   ```

3. **Check your backend logs** for:
   ```
   Received Stripe webhook event: checkout.session.completed
   Processing successful payment for booking: ...
   ✅ Payment marked as COMPLETED for booking: ...
   ✅ Booking ... confirmed and emails sent
   ```

## Verification Checklist

After setting up webhooks, verify:

- [ ] Webhook endpoint is accessible (test with Stripe CLI or ngrok)
- [ ] Webhook signing secret is configured in `application.properties`
- [ ] Backend logs show webhook events being received
- [ ] Payment status updates to `COMPLETED` after successful payment
- [ ] Booking status updates to `CONFIRMED` after successful payment
- [ ] Confirmation emails are sent after payment

## Troubleshooting

### Webhooks Not Received

1. **Check webhook URL is correct:**
   - Must be publicly accessible (use ngrok for local)
   - Must use HTTPS in production
   - Must match exactly: `/api/stripe/webhook`

2. **Check webhook secret:**
   - Must match the secret from Stripe Dashboard
   - Check `application.properties` or environment variable

3. **Check backend logs:**
   - Look for "Received Stripe webhook event" messages
   - Check for "Invalid webhook signature" errors

4. **Test with Stripe CLI:**
   ```bash
   stripe listen --forward-to localhost:8080/api/stripe/webhook
   ```

### Payment Status Still Shows PENDING

1. **Click Refresh button** in the bookings tab (triggers automatic sync)
2. **Check backend logs** for sync errors
3. **Verify Stripe checkout session ID** is stored in payment record
4. **Manually sync** using the API endpoint

### Webhook Signature Verification Fails

- Ensure webhook secret is correct
- Check that the secret doesn't have extra spaces or quotes
- Restart backend after updating webhook secret

## Current Implementation

The system now includes:

1. **Automatic sync** when viewing bookings (checks Stripe for pending payments)
2. **Manual sync endpoint** for troubleshooting
3. **Webhook handler** for real-time updates (when configured)

Even without webhooks configured, the automatic sync ensures payment status stays up-to-date when you view the bookings page.

## Important Notes

- **Local Development**: Use Stripe CLI or ngrok to forward webhooks
- **Production**: Configure webhooks in Stripe Dashboard with your production URL
- **Automatic Sync**: Works as a fallback but webhooks are preferred for real-time updates
- **Rate Limits**: Stripe API has rate limits, so automatic sync is done only when viewing bookings

