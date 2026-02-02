package com.event_management_system.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.event_management_system.entity.EventReminderSent;


@Repository
public interface EventReminderSentRepository extends JpaRepository<EventReminderSent, Long> {

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    
    Optional<EventReminderSent> findByEventIdAndUserId(Long eventId, Long userId);

   
}
