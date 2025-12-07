# Mailgun Email Integration Guide

## Overview

This application uses Mailgun for sending booking confirmation emails to both customers and business users. Each email includes a beautifully formatted HTML template and an ICS calendar file attachment for easy calendar integration.

## Features

- **Asynchronous Email Sending**: Emails are sent asynchronously using Spring's `@Async` annotation, ensuring bookings complete quickly without waiting for email delivery
- **Dual Notifications**: Both customer and business receive confirmation emails with different content
- **Calendar Integration**: ICS calendar file automatically attached to sync with user calendars
- **HTML Email Templates**: Professional, gradient-styled email templates with welcoming content
- **Error Handling**: Email failures don't block booking creation - they're logged and handled gracefully

## Architecture

### Components

1. **MailgunConfig** (`config/MailgunConfig.java`)
   - Configures Mailgun API client with credentials
   - Reads configuration from application.properties
   - Creates `MailgunMessagesApi` bean for sending emails

2. **EmailService** (`service/EmailService.java`)
   - Sends customer booking confirmation emails
   - Sends business booking notification emails
   - Generates HTML email bodies
   - Handles ICS attachment creation

3. **CalendarService** (`service/CalendarService.java`)
   - Generates RFC2445-compliant ICS calendar files using iCal4j library
   - Includes organizer, attendee, RSVP, event details

4. **AsyncConfig** (`config/AsyncConfig.java`)
   - Enables Spring's `@Async` annotation support
   - Allows email sending to run in background threads

5. **BookingService** (`service/BookingService.java`)
   - Triggers email sending after successful booking creation
   - Reloads booking with full relationships for email content
   - Gracefully handles email failures

## Setup Instructions

### 1. Create Mailgun Account

