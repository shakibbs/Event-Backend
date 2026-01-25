package com.event_management_system.service;

import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.event_management_system.entity.Event;
import com.event_management_system.entity.User;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final ApplicationLoggerService log;

    @Value("${app.mail.from:noreply@eventmanagement.com}")
    private String fromEmail;

    @Value("${app.mail.fromName:Event Management System}")
    private String fromName;

    @Value("${app.base.url:http://localhost:8083}")
    private String baseUrl;

    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a");

    public boolean sendEventReminder(Event event, User user) {
        try {
            log.info("[EmailService] INFO - Starting sendEventReminder() to: {} for event: {}", user.getEmail(), event.getTitle());
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(user.getEmail());
            helper.setReplyTo(fromEmail);
            helper.setSubject("Reminder: " + event.getTitle());
            
            message.setHeader("X-Priority", "3");
            message.setHeader("X-Mailer", "EventManagementSystem");

            String emailBody = buildEmailBody(event, user);
            helper.setText(emailBody, true);

            log.debug("[EmailService] DEBUG - Attempting to send event reminder email message...");
            mailSender.send(message);

            log.info("[EmailService] INFO - sendEventReminder() - Event reminder sent successfully to " + user.getEmail() + " for event '" + event.getTitle() + "' (eventId=" + event.getId() + ", userId=" + user.getId() + ", email=" + user.getEmail() + ")");

            return true;

        } catch (MessagingException e) {
            log.error("[EmailService] ERROR - sendEventReminder() - MessagingException for user {}: {}", user.getEmail(), e.getMessage());
            log.error("[EmailService] ERROR - Exception details:", e);
            return false;

        } catch (Exception e) {
            log.error("[EmailService] ERROR - sendEventReminder() - Unexpected error for user: " + user.getEmail() + ": " + e.getMessage());
            log.error("[EmailService] ERROR - Exception details:", e);
            return false;
        }
    }

    private String buildEmailBody(Event event, User user) {
        String eventDateTime = event.getStartTime().format(DATE_FORMATTER);

        StringBuilder body = new StringBuilder();
        
        body.append("<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>");
        body.append("<h2 style='color: #2c3e50;'>Hello ").append(user.getFullName()).append(",</h2>");
        body.append("<p>This is a friendly reminder that you have an upcoming event:</p>");
        body.append("<div style='background-color: #f8f9fa; padding: 20px; border-left: 4px solid #3498db; margin: 20px 0;'>");
        body.append("<h3 style='color: #3498db; margin-top: 0;'>").append(event.getTitle()).append("</h3>");
        body.append("<p><strong>📅 Date & Time:</strong> ").append(eventDateTime).append("</p>");
        
        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            body.append("<p><strong>📍 Location:</strong> ").append(event.getLocation()).append("</p>");
        }
        
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            body.append("<p><strong>ℹ️ Description:</strong><br>").append(event.getDescription()).append("</p>");
        }
        
        body.append("</div>");
        body.append("<p>We look forward to seeing you there! If you have any questions, please don't hesitate to reach out.</p>");
        body.append("<hr style='border: none; border-top: 1px solid #eee; margin: 30px 0;'>");
        body.append("<p style='color: #7f8c8d; font-size: 12px;'>This is an automated reminder from Event Management System. Please do not reply to this email.</p>");
        body.append("</body></html>");
        
        return body.toString();
    }

    public boolean sendInvitationEmail(Event event, String recipientEmail, String invitationToken) {
        try {
            log.info("[EmailService] INFO - Starting sendInvitationEmail() to: {} for event: {}", recipientEmail, event.getTitle());
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(recipientEmail);
            helper.setReplyTo(fromEmail);
            helper.setSubject("You're Invited: " + event.getTitle());
            
           
            message.setHeader("X-Priority", "3");
            message.setHeader("X-Mailer", "EventManagementSystem");

            String emailBody = buildInvitationEmailBody(event, invitationToken);
            helper.setText(emailBody, true);

            log.debug("[EmailService] DEBUG - Message ready, attempting to send invitation email to: {}", recipientEmail);
            try {
                mailSender.send(message);
                log.info("[EmailService] INFO - sendInvitationEmail() - Invitation sent successfully to " + recipientEmail + " for event '" + event.getTitle() + "' (eventId=" + event.getId() + ", token=" + invitationToken + ")");
                return true;
            } catch (Exception sendError) {
                log.error("[EmailService] ERROR - SMTP send failed for {}: {}", recipientEmail, sendError.getMessage());
                log.error("[EmailService] ERROR - Send error stacktrace: ", sendError);
                return false;
            }

        } catch (MessagingException e) {
            log.error("[EmailService] ERROR - sendInvitationEmail() - MessagingException for recipient {}: {}", recipientEmail, e.getMessage());
            log.error("[EmailService] ERROR - MessagingException cause: {}", e.getCause());
            log.error("[EmailService] ERROR - Exception details: ", e);
            return false;

        } catch (Exception e) {
            log.error("[EmailService] ERROR - sendInvitationEmail() - Unexpected error for recipient: " + recipientEmail + ": " + e.getMessage());
            log.error("[EmailService] ERROR - Exception details: ", e);
            return false;
        }
    }

    private String buildInvitationEmailBody(Event event, String invitationToken) {
        String eventDateTime = event.getStartTime().format(DATE_FORMATTER);
        String acceptUrl = baseUrl + "/api/events/respond?token=" + invitationToken + "&action=ACCEPT";
        String declineUrl = baseUrl + "/api/events/respond?token=" + invitationToken + "&action=DECLINE";

        StringBuilder body = new StringBuilder();
        
        body.append("<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>");
        body.append("<h2 style='color: #2c3e50;'>You're Invited to an Event!</h2>");
        body.append("<p>You have been invited to attend the following event:</p>");
        body.append("<div style='background-color: #f8f9fa; padding: 20px; border-left: 4px solid #3498db; margin: 20px 0;'>");
        body.append("<h3 style='color: #3498db; margin-top: 0;'>").append(event.getTitle()).append("</h3>");
        body.append("<p><strong>📅 Date & Time:</strong> ").append(eventDateTime).append("</p>");
        
        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            body.append("<p><strong>📍 Location:</strong> ").append(event.getLocation()).append("</p>");
        }
        
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            body.append("<p><strong>ℹ️ Description:</strong><br>").append(event.getDescription()).append("</p>");
        }
        
        body.append("<p><strong>👤 Organizer:</strong> ").append(event.getOrganizer().getFullName()).append("</p>");
        body.append("</div>");
        
        body.append("<p style='font-size: 16px; margin: 30px 0;'>Will you be attending?</p>");
        
        body.append("<div style='text-align: center; margin: 30px 0;'>");
        body.append("<a href='").append(acceptUrl).append("' style='display: inline-block; padding: 12px 30px; margin: 0 10px; background-color: #27ae60; color: white; text-decoration: none; border-radius: 5px; font-weight: bold;'>✓ Accept</a>");
        body.append("<a href='").append(declineUrl).append("' style='display: inline-block; padding: 12px 30px; margin: 0 10px; background-color: #e74c3c; color: white; text-decoration: none; border-radius: 5px; font-weight: bold;'>✗ Decline</a>");
        body.append("</div>");
        
        body.append("<p style='color: #7f8c8d; font-size: 14px; margin-top: 40px;'>Please respond as soon as possible so the organizer can plan accordingly.</p>");
        body.append("<hr style='border: none; border-top: 1px solid #eee; margin: 30px 0;'>");
        body.append("<p style='color: #7f8c8d; font-size: 12px;'>This is an automated invitation from Event Management System. Please do not reply to this email.</p>");
        body.append("</body></html>");
        
        return body.toString();
    }

    public boolean sendInvitationResponseConfirmation(Event event, String recipientEmail, boolean accepted) {
        try {
            log.info("[EmailService] INFO - Starting sendInvitationResponseConfirmation() to: {} for event: {}, accepted: {}", 
                     recipientEmail, event.getTitle(), accepted);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(recipientEmail);
            helper.setReplyTo(fromEmail);
            
            String subject = accepted 
                ? "Confirmed: " + event.getTitle() 
                : "Declined: " + event.getTitle();
            helper.setSubject(subject);
            
           
            message.setHeader("X-Priority", "3");
            message.setHeader("X-Mailer", "EventManagementSystem");

            String emailBody = buildResponseConfirmationEmailBody(event, accepted);
            helper.setText(emailBody, true);

            log.debug("[EmailService] DEBUG - Attempting to send response confirmation email...");
            try {
                mailSender.send(message);
                log.info("[EmailService] INFO - sendInvitationResponseConfirmation() - Response confirmation sent to " + recipientEmail + " for event '" + event.getTitle() + "' (eventId=" + event.getId() + ", accepted=" + accepted + ")");
                return true;
            } catch (Exception sendError) {
                log.error("[EmailService] ERROR - SMTP send failed for {}: {}", recipientEmail, sendError.getMessage());
                log.error("[EmailService] ERROR - Send error stacktrace: ", sendError);
                return false;
            }

        } catch (MessagingException e) {
            log.error("[EmailService] ERROR - sendInvitationResponseConfirmation() - MessagingException for recipient {}: {}", 
                     recipientEmail, e.getMessage());
            log.error("[EmailService] ERROR - Exception details:", e);
            return false;

        } catch (Exception e) {
            log.error("[EmailService] ERROR - sendInvitationResponseConfirmation() - Unexpected error for recipient: {}: {}", 
                     recipientEmail, e.getMessage());
            log.error("[EmailService] ERROR - Exception details:", e);
            return false;
        }
    }

    private String buildResponseConfirmationEmailBody(Event event, boolean accepted) {
        String eventDateTime = event.getStartTime().format(DATE_FORMATTER);

        StringBuilder body = new StringBuilder();
        
        body.append("<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>");
        
        if (accepted) {
            body.append("<h2 style='color: #27ae60;'>✓ You're Attending!</h2>");
            body.append("<p>Thank you for accepting the invitation. We're excited to see you at:</p>");
        } else {
            body.append("<h2 style='color: #e74c3c;'>Invitation Declined</h2>");
            body.append("<p>We've recorded that you won't be able to attend:</p>");
        }
        
        body.append("<div style='background-color: #f8f9fa; padding: 20px; border-left: 4px solid ").append(accepted ? "#27ae60" : "#e74c3c").append("; margin: 20px 0;'>");
        body.append("<h3 style='color: ").append(accepted ? "#27ae60" : "#e74c3c").append("; margin-top: 0;'>").append(event.getTitle()).append("</h3>");
        body.append("<p><strong>📅 Date & Time:</strong> ").append(eventDateTime).append("</p>");
        
        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            body.append("<p><strong>📍 Location:</strong> ").append(event.getLocation()).append("</p>");
        }
        
        if (event.getOrganizer() != null) {
            body.append("<p><strong>👤 Organizer:</strong> ").append(event.getOrganizer().getFullName()).append("</p>");
        }
        
        body.append("</div>");
        
        if (accepted) {
            body.append("<p>You will receive a reminder before the event starts. If you have any questions, please contact the organizer.</p>");
        } else {
            body.append("<p>The organizer has been notified of your response. If you change your mind, please contact the organizer directly.</p>");
        }
        
        body.append("<hr style='border: none; border-top: 1px solid #eee; margin: 30px 0;'>");
        body.append("<p style='color: #7f8c8d; font-size: 12px;'>This is an automated confirmation from Event Management System. Please do not reply to this email.</p>");
        body.append("</body></html>");
        
        return body.toString();
    }

    
    public boolean sendAutoAccountCredentials(String email, String fullName, String tempPassword) {
        try {
            log.info("[EmailService] INFO - 📧 sendAutoAccountCredentials() - Starting credentials email to: {}", email);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(email);
            helper.setReplyTo(fromEmail);
            helper.setSubject("Your Event Management Account Has Been Created");
            
            message.setHeader("X-Priority", "3");
            message.setHeader("X-Mailer", "EventManagementSystem");

            StringBuilder body = new StringBuilder();
            body.append("<html><body style=\"font-family: Arial, sans-serif; line-height: 1.6;\">");
            body.append("<h2>Welcome to Event Management System!</h2>");
            body.append("<p>Dear ").append(fullName).append(",</p>");
            body.append("<p>An account has been automatically created for you based on your event invitation acceptance.</p>");
            body.append("<h3>Your Account Credentials:</h3>");
            body.append("<table style=\"border-collapse: collapse;\">");
            body.append("<tr><td style=\"padding: 8px; border: 1px solid #ddd;\"><strong>Email:</strong></td><td style=\"padding: 8px; border: 1px solid #ddd;\">").append(email).append("</td></tr>");
            body.append("<tr><td style=\"padding: 8px; border: 1px solid #ddd;\"><strong>Temporary Password:</strong></td><td style=\"padding: 8px; border: 1px solid #ddd;\"><code style=\"background-color: #f0f0f0; padding: 5px;\">").append(tempPassword).append("</code></td></tr>");
            body.append("</table>");
            body.append("<p><strong>Important:</strong> Please change your password on first login for security.</p>");
            body.append("<p><a href=\"").append(baseUrl).append("/login\" style=\"display: inline-block; padding: 10px 20px; background-color: #007bff; color: white; text-decoration: none; border-radius: 5px;\">Login Here</a></p>");
            body.append("<p>If you did not expect this email or have any questions, please contact support.</p>");
            body.append("<p>Best regards,<br>Event Management System</p>");
            body.append("</body></html>");

            helper.setText(body.toString(), true);
            
            log.debug("[EmailService] DEBUG - Message prepared for {}", email);
            log.debug("[EmailService] DEBUG - Attempting to send auto-account credentials email via SMTP to: {}", email);
            
            long startTime = System.currentTimeMillis();
            try {
                mailSender.send(message);
                long duration = System.currentTimeMillis() - startTime;
                log.info("[EmailService] INFO - ✅ SUCCESS: Credentials email sent to {} ({}ms), login URL: {}/login", 
                         email, duration, baseUrl);
                return true;
            } catch (Exception smtpError) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("[EmailService] ERROR - ❌ SMTP delivery failed for {} ({}ms): {}", 
                         email, duration, smtpError.getMessage());
                log.error("[EmailService] ERROR - Error type: {}", smtpError.getClass().getSimpleName());
                log.error("[EmailService] ERROR - Stack trace: ", smtpError);
                return false;
            }

        } catch (MessagingException e) {
            log.error("[EmailService] ERROR - ❌ MessagingException building credentials email for {}: {}", 
                     email, e.getMessage());
            log.error("[EmailService] ERROR - Cause: {}", e.getCause());
            return false;
        } catch (Exception e) {
            log.error("[EmailService] ERROR - ❌ Unexpected error sending credentials to {}: {}", 
                     email, e.getMessage());
            log.error("[EmailService] ERROR - Stack trace: ", e);
            return false;
        }
    }

  
    public boolean sendWithRetry(java.util.function.Supplier<Boolean> emailSendTask, String recipientEmail, int maxAttempts) {
        int attempt = 1;
        long delayMs = 1000; 
        
        while (attempt <= maxAttempts) {
            try {
                log.debug("[EmailService] DEBUG - Attempt {}/{} to send email to: {}", attempt, maxAttempts, recipientEmail);
                
                if (emailSendTask.get()) {
                    log.info("[EmailService] INFO - Email sent successfully to {} on attempt {}", recipientEmail, attempt);
                    return true;
                }
                
                if (attempt >= maxAttempts) {
                    log.error("[EmailService] ERROR - Failed to send email to {} after {} attempts", recipientEmail, maxAttempts);
                    return false;
                }
                
                log.warn("[EmailService] WARN - Send failed for {}, retrying in {}ms (attempt {}/{})", 
                        recipientEmail, delayMs, attempt, maxAttempts);
                Thread.sleep(delayMs);
                delayMs *= 2; 
                attempt++;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[EmailService] ERROR - Retry interrupted for {}: {}", recipientEmail, e.getMessage());
                return false;
            } catch (Exception e) {
                log.error("[EmailService] ERROR - Unexpected error during retry for {}: {}", recipientEmail, e.getMessage());
                if (attempt >= maxAttempts) {
                    return false;
                }
                attempt++;
            }
        }
        
        return false;
    }

    
    public boolean sendWithRetry(java.util.function.Supplier<Boolean> emailSendTask, String recipientEmail) {
        return sendWithRetry(emailSendTask, recipientEmail, 5);
    }
}



