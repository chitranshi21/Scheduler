# Video Meeting Integration Guide

## Overview
This document outlines approaches for integrating video conferencing (Google Meet, Zoom, Microsoft Teams, etc.) into the Session Scheduler application.

## Google Meet Integration (Recommended - Simplest)

### Overview
Google Meet is the simplest to integrate as it doesn't require API setup. Business users can create a reusable Google Meet link and add it to their session types.

### Setup Steps

#### Step 1: Create Google Meet Link
1. Go to [Google Meet](https://meet.google.com/)
2. Click "New meeting" → "Create a meeting for later"
3. Copy the meeting link (e.g., `https://meet.google.com/abc-defg-hij`)
4. This link can be reused for all sessions of that type

#### Step 2: Add to Session Type
1. Business user logs into admin panel
2. Creates or edits a session type
3. Pastes the Google Meet link in the "Meeting Link" field
4. No password needed (Google Meet handles authentication)

#### Step 3: Email Integration (Already Implemented)
- Customer receives email with "Join Google Meet" button
- Business receives email with "Start Google Meet" button
- Both can join using the same link

### Benefits
- ✅ No API setup required
- ✅ Works with free Google accounts
- ✅ Instant setup (< 2 minutes)
- ✅ Reliable and widely used
- ✅ No rate limits
- ✅ Same link can be reused or created uniquely per session

### Best Practices
1. **Reusable Link**: Create one Google Meet link per session type for simplicity
2. **Unique Links**: Create unique links for each session if you prefer isolation
3. **Host Controls**: The business user (meeting creator) has host controls
4. **Security**: Enable "Host management" in Google Meet settings to control who can join
5. **Calendar Integration**: The ICS file in the email includes the Google Meet link

### Creating Unique Google Meet Links (Optional Advanced Setup)

If you want unique meeting links per booking, you can use Google Calendar API:

1. **Setup Google Calendar API**:
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Create a project
   - Enable Google Calendar API
   - Create OAuth 2.0 credentials (Service Account)

2. **Implementation** (similar to Zoom API approach):
   - Store Google service account credentials
   - When booking is created, create a Calendar event with Google Meet
   - Extract the Google Meet link from the event
   - Store in booking record

This is more complex but provides unique links per session. For most use cases, reusable links are sufficient.

---

## Zoom Integration

## Option 1: Full Zoom API Integration (Automated)

### Prerequisites
1. **Zoom Account**: Pro, Business, or Enterprise account
2. **Zoom Marketplace App**: Create a Server-to-Server OAuth app
3. **Required Credentials**:
   - Client ID
   - Client Secret
   - Account ID

### Setup Steps

#### Step 1: Create Zoom Server-to-Server OAuth App
1. Go to [Zoom Marketplace](https://marketplace.zoom.us/)
2. Click "Develop" → "Build App"
3. Select "Server-to-Server OAuth"
4. Fill in app details:
   - App Name: "Session Scheduler"
   - Company Name: Your company
   - Developer Email: Your email
5. Add required scopes:
   - `meeting:write:admin` - Create meetings
   - `meeting:read:admin` - Read meeting details
   - `user:read:admin` - Read user information
6. Copy credentials:
   - Account ID
   - Client ID
   - Client Secret

#### Step 2: Add Environment Variables
Add to `.env` file:
```bash
# Zoom API Configuration
ZOOM_ACCOUNT_ID=your_account_id_here
ZOOM_CLIENT_ID=your_client_id_here
ZOOM_CLIENT_SECRET=your_client_secret_here
ZOOM_ENABLED=true
```

#### Step 3: Add Zoom Dependencies
Add to `pom.xml`:
```xml
<!-- Zoom API -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

#### Step 4: Implementation Architecture

**Database Changes:**
```sql
-- Add zoom meeting fields to bookings table
ALTER TABLE bookings ADD COLUMN zoom_meeting_id VARCHAR(255);
ALTER TABLE bookings ADD COLUMN zoom_join_url TEXT;
ALTER TABLE bookings ADD COLUMN zoom_host_url TEXT;
ALTER TABLE bookings ADD COLUMN zoom_meeting_password VARCHAR(50);
```

**Classes to Create:**

1. **ZoomConfig.java** - Configuration for Zoom API credentials
2. **ZoomService.java** - Service to interact with Zoom API
3. **ZoomMeetingResponse.java** - DTO for Zoom API responses

**API Workflow:**
```
1. User creates booking
   ↓
2. BookingService calls ZoomService.createMeeting()
   ↓
3. ZoomService:
   - Gets OAuth token from Zoom
   - Creates meeting via Zoom API
   - Returns meeting details (join URL, meeting ID, password)
   ↓
4. Store meeting details in booking record
   ↓
5. Include Zoom link in email and calendar invite
```

### Zoom API Endpoints

**Get Access Token:**
```
POST https://zoom.us/oauth/token
Authorization: Basic base64(CLIENT_ID:CLIENT_SECRET)
Body: grant_type=account_credentials&account_id=ACCOUNT_ID
```

**Create Meeting:**
```
POST https://api.zoom.us/v2/users/me/meetings
Authorization: Bearer ACCESS_TOKEN
Body:
{
  "topic": "Session Name with Customer",
  "type": 2,
  "start_time": "2025-12-08T10:00:00Z",
  "duration": 60,
  "timezone": "America/New_York",
  "settings": {
    "host_video": true,
    "participant_video": true,
    "join_before_host": false,
    "mute_upon_entry": true,
    "waiting_room": true,
    "audio": "both",
    "auto_recording": "none"
  }
}
```

**Response:**
```json
{
  "id": 123456789,
  "join_url": "https://zoom.us/j/123456789?pwd=...",
  "start_url": "https://zoom.us/s/123456789?...",
  "password": "abc123"
}
```

### Benefits
- ✅ Unique meeting for each booking
- ✅ Automatic meeting creation
- ✅ Meeting passwords for security
- ✅ Waiting room control
- ✅ Automatic cleanup

### Drawbacks
- ❌ Requires Zoom developer account
- ❌ API rate limits
- ❌ More complex implementation
- ❌ Requires Pro account or higher

---

## Option 2: Manual Zoom Link Configuration (Recommended for Quick Start)

### Overview
Business users configure a Zoom meeting link per session type or use a Personal Meeting Room (PMR) link.

### Prerequisites
1. Basic Zoom account (free or paid)
2. Personal Meeting Room or recurring meeting link

### Setup Steps

#### Step 1: Database Changes
```sql
-- Add meeting_link to session_types table
ALTER TABLE session_types ADD COLUMN meeting_link VARCHAR(500);
ALTER TABLE session_types ADD COLUMN meeting_password VARCHAR(50);
```

#### Step 2: Update SessionType Model
Add fields:
```java
private String meetingLink;    // Zoom link
private String meetingPassword; // Optional password
```

#### Step 3: Business User Configuration
1. Business users create a Zoom meeting or use their PMR
2. Copy the meeting link (e.g., https://zoom.us/j/1234567890)
3. Add link to session type in admin panel
4. Optionally add meeting password

#### Step 4: Email Integration
- Include Zoom link in booking confirmation emails
- Add to calendar invite description
- Display in booking details

### Benefits
- ✅ Simple implementation
- ✅ Works with any Zoom account
- ✅ No API integration needed
- ✅ Immediate deployment
- ✅ No rate limits

### Drawbacks
- ❌ Same link for all sessions of that type
- ❌ Manual configuration required
- ❌ Less automated

---

## Recommended Approach

**For MVP/Quick Launch:** Use **Option 2** (Manual Configuration)
- Faster implementation
- No external dependencies
- Works with any Zoom account
- Sufficient for most use cases

**For Scale/Production:** Migrate to **Option 1** (Full API)
- Better security (unique meetings)
- Automated workflow
- Better customer experience
- More control over meetings

---

## Implementation Timeline

### Phase 1: Manual Configuration (1-2 hours)
1. Update database schema
2. Update SessionType model
3. Update email templates
4. Update admin UI to configure links

### Phase 2: Full API Integration (1-2 days)
1. Register Zoom app
2. Implement OAuth flow
3. Create ZoomService
4. Update booking flow
5. Testing and error handling

---

## Security Best Practices

### For Manual Links:
- Use waiting rooms
- Require passwords
- Enable waiting room for all participants
- Use meeting registration if needed

### For API Integration:
- Store credentials in environment variables
- Rotate access tokens regularly
- Log all API calls
- Handle rate limits gracefully
- Implement retry logic

---

## Testing

### Manual Configuration Testing:
1. Configure Zoom link on session type
2. Create booking
3. Verify link in email
4. Test joining meeting

### API Integration Testing:
1. Test OAuth token generation
2. Test meeting creation
3. Test error scenarios (API down, rate limit)
4. Test meeting cleanup
5. Load testing

---

## Monitoring

### Key Metrics:
- Meeting creation success rate
- API response times
- Failed meeting creations
- Customer join rates
- API quota usage

### Alerts:
- OAuth token expiration
- API rate limit approaching
- Failed meeting creations
- Zoom API downtime

---

## Sources

- [Zoom Meeting API](https://developers.zoom.us/docs/api/rest/reference/zoom-api/methods/)
- [Meetings APIs Documentation](https://developers.zoom.us/docs/api/meetings/)
- [Simple Guide to Creating Zoom Meetings with the Zoom API](https://medium.com/@abhay.nmdo.dev/simple-guide-to-creating-zoom-meetings-with-the-zoom-api-9254ff175283)
- [How to create a meeting using the zoom api - Stack Overflow](https://stackoverflow.com/questions/71041806/how-to-create-a-meeting-using-the-zoom-api)
- [Zoom Developer Documentation Guide](https://moldstud.com/articles/p-navigating-zoom-developer-documentation-a-quick-start-guide-for-seamless-integration)
