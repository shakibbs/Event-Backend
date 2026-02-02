package com.event_management_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "event_reminder_sent")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventReminderSent {

    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

   
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    
    @Column(name = "email_sent_to", length = 255)
    private String emailSentTo;

    
    @Column(name = "delivery_status", length = 20)
    private String deliveryStatus;

    
    @PrePersist
    protected void onCreate() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
        if (deliveryStatus == null) {
            deliveryStatus = "SENT";
        }
    }
}
