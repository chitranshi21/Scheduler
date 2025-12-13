package com.scheduler.booking.service;

import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.model.message.Message;
import com.mailgun.model.message.MessageResponse;
import com.scheduler.booking.config.MailgunConfig;
import com.scheduler.booking.model.Booking;
import com.scheduler.booking.model.Tenant;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final MailgunConfig mailgunConfig;
    private final MailgunMessagesApi mailgunMessagesApi;
    private final CalendarService calendarService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a");

    /**
     * Send booking confirmation email to customer
     */
    @Async
    public void sendCustomerBookingConfirmation(Booking booking, Tenant tenant) {
        if (!mailgunConfig.isEnabled()) {
            log.warn("⚠️ Email sending is DISABLED (mailgun.enabled=false). Skipping customer booking confirmation for booking {}. " +
                    "To enable emails, set MAILGUN_ENABLED=true and configure Mailgun credentials.", booking.getId());
            return;
        }

        try {
            String customerName = booking.getCustomer().getFirstName() + " " + booking.getCustomer().getLastName();
            String sessionName = booking.getSessionType().getName();
            String formattedDateTime = booking.getStartTime().format(DATE_TIME_FORMATTER);

            // Generate email subject
            String subject = "Your " + sessionName + " Session is Confirmed! ✨";

            // Generate email body
            String htmlBody = buildCustomerEmailBody(customerName, sessionName, formattedDateTime,
                    booking.getSessionType().getDurationMinutes(), tenant, booking.getStartTime(), booking.getEndTime(),
                    booking.getSessionType().getMeetingLink(), booking.getSessionType().getMeetingPassword());

            // Generate ICS file with Google Meet link
            String meetingLink = booking.getSessionType().getMeetingLink();
            log.info("Customer email - Meeting link from session type: {}", meetingLink);

            String description = "Your session: " + sessionName + "\\n\\n";
            if (meetingLink != null && !meetingLink.isEmpty()) {
                description += "Join Google Meet: " + meetingLink + "\\n\\n";
                log.info("Customer email - Added meeting link to ICS description");
            } else {
                log.warn("Customer email - No meeting link found for session type: {}", sessionName);
            }
            if (booking.getNotes() != null) {
                description += "Notes: " + booking.getNotes();
            }

            byte[] icsFile = calendarService.generateIcsFile(
                    sessionName + " with " + tenant.getName(),
                    description,
                    booking.getStartTime(),
                    booking.getEndTime(),
                    meetingLink != null && !meetingLink.isEmpty() ? meetingLink : "Online Session",
                    tenant.getEmail(),
                    tenant.getName(),
                    booking.getCustomer().getEmail(),
                    customerName);

            String icsFilename = calendarService.generateIcsFilename(sessionName, booking.getStartTime());

            // Send email
            sendEmailWithAttachment(
                    booking.getCustomer().getEmail(),
                    subject,
                    htmlBody,
                    icsFile,
                    icsFilename);

            log.info("Booking confirmation email sent to customer: {}", booking.getCustomer().getEmail());

        } catch (Exception e) {
            log.error("Failed to send customer booking confirmation email", e);
        }
    }

    /**
     * Send booking notification email to business
     */
    @Async
    public void sendBusinessBookingNotification(Booking booking, Tenant tenant, String businessEmail) {
        if (!mailgunConfig.isEnabled()) {
            log.warn("⚠️ Email sending is DISABLED (mailgun.enabled=false). Skipping business booking notification for booking {}. " +
                    "To enable emails, set MAILGUN_ENABLED=true and configure Mailgun credentials.", booking.getId());
            return;
        }

        try {
            String customerName = booking.getCustomer().getFirstName() + " " + booking.getCustomer().getLastName();
            String sessionName = booking.getSessionType().getName();
            String formattedDateTime = booking.getStartTime().format(DATE_TIME_FORMATTER);

            // Generate email subject
            String subject = "New Booking: " + sessionName + " with " + customerName;

            // Generate email body
            String htmlBody = buildBusinessEmailBody(customerName, booking.getCustomer().getEmail(),
                    booking.getCustomer().getPhone(), sessionName, formattedDateTime,
                    booking.getSessionType().getDurationMinutes(), booking.getNotes(),
                    booking.getStartTime(), booking.getEndTime(),
                    booking.getSessionType().getMeetingLink(), booking.getSessionType().getMeetingPassword());

            // Generate ICS file with Google Meet link
            String meetingLinkBusiness = booking.getSessionType().getMeetingLink();
            log.info("Business email - Meeting link from session type: {}", meetingLinkBusiness);

            String descriptionBusiness = "Customer: " + customerName + "\\n" +
                    "Email: " + booking.getCustomer().getEmail() + "\\n" +
                    "Phone: " + booking.getCustomer().getPhone() + "\\n\\n";
            if (meetingLinkBusiness != null && !meetingLinkBusiness.isEmpty()) {
                descriptionBusiness += "Google Meet Link: " + meetingLinkBusiness + "\\n\\n";
                log.info("Business email - Added meeting link to ICS description");
            } else {
                log.warn("Business email - No meeting link found for session type: {}", sessionName);
            }
            if (booking.getNotes() != null) {
                descriptionBusiness += "Notes: " + booking.getNotes();
            }

            byte[] icsFile = calendarService.generateIcsFile(
                    sessionName + " - " + customerName,
                    descriptionBusiness,
                    booking.getStartTime(),
                    booking.getEndTime(),
                    meetingLinkBusiness != null && !meetingLinkBusiness.isEmpty() ? meetingLinkBusiness
                            : "Online Session",
                    businessEmail,
                    tenant.getName(),
                    booking.getCustomer().getEmail(),
                    customerName);

            String icsFilename = calendarService.generateIcsFilename(sessionName + "_" + customerName,
                    booking.getStartTime());

            // Send email
            sendEmailWithAttachment(
                    businessEmail,
                    subject,
                    htmlBody,
                    icsFile,
                    icsFilename);

            log.info("Booking notification email sent to business: {}", businessEmail);

        } catch (Exception e) {
            log.error("Failed to send business booking notification email", e);
        }
    }

    /**
     * Send email with ICS attachment using Mailgun
     */
    private void sendEmailWithAttachment(String to, String subject, String htmlBody,
            byte[] attachment, String attachmentFilename) {
        try {
            // Create temporary file for attachment
            File tempFile = File.createTempFile("calendar", ".ics");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(attachment);
            }

            // Build message
            Message message = Message.builder()
                    .from(mailgunConfig.getFromName() + " <" + mailgunConfig.getFromEmail() + ">")
                    .to(to)
                    .subject(subject)
                    .html(htmlBody)
                    .attachment(tempFile)
                    .build();

            // Send via Mailgun
            MessageResponse response = mailgunMessagesApi.sendMessage(mailgunConfig.getDomain(), message);

            log.info("Email sent successfully. Message ID: {}", response.getId());

            // Clean up temp file
            tempFile.delete();

        } catch (FeignException e) {
            log.error("Mailgun API error: {} - {}", e.status(), e.contentUTF8());
            throw new RuntimeException("Failed to send email via Mailgun", e);
        } catch (IOException e) {
            log.error("IO error creating temporary file", e);
            throw new RuntimeException("Failed to create temporary file for attachment", e);
        }
    }

    /**
     * Generate Google Calendar URL
     */
    private String generateGoogleCalendarUrl(String title, String description, LocalDateTime startTime,
            LocalDateTime endTime) {
        try {
            String start = startTime.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
            String end = endTime.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));

            return String.format(
                    "https://calendar.google.com/calendar/render?action=TEMPLATE&text=%s&dates=%s/%s&details=%s",
                    java.net.URLEncoder.encode(title, "UTF-8"),
                    start,
                    end,
                    java.net.URLEncoder.encode(description, "UTF-8"));
        } catch (Exception e) {
            return "#";
        }
    }

    /**
     * Generate Outlook Calendar URL
     */
    private String generateOutlookCalendarUrl(String title, String description, LocalDateTime startTime,
            LocalDateTime endTime) {
        try {
            String start = startTime.format(DateTimeFormatter.ISO_DATE_TIME);
            String end = endTime.format(DateTimeFormatter.ISO_DATE_TIME);

            return String.format(
                    "https://outlook.live.com/calendar/0/deeplink/compose?subject=%s&startdt=%s&enddt=%s&body=%s",
                    java.net.URLEncoder.encode(title, "UTF-8"),
                    start,
                    end,
                    java.net.URLEncoder.encode(description, "UTF-8"));
        } catch (Exception e) {
            return "#";
        }
    }

    /**
     * Build HTML email body for customer confirmation
     */
    private String buildCustomerEmailBody(String customerName, String sessionName,
            String formattedDateTime, int durationMinutes,
            Tenant tenant, LocalDateTime startTime, LocalDateTime endTime,
            String meetingLink, String meetingPassword) {
        // Build description with meeting link for calendar events
        String calendarDescription = "Your session: " + sessionName;
        if (meetingLink != null && !meetingLink.isEmpty()) {
            calendarDescription += "\n\nJoin Google Meet: " + meetingLink;
            if (meetingPassword != null && !meetingPassword.isEmpty()) {
                calendarDescription += "\nPassword: " + meetingPassword;
            }
        }

        String googleCalUrl = generateGoogleCalendarUrl(
                sessionName + " with " + tenant.getName(),
                calendarDescription,
                startTime,
                endTime);

        String outlookCalUrl = generateOutlookCalendarUrl(
                sessionName + " with " + tenant.getName(),
                calendarDescription,
                startTime,
                endTime);

        // Meeting link section
        String meetingSection = "";
        if (meetingLink != null && !meetingLink.isEmpty()) {
            String passwordSection = "";
            if (meetingPassword != null && !meetingPassword.isEmpty()) {
                passwordSection = "<p style=\"margin: 10px 0; color: #1f2937;\"><strong>Password:</strong> "
                        + meetingPassword + "</p>";
            }
            meetingSection = "<div style=\"background: #dbeafe; border-left: 4px solid #3b82f6; padding: 20px; margin: 25px 0; border-radius: 4px; text-align: center;\">"
                    +
                    "    <p style=\"font-weight: 700; color: #1e40af; font-size: 16px; margin-bottom: 15px;\">📹 Join Virtual Meeting</p>"
                    +
                    "    <a href=\"" + meetingLink
                    + "\" style=\"display: inline-block; background: #4285f4; color: white; padding: 14px 32px; text-decoration: none; border-radius: 6px; font-weight: 600; font-size: 16px; margin: 10px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.2);\">Join Google Meet</a>"
                    +
                    passwordSection +
                    "    <p style=\"font-size: 13px; color: #6b7280; margin-top: 10px;\">Click the button above when it's time for your session</p>"
                    +
                    "</div>";
        }
        return String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "    <meta charset=\"UTF-8\">" +
                        "    <style>" +
                        "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; }"
                        +
                        "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                        "        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center; }"
                        +
                        "        .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }" +
                        "        .booking-details { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }"
                        +
                        "        .detail-row { padding: 10px 0; border-bottom: 1px solid #e5e7eb; }" +
                        "        .detail-row:last-child { border-bottom: none; }" +
                        "        .label { font-weight: 600; color: #6b7280; }" +
                        "        .value { color: #1f2937; }" +
                        "        .calendar-note { background: #ecfdf5; border-left: 4px solid #10b981; padding: 15px; margin: 20px 0; border-radius: 4px; }"
                        +
                        "        .calendar-buttons { text-align: center; margin: 25px 0; }" +
                        "        .cal-btn { display: inline-block; margin: 8px; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 600; font-size: 14px; transition: all 0.3s; }"
                        +
                        "        .cal-btn-google { background: #4285f4; color: white; }" +
                        "        .cal-btn-outlook { background: #0078d4; color: white; }" +
                        "        .footer { text-align: center; padding: 20px; color: #6b7280; font-size: 14px; }" +
                        "    </style>" +
                        "</head>" +
                        "<body>" +
                        "    <div class=\"container\">" +
                        "        <div class=\"header\">" +
                        "            <h1 style=\"margin: 0; font-size: 28px;\">Booking Confirmed!</h1>" +
                        "        </div>" +
                        "        <div class=\"content\">" +
                        "            <p style=\"font-size: 18px; color: #1f2937;\">Hello %s,</p>" +
                        "            <p style=\"color: #4b5563;\">We're delighted to confirm your session booking. We look forward to seeing you!</p>"
                        +
                        "" +
                        "            <div class=\"booking-details\">" +
                        "                <div class=\"detail-row\">" +
                        "                    <div class=\"label\">Session Type</div>" +
                        "                    <div class=\"value\">%s</div>" +
                        "                </div>" +
                        "                <div class=\"detail-row\">" +
                        "                    <div class=\"label\">Date & Time</div>" +
                        "                    <div class=\"value\">%s</div>" +
                        "                </div>" +
                        "                <div class=\"detail-row\">" +
                        "                    <div class=\"label\">Duration</div>" +
                        "                    <div class=\"value\">%d minutes</div>" +
                        "                </div>" +
                        "                <div class=\"detail-row\">" +
                        "                    <div class=\"label\">Provider</div>" +
                        "                    <div class=\"value\">%s</div>" +
                        "                </div>" +
                        "            </div>" +
                        "" +
                        "            %s" +
                        "" +
                        "            <div class=\"calendar-buttons\">" +
                        "                <p style=\"font-weight: 600; color: #1f2937; margin-bottom: 15px;\">Add to Your Calendar</p>"
                        +
                        "                <a href=\"%s\" class=\"cal-btn cal-btn-google\">+ Google Calendar</a>" +
                        "                <a href=\"%s\" class=\"cal-btn cal-btn-outlook\">+ Outlook</a>" +
                        "            </div>" +
                        "" +
                        "            <div class=\"calendar-note\">" +
                        "                <strong>Calendar File Attached</strong><br>" +
                        "                An ICS calendar file is also attached to this email. Open it to add this session to Apple Calendar, Outlook, or any other calendar app!"
                        +
                        "            </div>" +
                        "" +
                        "            <p style=\"color: #4b5563; margin-top: 20px;\">If you need to reschedule or have any questions, please don't hesitate to reach out.</p>"
                        +
                        "" +
                        "            <p style=\"color: #4b5563;\">We're here to provide you with a wonderful experience. Take a deep breath, relax, and we'll see you soon!</p>"
                        +
                        "        </div>" +
                        "        <div class=\"footer\">" +
                        "            <p>This email was sent by %s</p>" +
                        "            <p style=\"font-size: 12px; color: #9ca3af;\">Please do not reply to this email.</p>"
                        +
                        "        </div>" +
                        "    </div>" +
                        "</body>" +
                        "</html>",
                customerName, sessionName, formattedDateTime, durationMinutes,
                tenant.getName(), meetingSection, googleCalUrl, outlookCalUrl, tenant.getName());
    }

    /**
     * Build HTML email body for business notification
     */
    private String buildBusinessEmailBody(String customerName, String customerEmail,
            String customerPhone, String sessionName,
            String formattedDateTime, int durationMinutes,
            String notes, LocalDateTime startTime, LocalDateTime endTime,
            String meetingLink, String meetingPassword) {
        String notesSection = notes != null && !notes.isEmpty()
                ? "<div class=\"notes\"><strong>Customer Notes:</strong><br>" + notes + "</div>"
                : "";

        // Meeting link section for business
        String meetingSection = "";
        if (meetingLink != null && !meetingLink.isEmpty()) {
            String passwordSection = "";
            if (meetingPassword != null && !meetingPassword.isEmpty()) {
                passwordSection = "<p style=\"margin: 10px 0; color: #1f2937;\"><strong>Password:</strong> "
                        + meetingPassword + "</p>";
            }
            meetingSection = "<div style=\"background: #dbeafe; border-left: 4px solid #3b82f6; padding: 20px; margin: 25px 0; border-radius: 4px; text-align: center;\">"
                    +
                    "    <p style=\"font-weight: 700; color: #1e40af; font-size: 16px; margin-bottom: 15px;\">📹 Virtual Meeting Link</p>"
                    +
                    "    <a href=\"" + meetingLink
                    + "\" style=\"display: inline-block; background: #4285f4; color: white; padding: 14px 32px; text-decoration: none; border-radius: 6px; font-weight: 600; font-size: 16px; margin: 10px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.2);\">Start Google Meet</a>"
                    +
                    passwordSection +
                    "    <p style=\"font-size: 13px; color: #6b7280; margin-top: 10px;\">As the host, use this link to start the meeting</p>"
                    +
                    "</div>";
        }

        // Build description with meeting link for calendar events
        String calendarDescriptionBusiness = "Customer: " + customerName + "\\nEmail: " + customerEmail + "\\nPhone: "
                + customerPhone;
        if (meetingLink != null && !meetingLink.isEmpty()) {
            calendarDescriptionBusiness += "\n\nGoogle Meet Link: " + meetingLink;
            if (meetingPassword != null && !meetingPassword.isEmpty()) {
                calendarDescriptionBusiness += "\nPassword: " + meetingPassword;
            }
        }

        String googleCalUrl = generateGoogleCalendarUrl(
                sessionName + " - " + customerName,
                calendarDescriptionBusiness,
                startTime,
                endTime);

        String outlookCalUrl = generateOutlookCalendarUrl(
                sessionName + " - " + customerName,
                calendarDescriptionBusiness,
                startTime,
                endTime);

        return String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "    <meta charset=\"UTF-8\">" +
                        "    <style>" +
                        "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; }"
                        +
                        "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                        "        .header { background: linear-gradient(135deg, #3b82f6 0%%, #1e40af 100%%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center; }"
                        +
                        "        .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }" +
                        "        .booking-details { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }"
                        +
                        "        .detail-row { padding: 10px 0; border-bottom: 1px solid #e5e7eb; }" +
                        "        .detail-row:last-child { border-bottom: none; }" +
                        "        .label { font-weight: 600; color: #6b7280; }" +
                        "        .value { color: #1f2937; }" +
                        "        .notes { background: #fef3c7; border-left: 4px solid #f59e0b; padding: 15px; margin: 20px 0; border-radius: 4px; }"
                        +
                        "        .calendar-buttons { text-align: center; margin: 25px 0; }" +
                        "        .cal-btn { display: inline-block; margin: 8px; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 600; font-size: 14px; }"
                        +
                        "        .cal-btn-google { background: #4285f4; color: white; }" +
                        "        .cal-btn-outlook { background: #0078d4; color: white; }" +
                        "        .footer { text-align: center; padding: 20px; color: #6b7280; font-size: 14px; }" +
                        "    </style>" +
                        "</head>" +
                        "<body>" +
                        "    <div class=\"container\">" +
                        "        <div class=\"header\">" +
                        "            <h1 style=\"margin: 0; font-size: 28px;\">New Booking Received</h1>" +
                        "        </div>" +
                        "        <div class=\"content\">" +
                        "            <p style=\"font-size: 16px; color: #1f2937;\">A new session has been booked:</p>" +
                        "" +
                        "            <div class=\"booking-details\">" +
                        "                <div class=\"detail-row\">" +
                        "                    <div class=\"label\">Customer Name</div>" +
                        "                    <div class=\"value\">%s</div>" +
                        "                </div>" +
                        "                <div class=\"detail-row\">" +
                        "                    <div class=\"label\">Email</div>" +
                        "                    <div class=\"value\"><a href=\"mailto:%s\">%s</a></div>" +
                        "                </div>" +
                        "                <div class=\"detail-row\">" +
                        "                    <div class=\"label\">Phone</div>" +
                        "                    <div class=\"value\">%s</div>" +
                        "                </div>" +
                        "                <div class=\"detail-row\">" +
                        "                    <div class=\"label\">Session Type</div>" +
                        "                    <div class=\"value\">%s</div>" +
                        "                </div>" +
                        "                <div class=\"detail-row\">" +
                        "                    <div class=\"label\">Date & Time</div>" +
                        "                    <div class=\"value\">%s</div>" +
                        "                </div>" +
                        "                <div class=\"detail-row\">" +
                        "                    <div class=\"label\">Duration</div>" +
                        "                    <div class=\"value\">%d minutes</div>" +
                        "                </div>" +
                        "            </div>" +
                        "" +
                        "            %s" +
                        "" +
                        "            %s" +
                        "" +
                        "            <div class=\"calendar-buttons\">" +
                        "                <p style=\"font-weight: 600; color: #1f2937; margin-bottom: 15px;\">Add to Your Calendar</p>"
                        +
                        "                <a href=\"%s\" class=\"cal-btn cal-btn-google\">+ Google Calendar</a>" +
                        "                <a href=\"%s\" class=\"cal-btn cal-btn-outlook\">+ Outlook</a>" +
                        "            </div>" +
                        "" +
                        "            <p style=\"color: #4b5563; margin-top: 20px;\">" +
                        "                An ICS calendar file is also attached. Open it to add to Apple Calendar or any other calendar app."
                        +
                        "            </p>" +
                        "        </div>" +
                        "        <div class=\"footer\">" +
                        "            <p>Session Scheduler - Business Notification</p>" +
                        "        </div>" +
                        "    </div>" +
                        "</body>" +
                        "</html>",
                customerName, customerEmail, customerEmail,
                customerPhone != null ? customerPhone : "Not provided",
                sessionName, formattedDateTime, durationMinutes,
                notesSection, meetingSection, googleCalUrl, outlookCalUrl);
    }
}
