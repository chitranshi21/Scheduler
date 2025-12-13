# Cloudflare R2 Setup Guide

This guide will help you set up Cloudflare R2 for image storage in the Session Scheduler application.

## 📋 Prerequisites

- A Cloudflare account (free tier is sufficient)
- Access to Cloudflare Dashboard

## 🚀 Step-by-Step Setup

### Step 1: Create Cloudflare Account (if needed)

1. Go to https://dash.cloudflare.com/sign-up
2. Sign up with your email
3. Verify your email address

### Step 2: Create R2 Bucket

1. **Navigate to R2**:
   - Login to Cloudflare Dashboard
   - Click **"R2"** in the left sidebar
   - If prompted, click **"Begin setup"** or **"Purchase R2"** (it's free for the tier we need)

2. **Create Bucket**:
   - Click **"Create bucket"**
   - Enter bucket name: `scheduler-profile-images` (or your preferred name)
   - Choose location: **Automatic** (recommended)
   - Click **"Create bucket"**

3. **Enable Public Access**:
   - Click on your newly created bucket
   - Go to **"Settings"** tab
   - Scroll to **"Public access"** section
   - Click **"Allow Access"** or **"Connect Domain"**
   - If using default R2.dev domain:
     - Click **"Allow Access"**
     - Note the public URL: `https://pub-{hash}.r2.dev`
   - **Copy this URL** - you'll need it for configuration

### Step 3: Generate API Tokens

1. **Go back to R2 Overview**:
   - Click "R2" in the sidebar to return to R2 home

2. **Manage API Tokens**:
   - Click **"Manage R2 API Tokens"** (top right)

3. **Create API Token**:
   - Click **"Create API Token"**
   - **Token name**: "Scheduler App Token" (or your preferred name)
   - **Permissions**: Select **"Object Read & Write"**
   - **Bucket**:
     - Select "Apply to specific buckets only"
     - Choose `scheduler-profile-images`
   - **TTL (Time to Live)**: Leave as default (Forever) or set expiration
   - Click **"Create API Token"**

4. **Save Credentials**:
   ```
   ⚠️ IMPORTANT: These credentials will only be shown ONCE!
   Copy them immediately and save them securely.
   ```
   - **Access Key ID**: `xxxxxxxxxxxxxxxxxxxxxxxx`
   - **Secret Access Key**: `yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy`
   - Also note your **Account ID** (shown at the top of the page)

### Step 4: Find Your Account ID

1. On the R2 API Tokens page, look at the top
2. You'll see: **"Account ID: abc123def456"**
3. **Copy this Account ID**

### Step 5: Configure Environment Variables

1. **Open the `.env` file** in the `backend/` directory

2. **Replace the placeholder values** with your actual credentials:

   ```bash
   # Cloudflare R2 Configuration
   export R2_ACCESS_KEY_ID=your-actual-access-key-id-here
   export R2_SECRET_ACCESS_KEY=your-actual-secret-access-key-here
   export R2_ACCOUNT_ID=your-actual-account-id-here
   export R2_BUCKET_NAME=scheduler-profile-images
   export R2_PUBLIC_URL=https://pub-your-actual-hash.r2.dev
   ```

3. **Example** (with fake values):
   ```bash
   export R2_ACCESS_KEY_ID=a1b2c3d4e5f6g7h8i9j0
   export R2_SECRET_ACCESS_KEY=X1Y2Z3A4B5C6D7E8F9G0H1I2J3K4L5M6N7O8P9Q0
   export R2_ACCOUNT_ID=abc123def456ghi789
   export R2_BUCKET_NAME=scheduler-profile-images
   export R2_PUBLIC_URL=https://pub-e8f9a0b1c2d3e4f5.r2.dev
   ```

4. **Save the file**

### Step 6: Load Environment Variables

Before running the application, you need to load the environment variables.

**On macOS/Linux:**

```bash
cd /Users/chitranshi/projects/one_day/Scheduler/backend
source .env
```

**On Windows (PowerShell):**

```powershell
cd C:\path\to\Scheduler\backend
Get-Content .env | ForEach-Object {
    if ($_ -match '^export\s+(.+?)=(.+)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2])
    }
}
```

### Step 7: Run the Application

```bash
# Make sure you're in the backend directory
cd /Users/chitranshi/projects/one_day/Scheduler/backend

# Load environment variables
source .env

# Run the application
mvn spring-boot:run
```

### Step 8: Test the Upload

1. **Start frontend** (in another terminal):
   ```bash
   cd /Users/chitranshi/projects/one_day/Scheduler/frontend
   npm run dev
   ```

2. **Login as a business user**
3. **Navigate to Business Dashboard**
4. **Scroll to "Business Profile" section**
5. **Upload an image**:
   - Drag and drop an image onto the upload area, OR
   - Click the upload area to select a file
6. **Verify**:
   - Image preview should appear
   - Check the browser console for success message
   - Visit your public booking page to see the logo

## 🔍 Verification Checklist

- [ ] Cloudflare account created
- [ ] R2 bucket created and named `scheduler-profile-images`
- [ ] Public access enabled on the bucket
- [ ] R2 API token created with Read & Write permissions
- [ ] All 5 environment variables configured in `.env`
- [ ] Environment variables loaded (`source .env`)
- [ ] Application starts without R2-related errors
- [ ] Image upload works in Business Dashboard
- [ ] Logo displays on public booking page

## ❌ Troubleshooting

### Error: "Access Denied" when uploading

**Cause**: API token doesn't have proper permissions

**Fix**:
1. Go to Cloudflare Dashboard → R2 → Manage API Tokens
2. Delete the old token
3. Create a new token with **"Object Read & Write"** permissions
4. Update `.env` with the new credentials
5. Restart the application

### Error: "Bucket not found"

**Cause**: Bucket name mismatch

**Fix**:
1. Check the bucket name in Cloudflare Dashboard
2. Update `R2_BUCKET_NAME` in `.env` to match exactly
3. Restart the application

### Error: "Invalid endpoint"

**Cause**: Account ID is incorrect

**Fix**:
1. Go to Cloudflare Dashboard → R2 → Manage API Tokens
2. Copy the Account ID shown at the top
3. Update `R2_ACCOUNT_ID` in `.env`
4. Restart the application

### Images upload but don't display

**Cause**: Public URL is incorrect or public access not enabled

**Fix**:
1. Go to your bucket → Settings → Public access
2. Ensure public access is enabled
3. Copy the public URL (e.g., `https://pub-abc123.r2.dev`)
4. Update `R2_PUBLIC_URL` in `.env` with the correct URL (no trailing slash)
5. Restart the application

### Environment variables not loading

**Cause**: `.env` file not sourced

**Fix**:
```bash
# Make sure you run this before starting the app
source .env

# Verify variables are set
echo $R2_ACCESS_KEY_ID
# Should print your access key

# If empty, the source command didn't work
# Try running in the same terminal session where you'll run mvn
```

## 🔒 Security Notes

- ✅ **`.env` is in `.gitignore`** - Your credentials won't be committed to git
- ✅ **Never share your `.env` file** or commit it to version control
- ✅ **Rotate tokens periodically** for better security
- ✅ **Use separate tokens** for development and production

## 💰 Cost Information

**R2 Free Tier (Forever Free)**:
- **Storage**: 10 GB/month
- **Class A Operations** (writes): 1 million/month
- **Class B Operations** (reads): 10 million/month
- **Egress**: **UNLIMITED** (zero fees)

**Estimated Usage for Scheduler App**:
- 1,000 businesses with logos (~500 KB each) = **~500 MB**
- Well within free tier limits
- **Monthly cost**: **$0**

## 📚 Additional Resources

- [Cloudflare R2 Documentation](https://developers.cloudflare.com/r2/)
- [R2 Pricing](https://developers.cloudflare.com/r2/pricing/)
- [R2 API Reference](https://developers.cloudflare.com/r2/api/s3/)

## 🆘 Need Help?

If you encounter issues not covered here:
1. Check the application logs for detailed error messages
2. Verify all environment variables are set correctly: `env | grep R2`
3. Test R2 connection using the AWS CLI (optional):
   ```bash
   aws s3 ls --endpoint-url https://<account-id>.r2.cloudflarestorage.com
   ```
