# Complete Tenant Creation Testing Guide

## ✅ System Status

The system is **fully configured** to automatically create Clerk users when creating tenants.

### What's Implemented

1. **ClerkUserService** - Integrates with Clerk Backend API
2. **TenantService** - Automatically creates Clerk users with BUSINESS role
3. **AdminDashboard UI** - Includes owner fields in tenant creation form
4. **JWT Authentication** - Validates admin role before allowing tenant creation

---

## Step-by-Step Testing Instructions

### Prerequisites Check

**Backend Running:**
```bash
# Check if running on port 8080
lsof -i :8080
```
✅ Should show Java process

**Frontend Running:**
```bash
# Check if running on port 5173
lsof -i :5173
```
✅ Should show node process

**Clerk Configuration:**
- ✅ `clerk.secret-key` set in application.properties
- ✅ `VITE_CLERK_PUBLISHABLE_KEY` set in frontend/.env.local
- ✅ Your admin user has `{"role": "ADMIN"}` in Clerk public_metadata

---

## Test Scenario: Create a New Business Tenant

### Step 1: Verify You're Logged In as Admin

1. Open browser: `http://localhost:5173`
2. Open DevTools (F12) → Console tab
3. Check you see: "Admin Dashboard"
4. Check you're on path: `/admin`

**If not:**
- Log out and log back in
- Verify your user has `{"role": "ADMIN"}` in Clerk Dashboard

### Step 2: Open Tenant Creation Modal

1. Click **"+ Add Tenant"** button
2. Modal should open with form

### Step 3: Fill in Tenant Information

**Business Details:**
```
Business Name: Sunrise Yoga Studio
Slug: sunrise-yoga
Email: contact@sunriseyoga.com
Phone: 555-0100
Description: Morning yoga classes for all levels
```

**Business Owner Account (This creates the Clerk user!):**
```
Owner First Name: Michael
Owner Last Name: Chen
Owner Email: michael@sunriseyoga.com
Owner Password: YogaPass123!
```

⚠️ **Important:** Password must be at least 8 characters

### Step 4: Submit Form

1. Click **"Create Tenant"** button
2. Watch the console for debug output

**Expected Console Output:**
```
🔐 API Interceptor - Getting token...
🔐 Token retrieved: Yes ✓
🔐 Authorization header set
📝 Creating tenant with data: {...}
✅ Tenant created successfully: {...}
```

**Expected Alert:**
```
Tenant and business owner created successfully!
The owner can now log in with their credentials.
```

### Step 5: Verify in Backend Logs

```bash
tail -100 /tmp/backend.log | grep -A 20 "CLERK JWT DEBUG"
```

**Expected Output:**
```
=== CLERK JWT DEBUG ===
User: your-admin-email@example.com
Public Metadata: {role=ADMIN}
Extracted Role: ADMIN
Granted Authority: ROLE_ADMIN
Final Authorities: [ROLE_ADMIN, ROLE_USER]
======================
```

Then:
```bash
tail -100 /tmp/backend.log | grep -A 5 "Creating Clerk user"
```

**Expected Output:**
```
Creating Clerk user with email: michael@sunriseyoga.com
Successfully created Clerk user with ID: user_xxxxxxxxxxxxx
Created BusinessUser record for tenant: xxxxx-xxxx-xxxx
```

### Step 6: Verify Tenant Appears in Table

The new tenant should appear in the admin dashboard table:
- Name: "Sunrise Yoga Studio"
- Slug: "sunrise-yoga"
- Status: ACTIVE
- Subscription: BASIC

### Step 7: Verify User Created in Clerk Dashboard

