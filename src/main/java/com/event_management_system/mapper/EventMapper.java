
package com.event_management_system.mapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.event_management_system.dto.EventRequestDTO;
import com.event_management_system.dto.EventResponseDTO;
import com.event_management_system.entity.Event;
import com.event_management_system.entity.EventAttendees;
import com.event_management_system.repository.EventAttendeesRepository;

@Component
public class EventMapper {

    @Autowired
    private EventAttendeesRepository eventAttendeesRepository;

    public Event toEntity(EventRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        Event event = new Event();
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        if (dto.getStartTime() != null) {
            event.setStartTime(LocalDateTime.parse(dto.getStartTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (dto.getEndTime() != null) {
            event.setEndTime(LocalDateTime.parse(dto.getEndTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        event.setLocation(dto.getLocation());
        event.setVisibility(dto.getVisibility());
        return event;
    }

    public EventResponseDTO toDto(Event entity) {
        if (entity == null) {
            return null;
        }

        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        if (entity.getStartTime() != null) {
            dto.setStartTime(entity.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (entity.getEndTime() != null) {
            dto.setEndTime(entity.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        dto.setLocation(entity.getLocation());
        dto.setVisibility(entity.getVisibility());
        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(entity.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (entity.getUpdatedAt() != null) {
            dto.setUpdatedAt(entity.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setApprovalStatus(entity.getApprovalStatus());
        if (entity.getApprovedBy() != null) {
            dto.setApprovedById(entity.getApprovedBy().getId());
            dto.setApprovedByName(entity.getApprovedBy().getFullName());
        }
        if (entity.getApprovedAt() != null) {
            dto.setApprovedAt(entity.getApprovedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        dto.setEventStatus(entity.getCurrentEventStatus());
        dto.setDeleted(entity.getDeleted());
        // Set attendee count (accepted only)
        if (entity.getId() != null) {
            long attendeeCount = eventAttendeesRepository.countByEventAndInvitationStatus(entity, EventAttendees.InvitationStatus.ACCEPTED);
            dto.setAttendees(attendeeCount);
        } else {
            dto.setAttendees(0L);
        }
        return dto;
    }

    public void updateEntity(EventRequestDTO dto, Event entity) {
        if (dto == null || entity == null) {
            return;
        }
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        if (dto.getStartTime() != null) {
            entity.setStartTime(LocalDateTime.parse(dto.getStartTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (dto.getEndTime() != null) {
            entity.setEndTime(LocalDateTime.parse(dto.getEndTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        entity.setLocation(dto.getLocation());
        if (dto.getVisibility() != null) {
            entity.setVisibility(dto.getVisibility());
        }
    }
}
