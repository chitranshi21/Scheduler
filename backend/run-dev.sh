#!/bin/bash

# Load environment variables from .env file
if [ -f .env ]; then
    echo "📦 Loading environment variables from .env..."
    source .env
    echo "✅ Environment variables loaded"
else
    echo "❌ Error: .env file not found!"
    echo "Please create a .env file with your configuration."
    echo "See R2_SETUP_GUIDE.md for instructions."
    exit 1
fi

# Verify R2 configuration
echo ""
echo "🔍 Verifying R2 configuration..."
if [ -z "$R2_ACCESS_KEY_ID" ] || [ "$R2_ACCESS_KEY_ID" = "your-r2-access-key-id-here" ]; then
    echo "⚠️  WARNING: R2_ACCESS_KEY_ID not configured"
    echo "   Please update .env with your Cloudflare R2 credentials"
    echo "   See R2_SETUP_GUIDE.md for instructions"
fi

if [ -z "$R2_ACCOUNT_ID" ] || [ "$R2_ACCOUNT_ID" = "your-cloudflare-account-id-here" ]; then
    echo "⚠️  WARNING: R2_ACCOUNT_ID not configured"
fi

if [ -z "$R2_BUCKET_NAME" ]; then
    echo "⚠️  WARNING: R2_BUCKET_NAME not configured"
fi

if [ -z "$R2_PUBLIC_URL" ] || [ "$R2_PUBLIC_URL" = "https://pub-your-hash.r2.dev" ]; then
    echo "⚠️  WARNING: R2_PUBLIC_URL not configured"
fi

echo ""
echo "🚀 Starting Spring Boot application..."
echo ""

# Run the application
mvn spring-boot:run
