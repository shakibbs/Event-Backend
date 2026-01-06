package com.event_management_system.service;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
public class ApplicationLoggerService {

    // ==================== TRACE LEVEL ====================
    
    /**
     * Log at TRACE level (most verbose)
     * Use for: Variable values, detailed step-by-step execution
     */
    public void trace(String message) {
        log.trace(message);
    }
    
    public void trace(String message, Object... args) {
        log.trace(message, args);
    }

    // ==================== DEBUG LEVEL ====================
    
    /**
     * Log at DEBUG level
     * Use for: Technical details, method parameters, request data
     */
    public void debug(String message) {
        log.debug(message);
    }
    
    public void debug(String message, Object... args) {
        log.debug(message, args);
    }
    
    public void debug(String message, Exception exception) {
        log.debug(message, exception);
    }

   
    public void info(String message) {
        log.info(message);
    }
    
    public void info(String message, Object... args) {
        log.info(message, args);
    }

   
    public void warn(String message) {
        log.warn(message);
    }
    
    public void warn(String message, Object... args) {
        log.warn(message, args);
    }
    
    public void warn(String message, Exception exception) {
        log.warn(message, exception);
    }

  
    public void error(String message) {
        log.error(message);
    }
    
    public void error(String message, Object... args) {
        log.error(message, args);
    }
    
    public void error(String message, Exception exception) {
        log.error(message, exception);
    }

    // ==================== CONTEXTUAL LOGGING ====================
    
    /**
     * Log with context (controller/service name)
     * Format: [ControllerName] message
     */
    public void debugWithContext(String context, String message, Object... args) {
        log.debug("[{}] {}", context, formatMessage(message, args));
    }
    
    public void infoWithContext(String context, String message, Object... args) {
        log.info("[{}] {}", context, formatMessage(message, args));
    }
    
    public void warnWithContext(String context, String message, Object... args) {
        log.warn("[{}] {}", context, formatMessage(message, args));
    }
    
    public void errorWithContext(String context, String message, Exception exception) {
        log.error("[{}] {}", context, message, exception);
    }
    
    public void traceWithContext(String context, String message, Object... args) {
        log.trace("[{}] {}", context, formatMessage(message, args));
    }

    // ==================== UTILITY METHODS ====================
    
    /**
     * Format parameterized message
     * Replaces {} with arguments
     */
    private String formatMessage(String message, Object... args) {
        if (args.length == 0) {
            return message;
        }
        String result = message;
        for (Object arg : args) {
            result = result.replaceFirst("\\{\\}", String.valueOf(arg));
        }
        return result;
    }
    
    /**
     * Log method entry
     */
    public void methodEntry(String methodName) {
        log.debug("→ Entering {}", methodName);
    }
    
    /**
     * Log method exit
     */
    public void methodExit(String methodName) {
        log.debug("← Exiting {}", methodName);
    }
    
    /**
     * Log method exception
     */
    public void methodException(String methodName, Exception exception) {
        log.error("✗ Exception in {}: {}", methodName, exception.getMessage(), exception);
    }
}