1. Go to [Clerk Dashboard](https://dashboard.clerk.com)
2. Navigate to **Users**
3. Find user: `michael@sunriseyoga.com`
4. Click on the user
5. Check **Public metadata**:

**Expected:**
```json
{
  "role": "BUSINESS",
  "tenant_id": "<generated-uuid>"
}
```

### Step 8: Test Business Owner Login

**This is the critical test!**

1. **Log out** from admin account
2. Go to login page
3. Sign in with:
   - Email: `michael@sunriseyoga.com`
   - Password: `YogaPass123!`
4. Should redirect to `/business`
5. Should see "Sunrise Yoga Studio" dashboard
6. Should see booking link: `http://localhost:5173/book/sunrise-yoga`

✅ **SUCCESS!** Business owner can log in immediately.

---

## Troubleshooting

### Issue 1: 403 Forbidden Error

**Symptom:** Error creating tenant, status 403

**Console Shows:**
```
❌ Error creating tenant: Error: Request failed with status code 403
Error status: 403
```

**Cause:** Your admin user doesn't have ADMIN role

**Solution:**
1. Go to Clerk Dashboard → Users
2. Find your admin user
3. Edit Public metadata:
```json
{
  "role": "ADMIN"
}
```
4. Save
5. **Log out and log back in** (critical!)
6. Try again

### Issue 2: No Token Available

**Console Shows:**
```
⚠️ No token available
```

**Cause:** Not logged in or token expired

**Solution:**
1. Hard refresh browser (Ctrl+Shift+R or Cmd+Shift+R)
2. Log out and log back in
3. Check you're on `/admin` path

### Issue 3: Backend Logs Show "No role found"

**Backend Shows:**
```
Public Metadata: null
No role found in public_metadata - defaulting to CUSTOMER
```

**Cause:** Role not set in Clerk or haven't logged in after setting it

**Solution:**
1. Verify role is set in Clerk Dashboard
2. **Must log out and log back in** to get new JWT token
3. Old token is cached and doesn't have the role

### Issue 4: Clerk User Creation Fails

**Backend Shows:**
```
Error creating user in Clerk: 401 Unauthorized
```

**Cause:** Invalid Clerk secret key

**Solution:**
1. Check `clerk.secret-key` in `backend/src/main/resources/application.properties`
2. Should start with `sk_test_` or `sk_live_`
3. Get from Clerk Dashboard → API Keys
4. Restart backend after updating

### Issue 5: "Invalid email" or "Email exists"

**Backend Shows:**
```
Error creating user in Clerk: Email already exists
```

**Cause:** Owner email already exists in Clerk

**Solution:**
- Use a different email for the owner
- Or delete the existing user from Clerk Dashboard first

---

## Verification Checklist

After creating a tenant, verify:

- [ ] Tenant appears in admin dashboard table
- [ ] No errors in browser console
- [ ] Backend logs show "Successfully created Clerk user"
- [ ] Backend logs show "Created BusinessUser record"
- [ ] User exists in Clerk Dashboard
- [ ] User has `{"role": "BUSINESS", "tenant_id": "..."}` in public_metadata
- [ ] Business owner can log in with provided credentials
- [ ] Business owner sees their dashboard at `/business`
- [ ] Business owner sees their tenant name
- [ ] Business owner sees their booking link

---

## What Happens Behind the Scenes

```
Admin fills form and clicks Create
         ↓
Frontend sends POST /api/admin/tenants with:
{
  name: "Sunrise Yoga Studio",
  slug: "sunrise-yoga",
  email: "contact@sunriseyoga.com",
  ownerFirstName: "Michael",
  ownerLastName: "Chen",
  ownerEmail: "michael@sunriseyoga.com",
  ownerPassword: "YogaPass123!"
}
         ↓
Backend validates admin has ROLE_ADMIN
         ↓
[1] TenantService.createTenant()
    - Creates tenant in database
    - tenant.id = "uuid-generated"
         ↓
[2] ClerkUserService.createUser()
    - Calls Clerk API: POST /v1/users
    - Payload:
      {
        email_address: ["michael@sunriseyoga.com"],
        password: "YogaPass123!",
        first_name: "Michael",
        last_name: "Chen",
        public_metadata: {
          role: "BUSINESS",
          tenant_id: "uuid-generated"
        }
      }
    - Returns: user_xxxxx (Clerk user ID)
         ↓
[3] Create BusinessUser record
    - Links Clerk user ID to tenant ID
    - Stores in database
         ↓
Success! Returns tenant to frontend
         ↓
Frontend shows success alert
         ↓
Owner can immediately log in at /login
```

---

## Expected Results Summary

### In Database:

**Tenants table:**
```sql
id: <uuid>
name: "Sunrise Yoga Studio"
slug: "sunrise-yoga"
email: "contact@sunriseyoga.com"
status: "ACTIVE"
```

**BusinessUsers table:**
```sql
id: <uuid>
tenant_id: <tenant-uuid>
email: "michael@sunriseyoga.com"
clerk_user_id: "user_xxxxx"
first_name: "Michael"
last_name: "Chen"
role: "OWNER"
is_active: true
```

### In Clerk:

**User:** michael@sunriseyoga.com
```json
{
  "id": "user_xxxxx",
  "email_addresses": ["michael@sunriseyoga.com"],
  "first_name": "Michael",
  "last_name": "Chen",
  "public_metadata": {
    "role": "BUSINESS",
    "tenant_id": "<tenant-uuid>"
  }
}
```

---

## Quick Test Command

After attempting to create a tenant, run:

```bash
# Check if backend received the request
tail -200 /tmp/backend.log | grep -E "(POST|createTenant|Clerk user|BusinessUser)" | tail -20

# Check for any errors
tail -200 /tmp/backend.log | grep -E "(Error|Exception|Failed)" | tail -10

# Check JWT debug output
tail -200 /tmp/backend.log | grep -A 10 "CLERK JWT DEBUG" | tail -15
```

---

## Success Criteria

✅ Admin can create tenant with owner details in one form
✅ Clerk user created automatically with BUSINESS role
✅ BusinessUser record created linking user to tenant
✅ Owner can log in immediately after creation
✅ Owner sees business dashboard with correct tenant
✅ No manual Clerk Dashboard configuration needed

---

## If Everything Works

You should be able to:
1. Create multiple tenants with different owners
2. Each owner gets their own Clerk account
3. Each owner can log in independently
4. Each owner only sees their own tenant's data
5. Admin can see all tenants

This is the complete multi-tenant SaaS flow! 🎉
