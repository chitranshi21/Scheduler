# Quick Start Guide - Clerk Authentication

Follow these steps to configure and run the Session Scheduler application with Clerk authentication.

---

## Step 1: Get Clerk API Keys

1. Go to [https://dashboard.clerk.com](https://dashboard.clerk.com)
2. Sign in or create an account
3. Create a new application (or select existing)
4. From the dashboard, copy these values:
   - **Publishable Key** (starts with `pk_test_`)
   - **Secret Key** (starts with `sk_test_`)
   - **Frontend API** (e.g., `your-app-name.clerk.accounts.dev`)

---

## Step 2: Configure Backend

Edit `backend/src/main/resources/application.properties`:

Replace the following placeholders with your actual Clerk values:

```properties
# Line 24: Replace with https://api.clerk.com (already set)
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://api.clerk.com

# Line 25: Replace YOUR_APP with your actual Clerk app subdomain
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://YOUR_APP.clerk.accounts.dev/.well-known/jwks.json

# Line 29: Replace with your Clerk Publishable Key
clerk.publishable-key=pk_test_YOUR_PUBLISHABLE_KEY

# Line 30: Replace with your Clerk Secret Key
clerk.secret-key=sk_test_YOUR_SECRET_KEY
```

**Your current values are already configured:**
- `issuer-uri`: ✓ Set to `https://api.clerk.com`
- `jwk-set-uri`: ✓ Set to `https://tender-racer-40.clerk.accounts.dev/.well-known/jwks.json`
- `publishable-key`: ✓ Set to `pk_test_dGVuZGVyLXJhY2VyLTQwLmNsZXJrLmFjY291bnRzLmRldiQ`
- `secret-key`: ✓ Set to `sk_test_g2SZvjlhr8wZG53u97LyQ95qULSLRGcQTVJ6ljkoII`

---

## Step 3: Configure Frontend

1. **Navigate to frontend directory:**
   ```bash
   cd frontend
   ```

2. **Create `.env.local` file:**
   ```bash
   touch .env.local
   ```

3. **Add your Clerk Publishable Key to `.env.local`:**
   ```env
   VITE_CLERK_PUBLISHABLE_KEY=pk_test_dGVuZGVyLXJhY2VyLTQwLmNsZXJrLmFjY291bnRzLmRldiQ
   ```

4. **Install dependencies:**
   ```bash
   npm install
   ```

---

## Step 4: Start the Application

**Terminal 1 - Start Backend:**
```bash
cd backend
mvn spring-boot:run
```

Backend will start on: **http://localhost:8080**

**Terminal 2 - Start Frontend:**
```bash
cd frontend
npm run dev
```

Frontend will start on: **http://localhost:5173**

---

## Step 5: Create Your First User

1. **Open browser:** Navigate to `http://localhost:5173`

2. **Sign up:** Click "Don't have an account? Sign up"

3. **Fill in details:**
   - Email
   - Password
   - Complete verification if required

4. **Sign in:** After signup, sign in with your credentials

---

## Step 6: Set User Role in Clerk Dashboard

By default, users have the `CUSTOMER` role. To access Admin or Business features:

1. **Go to Clerk Dashboard:** [https://dashboard.clerk.com](https://dashboard.clerk.com)

2. **Navigate to Users:** Click "Users" in the sidebar

3. **Select your user:** Click on the email you just created

4. **Edit Public Metadata:**
   - Scroll to the "Metadata" section
   - Click "Edit" under "Public metadata"

5. **Add role:**

   **For Admin access:**
   ```json
   {
     "role": "ADMIN"
   }
   ```

   **For Business access:**
   ```json
   {
     "role": "BUSINESS"
   }
   ```

   **For Customer access (default):**
   ```json
   {
     "role": "CUSTOMER"
   }
   ```

6. **Save changes:** Click "Save"

7. **Log out and log back in:** The new role will take effect after re-authentication

---

## Step 7: Test Role-Based Access

After setting the role and logging back in:

- **ADMIN users** → Redirected to `/admin`
- **BUSINESS users** → Redirected to `/business`
- **CUSTOMER users** → Redirected to customer portal

---

## Verification

### Test Backend is Running:
```bash
curl http://localhost:8080/api/auth/test
```
Expected output: `API is working!`

### Test Frontend is Running:
Open `http://localhost:5173` in your browser

### Test Authentication:
1. Sign up/Sign in
2. Open browser DevTools (F12)
3. Go to Network tab
4. Make any action that triggers an API call
5. Check request headers for: `Authorization: Bearer eyJ...`

---

## Troubleshooting

### Backend won't start

**Error: "Invalid issuer-uri"**
- Verify `issuer-uri` is set to: `https://api.clerk.com`
- Verify `jwk-set-uri` matches your Clerk Frontend API

**Error: "Bean creation error"**
- All Supabase files have been removed
- Run: `mvn clean compile`

### Frontend won't start

**Error: "Missing Clerk Publishable Key"**
- Ensure `.env.local` exists in `frontend` directory
- Verify variable name: `VITE_CLERK_PUBLISHABLE_KEY`
- Restart dev server: `npm run dev`

### Can't access protected routes after login

**User redirected to login immediately**
- Set user role in Clerk Dashboard public metadata
- Log out and log back in

### 401 Unauthorized on API calls

**API returns 401**
- Check browser console for errors
- Verify JWT token in request headers
- Check backend logs for JWT validation errors

---

## Quick Commands Summary

```bash
# Backend
cd backend
mvn spring-boot:run

# Frontend (new terminal)
cd frontend
npm install
npm run dev

# Access application
open http://localhost:5173
```

---

## What's Next?

- Create additional users with different roles
- Explore the Admin dashboard (`/admin`)
- Explore the Business dashboard (`/business`)
- Test the Customer booking flow (`/book/:slug`)

For detailed information, see:
- [CLERK_SETUP_GUIDE.md](./CLERK_SETUP_GUIDE.md) - Complete setup documentation
- [MIGRATION_SUMMARY.md](./MIGRATION_SUMMARY.md) - Technical migration details

---

## Your Current Configuration

Based on your `application.properties`, you're using:
- **Clerk Frontend API:** `tender-racer-40.clerk.accounts.dev`
- **Issuer URI:** `https://api.clerk.com` ✓
- **JWKS URI:** `https://tender-racer-40.clerk.accounts.dev/.well-known/jwks.json` ✓

All backend configurations are correct. Just need to:
1. Create `.env.local` in frontend with your publishable key
2. Run `npm install` in frontend
3. Start both services
4. Create users and set roles
