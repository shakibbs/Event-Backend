package com.event_management_system.mapper;

import com.event_management_system.dto.EventRequestDTO;
import com.event_management_system.dto.EventResponseDTO;
import com.event_management_system.entity.Event;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {
    
    public Event toEntity(EventRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Event event = new Event();
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setStartTime(dto.getStartTime());
        event.setEndTime(dto.getEndTime());
        event.setLocation(dto.getLocation());
        
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
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setLocation(entity.getLocation());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
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
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setLocation(dto.getLocation());
    }
}