1. Sign up at [mailgun.com](https://www.mailgun.com)
2. Verify your domain or use Mailgun's sandbox domain for testing
3. Get your API key from the Mailgun dashboard

### 2. Configure Environment Variables

Set the following environment variables:

```bash
export MAILGUN_API_KEY="your-mailgun-api-key"
export MAILGUN_DOMAIN="your-domain.mailgun.org"
export MAILGUN_FROM_EMAIL="noreply@your-domain.com"
export MAILGUN_FROM_NAME="Your Business Name"
export MAILGUN_ENABLED="true"
```

### 3. Update application.properties

The application.properties file already includes Mailgun configuration with default values:

```properties
# Mailgun Configuration
mailgun.api-key=${MAILGUN_API_KEY:your-mailgun-api-key}
mailgun.domain=${MAILGUN_DOMAIN:your-domain.mailgun.org}
mailgun.from-email=${MAILGUN_FROM_EMAIL:noreply@your-domain.com}
mailgun.from-name=${MAILGUN_FROM_NAME:Session Scheduler}
mailgun.enabled=${MAILGUN_ENABLED:true}
```

### 4. Sandbox Domain Testing

For testing, you can use Mailgun's sandbox domain:

1. Go to Mailgun Dashboard → Sending → Domains
2. Click on your sandbox domain
3. Add authorized recipients (email addresses that can receive test emails)
4. Use the sandbox domain in your configuration

Example:
```bash
export MAILGUN_DOMAIN="sandboxXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX.mailgun.org"
export MAILGUN_FROM_EMAIL="noreply@sandboxXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX.mailgun.org"
```

### 5. Disable Email Sending (Optional)

To disable email sending (useful for development/testing):

```bash
export MAILGUN_ENABLED="false"
```

Or in application.properties:
```properties
mailgun.enabled=false
```

## Email Templates

### Customer Confirmation Email

**Subject**: "Your {Session Name} Session is Confirmed! ✨"

**Content**:
- Welcoming greeting with customer name
- Session details (type, date/time, duration, provider)
- Calendar attachment instructions
- Warm, soothing tone

**Design**: Purple gradient header, clean layout, professional styling

### Business Notification Email

**Subject**: "New Booking: {Session Name} with {Customer Name}"

**Content**:
- Customer details (name, email, phone)
- Session details (type, date/time, duration)
- Customer notes (if provided)
- Calendar attachment for business calendar

**Design**: Blue gradient header, business-focused layout

## ICS Calendar File

Each email includes an ICS calendar file attachment with:

- **Event Title**: Session name with participant details
- **Date/Time**: Converted to system timezone
- **Duration**: Based on session type
- **Organizer**: Business user email and name
- **Attendee**: Customer email and name with RSVP
- **Description**: Session details and customer notes
- **Status**: CONFIRMED
- **Location**: Configurable (currently "Online Session")

## Testing

### Test Email Sending

1. Create a booking through the API:

```bash
curl -X POST http://localhost:8080/api/tenants/{tenantId}/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "sessionTypeId": "session-type-uuid",
    "startTime": 1733565600000,
    "email": "customer@example.com",
    "firstName": "Jane",
    "lastName": "Doe",
    "phone": "+1234567890",
    "participants": 1,
    "notes": "Looking forward to the session",
    "customerTimezone": "America/New_York"
  }'
```

2. Check application logs for email sending confirmation:

```
INFO ... EmailService : Booking confirmation email sent to customer: customer@example.com
INFO ... EmailService : Booking notification email sent to business: business@example.com
```

3. Check Mailgun dashboard → Logs to verify email delivery

### Test ICS File Generation

The CalendarService can be tested independently:

```java
@Autowired
private CalendarService calendarService;

byte[] icsFile = calendarService.generateIcsFile(
    "Yoga Session",
    "60-minute yoga session with relaxation",
    LocalDateTime.now().plusDays(1),
    LocalDateTime.now().plusDays(1).plusHours(1),
    "Online Session",
    "business@example.com",
    "Yoga Studio",
    "customer@example.com",
    "Jane Doe"
);
```

## Email Flow

1. **Customer Creates Booking** → BookingService.createBooking()
2. **Booking Saved to Database** → bookingRepository.save()
3. **Booking Reloaded with Relationships** → bookingRepository.findById() with customer, sessionType, tenant
4. **Tenant and Business User Retrieved** → tenantService.getTenantById(), tenantService.getBusinessEmailForTenant()
5. **Email Sending Triggered (Async)** → emailService.sendCustomerBookingConfirmation(), emailService.sendBusinessBookingNotification()
6. **ICS File Generated** → calendarService.generateIcsFile()
7. **Email Sent via Mailgun** → mailgunMessagesApi.sendMessage()
8. **Logs Updated** → Success or error logged

**Important**: Email failures don't block booking creation - they're caught and logged

## Customization

### Email Templates

To customize email templates, edit the HTML in:

- `EmailService.buildCustomerEmailBody()` - Customer confirmation template
- `EmailService.buildBusinessEmailBody()` - Business notification template

### Calendar Event Details

To customize calendar event details, modify parameters in:

- `BookingService.createBooking()` - Where calendarService.generateIcsFile() is called

### Email Sender Name

Update the `MAILGUN_FROM_NAME` environment variable or the default in application.properties

### Location

Currently hardcoded to "Online Session". To make dynamic:

1. Add location field to SessionType or Booking entity
2. Pass location parameter in calendarService.generateIcsFile() call

## Troubleshooting

### Emails Not Sending

**Check Mailgun Configuration**:
```bash
# Verify environment variables are set
echo $MAILGUN_API_KEY
echo $MAILGUN_DOMAIN
echo $MAILGUN_ENABLED
```

**Check Application Logs**:
```bash
tail -f logs/application.log | grep EmailService
```

**Common Issues**:
- Invalid API key → Check Mailgun dashboard
- Domain not verified → Verify domain in Mailgun
- Recipient not authorized (sandbox) → Add recipient to authorized list
- Email disabled → Set `mailgun.enabled=true`

### ICS File Not Attached

**Check Logs**:
```bash
tail -f logs/application.log | grep CalendarService
```

**Common Issues**:
- Temporary file creation failed → Check disk space and permissions
- Invalid date/time → Ensure booking times are valid LocalDateTime

### Email Formatting Issues

**Test in Multiple Email Clients**:
- Gmail
- Outlook
- Apple Mail
- Mobile devices

**HTML Email Best Practices**:
- Use inline CSS (already implemented)
- Test in email testing tools like Litmus or Email on Acid
- Avoid complex CSS that may not be supported

## Production Deployment

### 1. Verify Domain

In Mailgun dashboard:
1. Go to Sending → Domains
2. Add your domain
3. Add DNS records (MX, TXT, CNAME)
4. Wait for verification (can take 24-48 hours)

### 2. Set Production Environment Variables

```bash
export MAILGUN_API_KEY="live-api-key-from-mailgun"
export MAILGUN_DOMAIN="mg.your-production-domain.com"
export MAILGUN_FROM_EMAIL="noreply@your-production-domain.com"
export MAILGUN_FROM_NAME="Your Production Business Name"
export MAILGUN_ENABLED="true"
```

### 3. Configure Email Reputation

- Start with low volume
- Monitor bounce rates and complaints
- Implement SPF, DKIM, and DMARC
- Follow email best practices

### 4. Monitor Email Delivery

- Set up Mailgun webhooks for delivery events
- Monitor dashboard for bounces, complaints, unsubscribes
- Implement retry logic for failed deliveries (if needed)

## Dependencies

### Maven Dependencies

```xml
<!-- Mailgun for email sending -->
<dependency>
    <groupId>com.mailgun</groupId>
    <artifactId>mailgun-java</artifactId>
    <version>2.1.0</version>
</dependency>

<!-- iCal4j for ICS calendar file generation -->
<dependency>
    <groupId>org.mnode.ical4j</groupId>
    <artifactId>ical4j</artifactId>
    <version>3.2.14</version>
</dependency>
```

### Spring Configuration

- Spring Boot 3.2.0
- Spring Web
- Spring Data JPA
- Spring Security OAuth2 Resource Server

## API Reference

### Mailgun Java SDK

- [GitHub Repository](https://github.com/mailgun/mailgun-java)
- [Mailgun API Documentation](https://documentation.mailgun.com/en/latest/)

### iCal4j

- [Official Website](https://www.ical4j.org/)
- [GitHub Repository](https://github.com/ical4j/ical4j)

## Security Considerations

1. **Never commit API keys** to version control
2. **Use environment variables** for all sensitive configuration
3. **Enable HTTPS** for all email links (when applicable)
4. **Validate email addresses** before sending
5. **Rate limit email sending** to prevent abuse
6. **Monitor for spam complaints** and adjust content accordingly

## Future Enhancements

Potential improvements for the email system:

1. **Email Templates in Database**: Store templates in database for easy customization
2. **Email Queue**: Implement message queue (RabbitMQ, Kafka) for better reliability
3. **Retry Logic**: Automatic retry for failed email deliveries
4. **Email Preferences**: Allow users to opt out of certain email types
5. **Email Analytics**: Track open rates, click rates (using Mailgun tracking)
6. **Multi-language Support**: Send emails in customer's preferred language
7. **Dynamic Location**: Support for physical addresses and video call links
8. **Reminder Emails**: Send booking reminders before sessions
9. **Cancellation Emails**: Send emails when bookings are cancelled
10. **Email Scheduling**: Schedule emails to be sent at optimal times
