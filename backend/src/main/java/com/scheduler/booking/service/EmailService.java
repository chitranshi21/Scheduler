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

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a");

    /**
     * Send booking confirmation email to customer
     */
    @Async
    public void sendCustomerBookingConfirmation(Booking booking, Tenant tenant) {
        if (!mailgunConfig.isEnabled()) {
            log.info("Email sending is disabled. Skipping customer booking confirmation.");
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
                    booking.getSessionType().getDurationMinutes(), tenant);

            // Generate ICS file
            byte[] icsFile = calendarService.generateIcsFile(
                    sessionName + " with " + tenant.getName(),
                    "Your session: " + sessionName + "\\n\\n" +
                    (booking.getNotes() != null ? "Notes: " + booking.getNotes() : ""),
                    booking.getStartTime(),
                    booking.getEndTime(),
                    "Online Session", // You can customize this
                    tenant.getEmail(),
                    tenant.getName(),
                    booking.getCustomer().getEmail(),
                    customerName
            );

            String icsFilename = calendarService.generateIcsFilename(sessionName, booking.getStartTime());

            // Send email
            sendEmailWithAttachment(
                    booking.getCustomer().getEmail(),
                    subject,
                    htmlBody,
                    icsFile,
                    icsFilename
            );

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
            log.info("Email sending is disabled. Skipping business booking notification.");
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
                    booking.getSessionType().getDurationMinutes(), booking.getNotes());

            // Generate ICS file
            byte[] icsFile = calendarService.generateIcsFile(
                    sessionName + " - " + customerName,
                    "Customer: " + customerName + "\\n" +
                    "Email: " + booking.getCustomer().getEmail() + "\\n" +
                    "Phone: " + booking.getCustomer().getPhone() + "\\n\\n" +
                    (booking.getNotes() != null ? "Notes: " + booking.getNotes() : ""),
                    booking.getStartTime(),
                    booking.getEndTime(),
                    "Online Session",
                    businessEmail,
                    tenant.getName(),
                    booking.getCustomer().getEmail(),
                    customerName
            );

            String icsFilename = calendarService.generateIcsFilename(sessionName + "_" + customerName,
                    booking.getStartTime());

            // Send email
            sendEmailWithAttachment(
                    businessEmail,
                    subject,
                    htmlBody,
                    icsFile,
                    icsFilename
            );

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
     * Build HTML email body for customer confirmation
     */
    private String buildCustomerEmailBody(String customerName, String sessionName,
                                         String formattedDateTime, int durationMinutes,
                                         Tenant tenant) {
        return String.format(
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <style>" +
                "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; }" +
                "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                "        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center; }" +
                "        .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }" +
                "        .booking-details { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }" +
                "        .detail-row { padding: 10px 0; border-bottom: 1px solid #e5e7eb; }" +
                "        .detail-row:last-child { border-bottom: none; }" +
                "        .label { font-weight: 600; color: #6b7280; }" +
                "        .value { color: #1f2937; }" +
                "        .calendar-note { background: #ecfdf5; border-left: 4px solid #10b981; padding: 15px; margin: 20px 0; border-radius: 4px; }" +
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
                "            <p style=\"color: #4b5563;\">We're delighted to confirm your session booking. We look forward to seeing you!</p>" +
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
                "            <div class=\"calendar-note\">" +
                "                <strong>Add to Calendar</strong><br>" +
                "                An ICS calendar file is attached to this email. Simply open it to add this session to your calendar automatically!" +
                "            </div>" +
                "" +
                "            <p style=\"color: #4b5563; margin-top: 20px;\">If you need to reschedule or have any questions, please don't hesitate to reach out.</p>" +
                "" +
                "            <p style=\"color: #4b5563;\">We're here to provide you with a wonderful experience. Take a deep breath, relax, and we'll see you soon!</p>" +
                "        </div>" +
                "        <div class=\"footer\">" +
                "            <p>This email was sent by %s</p>" +
                "            <p style=\"font-size: 12px; color: #9ca3af;\">Please do not reply to this email.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>",
                customerName, sessionName, formattedDateTime, durationMinutes,
                tenant.getName(), tenant.getName());
    }

    /**
     * Build HTML email body for business notification
     */
    private String buildBusinessEmailBody(String customerName, String customerEmail,
                                         String customerPhone, String sessionName,
                                         String formattedDateTime, int durationMinutes,
                                         String notes) {
        String notesSection = notes != null && !notes.isEmpty()
                ? "<div class=\"notes\"><strong>Customer Notes:</strong><br>" + notes + "</div>"
                : "";

        return String.format(
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <style>" +
                "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; }" +
                "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                "        .header { background: linear-gradient(135deg, #3b82f6 0%%, #1e40af 100%%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center; }" +
                "        .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }" +
                "        .booking-details { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }" +
                "        .detail-row { padding: 10px 0; border-bottom: 1px solid #e5e7eb; }" +
                "        .detail-row:last-child { border-bottom: none; }" +
                "        .label { font-weight: 600; color: #6b7280; }" +
                "        .value { color: #1f2937; }" +
                "        .notes { background: #fef3c7; border-left: 4px solid #f59e0b; padding: 15px; margin: 20px 0; border-radius: 4px; }" +
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
                "            <p style=\"color: #4b5563; margin-top: 20px;\">" +
                "                An ICS calendar file is attached. Add it to your calendar to keep track of this appointment." +
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
                notesSection
        );
    }
}
