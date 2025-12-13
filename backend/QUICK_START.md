# 🚀 Quick Start - R2 Configuration

## Where to Put R2 Configuration

### ✅ **Answer: In the `.env` file**

**Location**: `/Users/chitranshi/projects/one_day/Scheduler/backend/.env`

## 📝 Configuration Steps

### 1. Get Cloudflare R2 Credentials

Follow the detailed guide: [R2_SETUP_GUIDE.md](./R2_SETUP_GUIDE.md)

**You need:**
- Access Key ID
- Secret Access Key
- Account ID
- Bucket Name
- Public URL

### 2. Update `.env` File

Open `backend/.env` and replace these values:

```bash
# Cloudflare R2 Configuration
export R2_ACCESS_KEY_ID=your-actual-access-key-here        # ← Replace this
export R2_SECRET_ACCESS_KEY=your-actual-secret-key-here    # ← Replace this
export R2_ACCOUNT_ID=your-actual-account-id-here           # ← Replace this
export R2_BUCKET_NAME=scheduler-profile-images             # ← Or your bucket name
export R2_PUBLIC_URL=https://pub-your-hash.r2.dev          # ← Replace with actual URL
```

### 3. Run the Application

**Option A: Using the helper script (easiest)**

```bash
cd /Users/chitranshi/projects/one_day/Scheduler/backend
./run-dev.sh
```

This automatically loads `.env` and starts the app.

**Option B: Manual way**

```bash
cd /Users/chitranshi/projects/one_day/Scheduler/backend
source .env
mvn spring-boot:run
```

## 🧪 Test It Works

1. Start the backend (see above)
2. Start the frontend:
   ```bash
   cd /Users/chitranshi/projects/one_day/Scheduler/frontend
   npm run dev
   ```
3. Login as a business user
4. Go to Business Dashboard → Business Profile
5. Upload an image
6. Should see ✅ success message

## ⚠️ Common Mistakes

❌ **Don't put credentials in `application.properties`**
✅ **Use `.env` file instead** (already ignored by git)

❌ **Don't commit `.env` to git**
✅ **It's already in `.gitignore`** - you're safe

❌ **Don't forget to run `source .env`**
✅ **Or use `./run-dev.sh`** which does it for you

## 🏗️ Production Deployment

For production (Heroku, AWS, etc.), set these as **environment variables** in your hosting platform:

- `R2_ACCESS_KEY_ID`
- `R2_SECRET_ACCESS_KEY`
- `R2_ACCOUNT_ID`
- `R2_BUCKET_NAME`
- `R2_PUBLIC_URL`

**Don't use the `.env` file in production** - use the platform's environment variable settings.

## 📚 More Help

- Full setup guide: [R2_SETUP_GUIDE.md](./R2_SETUP_GUIDE.md)
- Cloudflare R2 Docs: https://developers.cloudflare.com/r2/

## 💡 Summary

```
Configuration File: backend/.env
How to Run: ./run-dev.sh
Is it safe? Yes (.env is in .gitignore)
Cost? $0 (free tier)
```

That's it! 🎉
