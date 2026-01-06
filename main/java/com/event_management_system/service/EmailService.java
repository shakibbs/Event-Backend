package com.event_management_system.service;

import com.event_management_system.entity.Event;
import com.event_management_system.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * Service for sending email notifications.
 * Handles event reminder emails to attendees.
 * 
 * Purpose:
 * - Send professionally formatted HTML emails
 * - Abstract email sending logic from scheduler
 * - Centralize email template management
 * 
 * Dependencies:
 * - JavaMailSender: Spring's email sending interface (autowired from spring-boot-starter-mail)
 * - Configuration from application.properties (SMTP settings)
 * 
 * Email Flow:
 * 1. EventReminderJob calls sendEventReminder(event, user)
 * 2. Service creates HTML email with event details
 * 3. JavaMailSender sends via SMTP (Gmail, SendGrid, etc.)
 * 4. Returns true/false based on success
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    /**
     * JavaMailSender - Spring's abstraction for sending emails
     * 
     * How it's autowired:
     * - @RequiredArgsConstructor (Lombok) generates constructor
     * - Spring sees spring-boot-starter-mail dependency
     * - Spring auto-configures JavaMailSender bean using application.properties
     * - Bean is injected into this final field
     * 
     * What it does:
     * - Connects to SMTP server (Gmail, SendGrid, etc.)
     * - Sends MimeMessage (email with HTML, attachments, etc.)
     * - Handles connection pooling and retries
     */
    private final JavaMailSender mailSender;

    /**
     * ApplicationLoggerService - Our custom logging service
     * Used for consistent, context-aware logging across the application
     */
    private final ApplicationLoggerService logger;

    /**
     * Email sender address from application.properties
     * 
     * @Value annotation:
     * - Reads "app.mail.from" property
     * - Injects value into this field at runtime
     * - Default: "noreply@eventmanagement.com" if property not found
     * 
     * Example in application.properties:
     * app.mail.from=noreply@eventmanagement.com
     */
    @Value("${app.mail.from:noreply@eventmanagement.com}")
    private String fromEmail;

    /**
     * Email sender name (display name in recipient's inbox)
     * 
     * Example in application.properties:
     * app.mail.fromName=Event Management System
     * 
     * How it appears to user:
     * From: Event Management System <noreply@eventmanagement.com>
     */
    @Value("${app.mail.fromName:Event Management System}")
    private String fromName;

    /**
     * Date formatter for displaying event date/time in emails
     * Pattern: "MMMM dd, yyyy 'at' hh:mm a"
     * Example: "January 15, 2026 at 02:30 PM"
     * 
     * Why this pattern:
     * - MMMM: Full month name (January)
     * - dd: Day with leading zero (01, 15)
     * - yyyy: 4-digit year (2026)
     * - hh:mm: 12-hour time with minutes (02:30)
     * - a: AM/PM marker
     */
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a");

    /**
     * Send event reminder email to a single attendee.
     * 
     * Flow:
     * 1. Create MimeMessage (supports HTML, unlike SimpleMailMessage)
     * 2. Set email headers (from, to, subject)
     * 3. Build HTML email body with event details
     * 4. Send via JavaMailSender
     * 5. Log success/failure
     * 6. Return boolean result
     * 
     * Why MimeMessage instead of SimpleMailMessage?
     * - MimeMessage: Supports HTML, attachments, inline images
     * - SimpleMailMessage: Plain text only
     * - HTML emails look more professional and can include formatting
     * 
     * Error Handling:
     * - Catches MessagingException (SMTP errors, invalid email, network issues)
     * - Logs error with context (event ID, user ID, email)
     * - Returns false on failure (caller can decide to retry or skip)
     * 
     * @param event The event to send reminder about
     * @param user The attendee to send reminder to
     * @return true if email sent successfully, false otherwise
     */
    public boolean sendEventReminder(Event event, User user) {
        try {
            // Step 1: Create MimeMessage
            // MimeMessage = MIME (Multipurpose Internet Mail Extensions) message
            // Supports HTML, attachments, headers, etc.
            MimeMessage message = mailSender.createMimeMessage();
            
            // Step 2: Create MimeMessageHelper
            // Helper simplifies setting message properties
            // Parameters:
            // - message: The MimeMessage to configure
            // - true: Enable multipart mode (allows HTML + attachments)
            // - "UTF-8": Character encoding (supports international characters)
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Step 3: Set email headers
            
            // From: Who is sending this email?
            // Format: "Display Name <email@domain.com>"
            helper.setFrom(fromEmail, fromName);
            
            // To: Who is receiving this email?
            // Uses user.getEmail() from User entity
            helper.setTo(user.getEmail());
            
            // Subject: Email subject line
            // Format: "Event Reminder: {event name}"
            // Example: "Event Reminder: Annual Tech Conference 2026"
            helper.setSubject("Event Reminder: " + event.getTitle());

            // Step 4: Build HTML email body
            // Why HTML?
            // - Better formatting (bold, colors, spacing)
            // - Professional appearance
            // - Better readability
            String emailBody = buildEmailBody(event, user);
            
            // Set email body with HTML enabled
            // Parameters:
            // - emailBody: The HTML content
            // - true: Treat as HTML (false would be plain text)
            helper.setText(emailBody, true);

            // Step 5: Send the email
            // JavaMailSender connects to SMTP server and sends
            // This is a blocking call (waits for SMTP response)
            // Typical time: 1-3 seconds
            mailSender.send(message);

            // Step 6: Log success
            // Uses our ApplicationLoggerService for consistent logging
            // Logs at INFO level with context (event ID, user ID, email)
            logger.infoWithContext(
                "EmailService",
                "sendEventReminder",
                String.format("Event reminder sent successfully to %s for event '%s'", 
                    user.getEmail(), event.getTitle()),
                "eventId", event.getId(),
                "userId", user.getId(),
                "email", user.getEmail()
            );

            return true; // Success!

        } catch (MessagingException e) {
            // MessagingException: Email sending failed
            // Possible causes:
            // - Invalid email address
            // - SMTP server down/unreachable
            // - Authentication failed
            // - Network timeout
            // - Mailbox full
            
            // Log error with full context
            logger.errorWithContext(
                "EmailService",
                "sendEventReminder - MessagingException for event: " + event.getTitle(),
                e
            );

            return false; // Failure - caller can decide to retry or skip

        } catch (Exception e) {
            // Catch any other unexpected errors
            // Examples: NullPointerException, encoding errors, etc.
            logger.errorWithContext(
                "EmailService",
                "sendEventReminder - Unexpected error for user: " + user.getEmail(),
                e
            );

            return false;
        }
    }

    /**
     * Build HTML email body with event details.
     * 
     * Creates a professional, well-formatted HTML email with:
     * - Greeting with user's name
     * - Event details (name, date, location, description)
     * - Call-to-action
     * - Footer
     * 
     * HTML Structure:
     * - Inline CSS for maximum email client compatibility
     * - Responsive design (works on mobile and desktop)
     * - Professional color scheme
     * 
     * Why inline CSS?
     * - Many email clients (Gmail, Outlook) strip <style> tags
     * - Inline styles are more reliable across email clients
     * 
     * @param event The event details
     * @param user The recipient user
     * @return HTML string for email body
     */
    private String buildEmailBody(Event event, User user) {
        // Format event start time
        // Example: "January 15, 2026 at 02:30 PM"
        String eventDateTime = event.getStartTime().format(DATE_FORMATTER);

        // Build HTML with StringBuilder for efficiency
        // StringBuilder is mutable (unlike String concatenation)
        // Better performance when building large strings
        StringBuilder body = new StringBuilder();
        
        body.append("<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>");
        
        // Greeting
        body.append("<h2 style='color: #2c3e50;'>Hello ").append(user.getFullName()).append(",</h2>");
        
        // Reminder message
        body.append("<p>This is a friendly reminder that you have an upcoming event:</p>");
        
        // Event details box with background color
        body.append("<div style='background-color: #f8f9fa; padding: 20px; border-left: 4px solid #3498db; margin: 20px 0;'>");
        
        // Event name
        body.append("<h3 style='color: #3498db; margin-top: 0;'>").append(event.getTitle()).append("</h3>");
        
        // Event date/time with icon emoji
        body.append("<p><strong>📅 Date & Time:</strong> ").append(eventDateTime).append("</p>");
        
        // Event location with icon emoji (if location exists)
        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            body.append("<p><strong>📍 Location:</strong> ").append(event.getLocation()).append("</p>");
        }
        
        // Event description (if exists)
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            body.append("<p><strong>ℹ️ Description:</strong><br>").append(event.getDescription()).append("</p>");
        }
        
        body.append("</div>");
        
        // Call to action
        body.append("<p>We look forward to seeing you there! If you have any questions, please don't hesitate to reach out.</p>");
        
        // Footer
        body.append("<hr style='border: none; border-top: 1px solid #eee; margin: 30px 0;'>");
        body.append("<p style='color: #7f8c8d; font-size: 12px;'>This is an automated reminder from Event Management System. Please do not reply to this email.</p>");
        
        body.append("</body></html>");
        
        return body.toString();
    }

    /**
     * Send email verification link to user
     * Called when user registers or requests new verification email
     * 
     * @param user User who needs to verify email
     * @param verificationToken UUID token for verification
     * @return true if email sent successfully, false otherwise
     */
}

