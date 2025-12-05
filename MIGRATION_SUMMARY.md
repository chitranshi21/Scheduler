# Supabase to Clerk Migration Summary

This document summarizes the changes made during the migration from Supabase to Clerk authentication.

## Files Modified

### Backend

1. **pom.xml**
   - No dependency changes needed (Spring Security OAuth2 Resource Server works with both)
   - Updated comment to reference Clerk

2. **SecurityConfig.java**
   - Changed from `SupabaseJwtAuthenticationConverter` to `ClerkJwtAuthenticationConverter`
   - Updated documentation comments

3. **AuthController.java**
   - Changed helper method calls from `SupabaseJwtAuthenticationConverter` to `ClerkJwtAuthenticationConverter`
   - Updated field names (supabaseUserId → clerkUserId)
   - Added firstName and lastName to response

4. **application.properties**
   - Changed JWT issuer URI to Clerk's endpoint
   - Changed JWK Set URI to Clerk's endpoint
   - Replaced Supabase configuration with Clerk configuration
   - Updated with your Clerk credentials

5. **DataInitializer.java**
   - Updated comments from Supabase to Clerk
   - Updated log messages

6. **TenantService.java**
   - Updated comments from Supabase to Clerk

7. **Model Files (Admin.java, BusinessUser.java, Customer.java)**
   - Changed field name: `supabaseUserId` → `clerkUserId`

### Files Created

1. **ClerkJwtAuthenticationConverter.java**
   - New JWT converter that extracts roles from Clerk's `public_metadata`
   - Provides helper methods for extracting user information

### Files Deleted

1. **SupabaseUserService.java** - No longer needed
2. **SupabaseJwtAuthenticationConverter.java** - Replaced with ClerkJwtAuthenticationConverter
3. **SUPABASE_INTEGRATION_GUIDE.md** - Replaced with CLERK_SETUP_GUIDE.md

### Frontend

1. **package.json**
   - Added `@clerk/clerk-react` dependency

2. **main.tsx**
   - Wrapped app with `ClerkProvider`
   - Added environment variable for Clerk publishable key

3. **App.tsx**
   - Replaced custom `AuthContext` with Clerk's `useUser` and `useAuth` hooks
   - Updated routing logic
   - Integrated Clerk token with API service

4. **Login.tsx**
   - Completely replaced custom login form with Clerk's `SignIn` and `SignUp` components
   - Added role setup instructions

5. **api.ts**
   - Removed custom token storage
   - Added `setupApiAuth` function to inject Clerk's `getToken` function
   - Updated auth API endpoints

6. **.env.example**
   - Created template for Clerk publishable key

### Files Deleted

1. **AuthContext.tsx** - Replaced with Clerk's built-in auth management

---

## Key Differences: Supabase vs Clerk

### JWT Token Claims

**Supabase:**
- Roles stored in: `user_metadata.role`
- User ID claim: `sub`
- Email claim: `email`

**Clerk:**
- Roles stored in: `public_metadata.role`
- User ID claim: `sub`
- Email claim: `email`
- First name: `given_name`
- Last name: `family_name`

### Database Field Changes

| Old (Supabase) | New (Clerk) |
|----------------|-------------|
| `supabaseUserId` | `clerkUserId` |

### Configuration Changes

**Supabase:**
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://PROJECT_ID.supabase.co/auth/v1
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://PROJECT_ID.supabase.co/auth/v1/jwks
supabase.url=...
supabase.anon-key=...
```

**Clerk:**
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://api.clerk.com
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://YOUR_APP.clerk.accounts.dev/.well-known/jwks.json
clerk.publishable-key=pk_test_...
clerk.secret-key=sk_test_...
```

---

## Verification Checklist

- [x] Backend compiles successfully
- [x] All Supabase references removed
- [x] Clerk configuration in place
- [x] Frontend dependencies updated
- [x] JWT token extraction updated
- [x] API authentication updated
- [x] Database models updated (supabaseUserId → clerkUserId)
- [x] Documentation updated

---

## Next Steps

1. **Install frontend dependencies:**
   ```bash
   cd frontend
   npm install
   ```

2. **Create `.env.local` in frontend:**
   ```env
   VITE_CLERK_PUBLISHABLE_KEY=pk_test_your_key
   ```

3. **Start the application:**
   ```bash
   # Terminal 1 - Backend
   cd backend
   mvn spring-boot:run

   # Terminal 2 - Frontend
   cd frontend
   npm run dev
   ```

4. **Set up users in Clerk:**
   - Create users in Clerk Dashboard
   - Add public metadata: `{ "role": "ADMIN" }` or `{ "role": "BUSINESS" }` or `{ "role": "CUSTOMER" }`

---

## Notes

- The migration is complete and the application should work identically to before
- All authentication is now handled by Clerk instead of Supabase
- The backend validates JWT tokens from Clerk
- User roles are extracted from `public_metadata` instead of `user_metadata`
- Database schema change: `supabaseUserId` columns are now `clerkUserId`
  - **Important:** If you have existing data, you'll need to migrate or drop/recreate tables

---

For detailed setup instructions, see [CLERK_SETUP_GUIDE.md](./CLERK_SETUP_GUIDE.md)
