# Clerk Authentication Setup and Configuration Guide

This guide provides step-by-step instructions to configure and run the Session Scheduler application with Clerk authentication.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Clerk Account Setup](#clerk-account-setup)
3. [Backend Configuration](#backend-configuration)
4. [Frontend Configuration](#frontend-configuration)
5. [Setting Up User Roles](#setting-up-user-roles)
6. [Running the Application](#running-the-application)
7. [Testing the Integration](#testing-the-integration)
8. [Troubleshooting](#troubleshooting)

---

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java 17** or higher
- **Maven 3.6+**
- **Node.js 18+** and npm
- A **Clerk account** (free tier available at [clerk.com](https://clerk.com))

---

## Clerk Account Setup

### Step 1: Create a Clerk Account

1. Go to [https://clerk.com](https://clerk.com)
2. Click "Start building for free" or "Sign up"
3. Create an account using your email or OAuth provider

### Step 2: Create a New Application

1. Once logged in, click "Create Application"
2. Enter an application name (e.g., "Session Scheduler")
3. Choose your authentication methods:
   - Email/Password (recommended for this app)
   - OAuth providers (Google, GitHub, etc.) - optional
4. Click "Create Application"

### Step 3: Get Your API Keys

After creating your application, you'll see the API Keys page. You need:

1. **Publishable Key** (starts with `pk_test_` or `pk_live_`)
2. **Secret Key** (starts with `sk_test_` or `sk_live_`)
3. **Frontend API URL** (e.g., `your-app-name-123.clerk.accounts.dev`)

3. **Frontend API URL** (e.g., `your-app-name-123.clerk.accounts.dev`)

Keep these values handy - you'll need them for configuration.

### Step 4: Configure JWT Template

**CRITICAL STEP:** You must configure Clerk to include user roles in the JWT token.

1. Go to Clerk Dashboard -> **Configure** -> **Sessions**
2. Click **Customize Session Token**
3. In the **Claims** section, you will see the default claims.
4. You need to ensure `public_metadata` is included. It might be there by default, but if not, or if you are using a custom template:
   - Go to **JWT Templates** (under Configure)
   - Create a new template or edit the default one.
   - **Crucially**, if you are just using the default Session Token, you might need to add a custom claim if it's not appearing.
   - **BETTER APPROACH for this App:**
     1. Go to **Configure** -> **Sessions** -> **Customize Session Token**
     2. Add a custom claim:
        ```json
        {
          "public_metadata": "{{user.public_metadata}}",
          "email": "{{user.primary_email_address}}"
        }
        ```
     3. **Save** the changes.

**Without this step, the backend will not see the user's role and will default to CUSTOMER access (403 Forbidden).**

---

## Backend Configuration

### Step 1: Update application.properties

Navigate to `backend/src/main/resources/application.properties` and update the following values:

```properties
# Clerk OAuth2 JWT Configuration
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://YOUR_CLERK_FRONTEND_API
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://YOUR_CLERK_FRONTEND_API/.well-known/jwks.json

# Clerk Configuration
clerk.publishable-key=YOUR_CLERK_PUBLISHABLE_KEY
clerk.secret-key=YOUR_CLERK_SECRET_KEY
```

**Replace the placeholders:**
- `YOUR_CLERK_FRONTEND_API` → Your Clerk Frontend API URL (e.g., `your-app-name-123.clerk.accounts.dev`)
- `YOUR_CLERK_PUBLISHABLE_KEY` → Your Clerk Publishable Key
- `YOUR_CLERK_SECRET_KEY` → Your Clerk Secret Key

**Example:**
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://happy-toucan-12.clerk.accounts.dev
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://happy-toucan-12.clerk.accounts.dev/.well-known/jwks.json

clerk.publishable-key=pk_test_aGFwcHktdG91Y2FuLTEyLmNsZXJrLmFjY291bnRzLmRldiQ
clerk.secret-key=sk_test_abcdef1234567890abcdef1234567890
```

### Step 2: Build the Backend

```bash
cd backend
mvn clean install
```

---

## Frontend Configuration

### Step 1: Install Dependencies

```bash
cd frontend
npm install
```

This will install all required dependencies, including `@clerk/clerk-react`.

### Step 2: Create Environment Variables

Create a `.env.local` file in the `frontend` directory:

```bash
cd frontend
touch .env.local
```

Add the following content to `.env.local`:

```env
VITE_CLERK_PUBLISHABLE_KEY=pk_test_your_actual_publishable_key_here
```

**Replace** `pk_test_your_actual_publishable_key_here` with your actual Clerk Publishable Key.

**Example:**
```env
VITE_CLERK_PUBLISHABLE_KEY=pk_test_aGFwcHktdG91Y2FuLTEyLmNsZXJrLmFjY291bnRzLmRldiQ
```

---

## Setting Up User Roles

The application uses role-based access control with three roles:
- `ADMIN` - Full access to admin dashboard
- `BUSINESS` - Access to business dashboard
- `CUSTOMER` - Basic user access

### Step 1: Create Users in Clerk

1. Go to your Clerk Dashboard
2. Navigate to **Users** section
3. Click **Create User**
4. Enter user details (email, password, name)
5. Click **Create**

### Step 2: Assign Roles via Public Metadata

After creating a user, you need to assign them a role:

1. In the Clerk Dashboard, go to **Users**
2. Click on the user you want to configure
3. Scroll down to **Metadata** section
4. Click **Edit** on **Public Metadata**
5. Add the following JSON (choose the appropriate role):

**For Admin users:**
```json
{
  "role": "ADMIN"
}
```

**For Business users:**
```json
{
  "role": "BUSINESS",
  "tenant_id": "optional-tenant-id"
}
```

**For Customer users:**
```json
{
  "role": "CUSTOMER"
}
```

6. Click **Save**

**Note:** If no role is specified, users default to `CUSTOMER` role.

### Step 3: Verify Metadata

The public metadata should look like this in the dashboard:

```json
{
  "role": "ADMIN"
}
```

This metadata will be included in the JWT token and used by the backend for authorization.

---

## Running the Application

### Step 1: Start the Backend

Open a terminal in the `backend` directory:

```bash
cd backend
mvn spring-boot:run
```

The backend should start on `http://localhost:8080`

**Verify backend is running:**
```bash
curl http://localhost:8080/api/auth/test
```

You should see: `API is working!`

### Step 2: Start the Frontend

Open a new terminal in the `frontend` directory:

```bash
cd frontend
npm run dev
```

The frontend should start on `http://localhost:5173`

### Step 3: Access the Application

Open your browser and navigate to:
```
http://localhost:5173
```

---

## Testing the Integration

### Test 1: User Registration and Login

1. Navigate to `http://localhost:5173`
2. Click "Don't have an account? Sign up"
3. Enter email and password
4. Complete the sign-up process
5. You should be redirected to the login page
6. Sign in with your credentials

### Test 2: Role-Based Access

**For Admin Users:**
1. Set user's public metadata to `{"role": "ADMIN"}`
2. Login to the application
3. You should be redirected to `/admin`
4. You should have access to admin features

**For Business Users:**
1. Set user's public metadata to `{"role": "BUSINESS"}`
2. Login to the application
3. You should be redirected to `/business`
4. You should have access to business features

### Test 3: API Authentication

Test that the backend receives and validates the JWT token:

```bash
# This should return 401 Unauthorized (no token)
curl http://localhost:8080/api/auth/me

# After logging in the frontend, check the browser console
# You should see authenticated API calls with Bearer token
```

### Test 4: Check Token in Browser

1. Login to the application
2. Open browser Developer Tools (F12)
3. Go to Network tab
4. Make any API request
5. Check the request headers - you should see:
   ```
   Authorization: Bearer eyJhbGc...
   ```

---

## Troubleshooting

### Issue 1: "Missing Clerk Publishable Key" Error

**Cause:** Environment variable not loaded

**Solution:**
1. Ensure `.env.local` exists in the `frontend` directory
2. Verify the variable name is exactly: `VITE_CLERK_PUBLISHABLE_KEY`
3. Restart the frontend development server: `npm run dev`

### Issue 2: 401 Unauthorized on API Calls

**Cause:** JWT token not being sent or invalid

**Solution:**
1. Check browser console for errors
2. Verify the token is being sent in the Authorization header
3. Verify the backend `issuer-uri` matches your Clerk Frontend API URL exactly
4. Ensure the JWK Set URI is accessible: `https://YOUR_CLERK_FRONTEND_API/.well-known/jwks.json`

### Issue 3: User Redirected to Login After Sign In

**Cause:** Role not set in public metadata

**Solution:**
1. Go to Clerk Dashboard → Users
2. Select the user
3. Add role to public metadata as described in [Setting Up User Roles](#setting-up-user-roles)
4. Log out and log back in

### Issue 4: Backend Fails to Start - "Invalid issuer URI"

**Cause:** Incorrect or unreachable Clerk issuer URI

**Solution:**
1. Verify the `issuer-uri` in `application.properties`
2. Ensure it's in the format: `https://your-app-name.clerk.accounts.dev`
3. Test the URL in a browser - it should return JSON
4. Check for typos or extra spaces

### Issue 5: CORS Errors in Browser Console

**Cause:** Frontend origin not allowed by backend

**Solution:**
1. Check `SecurityConfig.java` CORS configuration
2. Ensure `http://localhost:5173` is in the allowed origins
3. If using a different port, update the CORS configuration:
   ```java
   configuration.setAllowedOrigins(Arrays.asList(
       "http://localhost:3000",
       "http://localhost:5173",
       "http://localhost:YOUR_PORT"
   ));
   ```

### Issue 6: Clerk Components Not Rendering

**Cause:** Missing ClerkProvider or incorrect configuration

**Solution:**
1. Verify `main.tsx` has `ClerkProvider` wrapping `<App />`
2. Check that the publishable key is correctly set
3. Clear browser cache and restart development server

### Issue 7: JWT Validation Fails

**Cause:** Clock skew or expired tokens

**Solution:**
1. Ensure system clock is synchronized
2. Check token expiration time
3. Verify the `jwk-set-uri` is accessible from your backend server
4. Add logging to see the exact JWT validation error:
   ```properties
   logging.level.org.springframework.security=DEBUG
   ```

---

## Additional Configuration

### Configure Session Duration

In Clerk Dashboard:
1. Go to **Sessions** settings
2. Adjust session duration as needed
3. Configure multi-session handling

### Configure Authentication Options

In Clerk Dashboard:
1. Go to **User & Authentication** → **Email, Phone, Username**
2. Enable/disable authentication options
3. Configure verification requirements

### Configure Appearance

Customize the Clerk components appearance:
1. Go to **Customization** → **Appearance**
2. Use the visual editor or add custom CSS
3. Match your application's design

---

## Production Deployment Checklist

Before deploying to production:

- [ ] Replace test keys with live keys (both frontend and backend)
- [ ] Update CORS allowed origins to include your production domain
- [ ] Configure production database (replace H2 with PostgreSQL)
- [ ] Set up environment variables securely (don't commit `.env` files)
- [ ] Enable Clerk production mode in dashboard
- [ ] Configure webhook endpoints for user events (optional)
- [ ] Set up monitoring and logging
- [ ] Test all user roles in production environment
- [ ] Configure proper session management and token refresh

---

## Useful Clerk Dashboard Links

- **Users Management:** `https://dashboard.clerk.com/apps/[your-app-id]/users`
- **API Keys:** `https://dashboard.clerk.com/apps/[your-app-id]/api-keys`
- **JWT Templates:** `https://dashboard.clerk.com/apps/[your-app-id]/jwt-templates`
- **Sessions Settings:** `https://dashboard.clerk.com/apps/[your-app-id]/sessions`

---

## Support and Resources

- **Clerk Documentation:** https://clerk.com/docs
- **Clerk React SDK:** https://clerk.com/docs/references/react/overview
- **Spring Security OAuth2:** https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- **Project Repository:** [Your GitHub repository URL]

---

## Summary

You have successfully integrated Clerk authentication into the Session Scheduler application. The authentication flow is:

1. User signs up/signs in via Clerk on the frontend
2. Clerk issues a JWT token to the client
3. Frontend includes the JWT token in API requests
4. Backend validates the JWT using Clerk's JWKS endpoint
5. Backend extracts user roles from the JWT's public metadata
6. Backend enforces role-based access control

For any issues not covered in this guide, please refer to the Clerk documentation or open an issue in the project repository.
