# Tenant Creation with Automatic Clerk User Setup

## Overview

When an admin creates a tenant, the system now automatically:
1. Creates a tenant in the database
2. Creates a business owner user in Clerk with BUSINESS role
3. Creates a BusinessUser record linking the Clerk user to the tenant
4. The business owner can immediately log in with their credentials

## What Changed

### Backend Changes

1. **Created `ClerkUserService`**
   - Integrates with Clerk Backend API
   - Creates users programmatically
   - Sets public metadata with role and tenant_id

2. **Updated `TenantService`**
   - Now accepts owner details in TenantRequest
   - Automatically creates Clerk user during tenant creation
   - Links BusinessUser to the new Clerk account

3. **Updated `TenantRequest` DTO**
   - Added owner fields: `ownerFirstName`, `ownerLastName`, `ownerEmail`, `ownerPassword`

### Frontend Changes

1. **Updated `AdminDashboard`**
   - Added "Business Owner Account" section to tenant creation form
   - Requires owner details when creating a tenant
   - Shows success message with login instructions

## How It Works

### Flow Diagram

```
Admin creates tenant in UI
  ↓
Frontend sends TenantRequest with owner details
  ↓
Backend TenantService.createTenant()
  ↓
1. Create Tenant in database
  ↓
2. Call ClerkUserService.createUser()
  ↓
3. Clerk API creates user with:
   - Email & Password
   - First Name & Last Name
   - Public Metadata: { "role": "BUSINESS", "tenant_id": "uuid" }
  ↓
4. Create BusinessUser record in database
   - Links Clerk user ID to tenant
  ↓
Success! Business owner can log in immediately
```

## Testing the Flow

### Step 1: Restart Backend

```bash
cd backend
mvn spring-boot:run
```

### Step 2: Ensure Frontend is Running

```bash
cd frontend
npm run dev
```

### Step 3: Log in as Admin

1. Go to `http://localhost:5173`
2. Sign in with your admin account
3. Should redirect to `/admin`

### Step 4: Create a New Tenant

1. Click **"+ Add Tenant"** button
2. Fill in **Tenant Information**:
   - Business Name: "Yoga Studio ABC"
   - Slug: "yoga-abc"
   - Email: "contact@yoga-abc.com"
   - Phone: "555-1234"
   - Description: "Premium yoga classes"

3. Fill in **Business Owner Account**:
   - Owner First Name: "Jane"
   - Owner Last Name: "Smith"
   - Owner Email: "jane@yoga-abc.com"
   - Owner Password: "SecurePass123!"

4. Click **"Create Tenant"**

### Step 5: Verify Success

You should see:
- Success message: "Tenant and business owner created successfully! The owner can now log in with their credentials."
- New tenant appears in the table

### Step 6: Test Business Owner Login

1. Log out from admin account
2. Go to `http://localhost:5173`
3. Sign in with:
   - Email: `jane@yoga-abc.com`
   - Password: `SecurePass123!`
4. Should redirect to `/business`
5. Should see "Business Dashboard" with tenant info

## Verification in Clerk Dashboard

### Check User Created

1. Go to [Clerk Dashboard](https://dashboard.clerk.com)
2. Navigate to **Users**
3. Find the newly created user (jane@yoga-abc.com)
4. Check **Public metadata** shows:
```json
{
  "role": "BUSINESS",
  "tenant_id": "generated-uuid"
}
```

### Verify User Can Log In

The user should be able to log in immediately with the credentials provided during tenant creation.

## What Happens Behind the Scenes

### Clerk API Call

```http
POST https://api.clerk.com/v1/users
Authorization: Bearer sk_test_your_secret_key
Content-Type: application/json

{
  "email_address": ["jane@yoga-abc.com"],
  "password": "SecurePass123!",
  "first_name": "Jane",
  "last_name": "Smith",
  "skip_password_checks": true,
  "skip_password_requirement": false,
  "public_metadata": {
    "role": "BUSINESS",
    "tenant_id": "generated-tenant-uuid"
  }
}
```

### Database Records Created

**1. Tenant Table:**
```
id: generated-uuid
name: "Yoga Studio ABC"
slug: "yoga-abc"
email: "contact@yoga-abc.com"
status: "ACTIVE"
...
```

**2. BusinessUser Table:**
```
id: generated-uuid
tenantId: tenant-uuid
email: "jane@yoga-abc.com"
clerkUserId: clerk-generated-user-id
firstName: "Jane"
lastName: "Smith"
role: "OWNER"
isActive: true
```

## Error Handling

### If Clerk User Creation Fails

The system will:
- ✅ Still create the tenant (tenant creation succeeds)
- ⚠️ Log error about user creation failure
- ⚠️ Show warning that admin needs to create user manually
- 📝 Admin can create the user in Clerk Dashboard manually

### Common Errors

**Error: "Email already exists in Clerk"**
- Someone already has that email in your Clerk instance
- Use a different email for the owner

**Error: "Invalid Clerk secret key"**
- Check `clerk.secret-key` in `application.properties`
- Ensure it starts with `sk_test_` or `sk_live_`

**Error: "Password too weak"**
- Ensure password is at least 8 characters
- Consider adding numbers and special characters

## Manual User Creation (Fallback)

If automatic creation fails, admin can manually:

1. Go to Clerk Dashboard → Users → Create User
2. Enter the owner's details
3. Set public metadata:
```json
{
  "role": "BUSINESS",
  "tenant_id": "copy-from-tenant-table"
}
```

## Benefits of This Approach

✅ **One-Step Process**: Create tenant and owner in one action
✅ **Automatic Role Assignment**: No manual metadata editing needed
✅ **Immediate Access**: Owner can log in right away
✅ **Consistent Data**: Tenant and user are properly linked
✅ **Admin Friendly**: Simple form, clear success messages

## Security Notes

### Password Handling

- Passwords are sent directly to Clerk API
- Never stored in your database
- Clerk handles password hashing and security
- Uses HTTPS for API communication

### API Key Security

- Clerk secret key is stored in `application.properties`
- Never exposed to frontend
- Only used in backend server-to-server calls
- Keep it secret, keep it safe!

## Troubleshooting

### Issue: "Failed to authenticate since the JWT was invalid"

**This was the original issue - now fixed!**

**Cause**: Backend was trying to create tenant without Clerk integration

**Solution**: Now uses Clerk Backend API to create authenticated users

### Issue: Tenant created but owner can't log in

**Check:**
1. Verify user exists in Clerk Dashboard
2. Check public_metadata has correct role
3. Ensure tenant_id matches
4. Try resetting password in Clerk Dashboard

### Issue: "CORS error" when creating tenant

**Solution:**
- Check backend logs
- Verify CORS configuration in SecurityConfig
- Ensure frontend URL is in allowed origins

## Next Steps

### For Production

1. **Use Strong Passwords**: Enforce password requirements in UI
2. **Email Verification**: Enable in Clerk Dashboard settings
3. **Welcome Emails**: Configure Clerk to send welcome emails
4. **Password Reset**: Clerk handles this automatically

### Optional Enhancements

- Send welcome email to new business owner
- Generate random secure password and email it
- Add owner phone number support
- Allow multiple owners per tenant

---

## Summary

The system now provides a seamless tenant creation experience where:
- Admins create tenants with owner details in one form
- Clerk users are automatically created with correct roles
- Business owners can log in immediately
- No manual Clerk Dashboard configuration needed

This eliminates the previous workflow where you had to manually create users in Clerk and set their metadata separately.
