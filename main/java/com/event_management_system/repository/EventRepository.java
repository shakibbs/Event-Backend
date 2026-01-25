package com.event_management_system.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.event_management_system.entity.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    
    Page<Event> findAllByDeletedFalse(Pageable pageable);
    
 
    List<Event> findAllByDeletedFalse();

    // Find all public, upcoming, not-deleted events
    List<Event> findByVisibilityAndEventStatusAndDeletedFalse(Event.Visibility visibility, Event.EventStatus eventStatus);
}