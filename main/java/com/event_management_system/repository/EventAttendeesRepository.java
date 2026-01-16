package com.event_management_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.event_management_system.entity.Event;
import com.event_management_system.entity.EventAttendees;
import com.event_management_system.entity.User;

@Repository
public interface EventAttendeesRepository extends JpaRepository<EventAttendees, Long> {

    Optional<EventAttendees> findByInvitationToken(String invitationToken);

    List<EventAttendees> findByEvent(Event event);

    List<EventAttendees> findByEventAndInvitationStatus(Event event, EventAttendees.InvitationStatus status);

    List<EventAttendees> findByUser(User user);

    List<EventAttendees> findByUserAndInvitationStatus(User user, EventAttendees.InvitationStatus status);

    boolean existsByEventAndEmail(Event event, String email);

    long countByEventAndInvitationStatus(Event event, EventAttendees.InvitationStatus status);
}
