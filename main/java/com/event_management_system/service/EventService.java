package com.event_management_system.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.event_management_system.dto.EventRequestDTO;
import com.event_management_system.dto.EventResponseDTO;
import com.event_management_system.entity.BaseEntity;
import com.event_management_system.entity.Event;
import com.event_management_system.mapper.EventMapper;
import com.event_management_system.repository.EventRepository;

@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private EventMapper eventMapper;

    public EventResponseDTO createEvent(EventRequestDTO eventRequestDTO) {
        Event event = eventMapper.toEntity(eventRequestDTO);
        
        // Validate date range
        if (!BaseEntity.isDateRangeValid(event.getStartTime(), event.getEndTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        
        // Set audit fields for new entity
        event.recordCreation("system"); // You might want to get this from security context
        Event savedEvent = eventRepository.save(event);
        return eventMapper.toDto(savedEvent);
    }

    public Optional<EventResponseDTO> getEventById(long id) {
        return eventRepository.findById(id)
                .map(eventMapper::toDto);
    }

    public Optional<EventResponseDTO> updateEvent(long id, EventRequestDTO eventRequestDTO) {
        return eventRepository.findById(id).map(existingEvent -> {
            eventMapper.updateEntity(eventRequestDTO, existingEvent);
            
            // Validate date range
            if (!BaseEntity.isDateRangeValid(existingEvent.getStartTime(), existingEvent.getEndTime())) {
                throw new IllegalArgumentException("End time must be after start time");
            }
            
            // Update audit fields
            existingEvent.recordUpdate("system"); // You might want to get this from security context
            Event updatedEvent = eventRepository.save(existingEvent);
            return eventMapper.toDto(updatedEvent);
        });
    }

    public boolean deleteEvent(long id) {
        return eventRepository.findById(id).map(event -> {
            event.markDeleted();
            eventRepository.save(event);
            return true;
        }).orElse(false);
    }
    
    public Page<EventResponseDTO> getAllEvents(Pageable pageable) {
        Page<Event> events = eventRepository.findAll(pageable);
        return events.map(eventMapper::toDto);
    }
    
    public List<EventResponseDTO> getAllEventsList() {
        List<Event> events = eventRepository.findAll();
        return events.stream()
                .map(eventMapper::toDto)
                .collect(Collectors.toList());
    }
}
