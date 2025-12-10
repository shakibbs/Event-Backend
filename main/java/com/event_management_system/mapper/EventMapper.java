package com.event_management_system.mapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.event_management_system.dto.EventRequestDTO;
import com.event_management_system.dto.EventResponseDTO;
import com.event_management_system.entity.Event;

@Component
public class EventMapper {
    
    public Event toEntity(EventRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Event event = new Event();
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        
        // Convert string dates to LocalDateTime
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
        
        // Convert LocalDateTime to string format
        if (entity.getStartTime() != null) {
            dto.setStartTime(entity.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (entity.getEndTime() != null) {
            dto.setEndTime(entity.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        
        dto.setLocation(entity.getLocation());
        dto.setVisibility(entity.getVisibility());
        
        // Convert LocalDateTime to string format
        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(entity.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (entity.getUpdatedAt() != null) {
            dto.setUpdatedAt(entity.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setStatus(entity.getStatus());
        dto.setDeleted(entity.getDeleted());
        
        return dto;
    }
    
    public void updateEntity(EventRequestDTO dto, Event entity) {
        if (dto == null || entity == null) {
            return;
        }
        
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        
        // Convert string dates to LocalDateTime
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
