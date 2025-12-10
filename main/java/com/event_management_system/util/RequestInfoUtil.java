package com.event_management_system.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * RequestInfoUtil
 * 
 * Utility class to extract information from HTTP requests
 * 
 * Features:
 * - Extract real client IP (handles proxies and load balancers)
 * - Parse User-Agent to get device/browser info
 * - Generate unique device ID
 * 
 * Purpose:
 * - Detect suspicious logins from new locations/devices
 * - Track device changes
 * - Security auditing
 */
@Component
public class RequestInfoUtil {
    
    /**
     * Get the real client IP address from request
     * 
     * Handles:
     * - Direct connections: 192.168.1.1
     * - Behind proxy: X-Forwarded-For header
     * - Behind load balancer: X-Real-IP header
     * 
     * Usage:
     * String clientIp = requestInfoUtil.getClientIpAddress(request);
     * → Returns: "192.168.1.1" or "115.42.33.55"
     * 
     * @param request - HTTP request
     * @return Client IP address as string
     */
    public String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }
        
        try {
            // Check X-Forwarded-For header (multiple proxies)
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                // X-Forwarded-For can have multiple IPs: "192.168.1.1, 10.0.0.1, 203.54.89.20"
                // First one is the client IP
                String[] ips = xForwardedFor.split(",");
                String clientIp = ips[0].trim();
                if (isValidIp(clientIp)) {
                    return clientIp;
                }
            }
            
            // Check X-Real-IP header (single proxy/load balancer)
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty() && isValidIp(xRealIp)) {
                return xRealIp;
            }
            
            // Check Cf-Connecting-IP (Cloudflare)
            String cfConnectingIp = request.getHeader("Cf-Connecting-IP");
            if (cfConnectingIp != null && !cfConnectingIp.isEmpty() && isValidIp(cfConnectingIp)) {
                return cfConnectingIp;
            }
            
            // Check if behind AWS ELB
            String realIp = request.getHeader("X-Aws-Cf-Source-Ip");
            if (realIp != null && !realIp.isEmpty() && isValidIp(realIp)) {
                return realIp;
            }
            
            // Fall back to direct connection IP
            String directIp = request.getRemoteAddr();
            if (isValidIp(directIp)) {
                return directIp;
            }
            
            return "0.0.0.0";
        } catch (Exception e) {
            System.err.println("Error getting client IP: " + e.getMessage());
            return "0.0.0.0";
        }
    }
    
    /**
     * Validate IP address format
     * 
     * @param ip - IP address to validate
     * @return true if valid IPv4/IPv6 format
     */
    private boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty() || ip.equals("unknown")) {
            return false;
        }
        
        // Simple validation - check if it looks like an IP
        // IPv4: xxx.xxx.xxx.xxx
        String ipv4Pattern = "^(\\d{1,3}\\.){3}\\d{1,3}$";
        // IPv6: starts with :
        String ipv6Pattern = "^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$";
        
        Pattern p4 = Pattern.compile(ipv4Pattern);
        Pattern p6 = Pattern.compile(ipv6Pattern);
        
        return p4.matcher(ip).matches() || p6.matcher(ip).matches();
    }
    
    /**
     * Parse User-Agent header to extract device/browser information
     * 
     * Usage:
     * Map<String, String> info = requestInfoUtil.parseUserAgent(request);
     * → Returns:
     *   {
     *     "browser": "Chrome",
     *     "browserVersion": "120.0",
     *     "os": "Windows",
     *     "osVersion": "10",
     *     "deviceType": "Desktop",
     *     "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)..."
     *   }
     * 
     * @param request - HTTP request
     * @return Map with browser, OS, and device type info
     */
    public Map<String, String> parseUserAgent(HttpServletRequest request) {
        Map<String, String> deviceInfo = new HashMap<>();
        
        if (request == null) {
            return getDefaultDeviceInfo();
        }
        
        try {
            String userAgent = request.getHeader("User-Agent");
            if (userAgent == null || userAgent.isEmpty()) {
                return getDefaultDeviceInfo();
            }
            
            deviceInfo.put("userAgent", userAgent);
            
            // Parse Browser
            if (userAgent.contains("Chrome")) {
                deviceInfo.put("browser", "Chrome");
                deviceInfo.put("browserVersion", extractVersion(userAgent, "Chrome/([\\d.]+)"));
            } else if (userAgent.contains("Safari")) {
                deviceInfo.put("browser", "Safari");
                deviceInfo.put("browserVersion", extractVersion(userAgent, "Safari/([\\d.]+)"));
            } else if (userAgent.contains("Firefox")) {
                deviceInfo.put("browser", "Firefox");
                deviceInfo.put("browserVersion", extractVersion(userAgent, "Firefox/([\\d.]+)"));
            } else if (userAgent.contains("Edge")) {
                deviceInfo.put("browser", "Edge");
                deviceInfo.put("browserVersion", extractVersion(userAgent, "Edge/([\\d.]+)"));
            } else if (userAgent.contains("MSIE") || userAgent.contains("Trident")) {
                deviceInfo.put("browser", "Internet Explorer");
                deviceInfo.put("browserVersion", extractVersion(userAgent, "MSIE ([\\d.]+)"));
            } else {
                deviceInfo.put("browser", "Unknown");
                deviceInfo.put("browserVersion", "Unknown");
            }
            
            // Parse Operating System
            if (userAgent.contains("Windows NT 10.0")) {
                deviceInfo.put("os", "Windows");
                deviceInfo.put("osVersion", "10");
            } else if (userAgent.contains("Windows NT 6.3")) {
                deviceInfo.put("os", "Windows");
                deviceInfo.put("osVersion", "8.1");
            } else if (userAgent.contains("Windows NT 6.2")) {
                deviceInfo.put("os", "Windows");
                deviceInfo.put("osVersion", "8");
            } else if (userAgent.contains("Windows")) {
                deviceInfo.put("os", "Windows");
                deviceInfo.put("osVersion", "Unknown");
            } else if (userAgent.contains("Mac OS X")) {
                deviceInfo.put("os", "macOS");
                deviceInfo.put("osVersion", extractVersion(userAgent, "Mac OS X ([\\d_]+)").replace("_", "."));
            } else if (userAgent.contains("Android")) {
                deviceInfo.put("os", "Android");
                deviceInfo.put("osVersion", extractVersion(userAgent, "Android ([\\d.]+)"));
            } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
                deviceInfo.put("os", "iOS");
                deviceInfo.put("osVersion", extractVersion(userAgent, "OS ([\\d_]+)").replace("_", "."));
            } else if (userAgent.contains("Linux")) {
                deviceInfo.put("os", "Linux");
                deviceInfo.put("osVersion", "Unknown");
            } else {
                deviceInfo.put("os", "Unknown");
                deviceInfo.put("osVersion", "Unknown");
            }
            
            // Parse Device Type
            if (userAgent.contains("Mobile") || userAgent.contains("Android") || userAgent.contains("iPhone")) {
                deviceInfo.put("deviceType", "Mobile");
            } else if (userAgent.contains("Tablet") || userAgent.contains("iPad")) {
                deviceInfo.put("deviceType", "Tablet");
            } else {
                deviceInfo.put("deviceType", "Desktop");
            }
            
            return deviceInfo;
        } catch (Exception e) {
            System.err.println("Error parsing User-Agent: " + e.getMessage());
            return getDefaultDeviceInfo();
        }
    }
    
    /**
     * Extract version number from User-Agent string using regex
     * 
     * @param userAgent - Full User-Agent string
     * @param pattern - Regex pattern to extract version
     * @return Version string or "Unknown"
     */
    private String extractVersion(String userAgent, String pattern) {
        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(userAgent);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            System.err.println("Error extracting version: " + e.getMessage());
        }
        return "Unknown";
    }
    
    /**
     * Get default device info
     * 
     * @return Map with default values
     */
    private Map<String, String> getDefaultDeviceInfo() {
        Map<String, String> defaultInfo = new HashMap<>();
        defaultInfo.put("browser", "Unknown");
        defaultInfo.put("browserVersion", "Unknown");
        defaultInfo.put("os", "Unknown");
        defaultInfo.put("osVersion", "Unknown");
        defaultInfo.put("deviceType", "Unknown");
        defaultInfo.put("userAgent", "Unknown");
        return defaultInfo;
    }
    
    /**
     * Generate unique device ID from IP address and User-Agent
     * 
     * Device ID is a hash of IP + User-Agent
     * Same device will always generate same ID
     * Different device will generate different ID
     * 
     * Usage:
     * String deviceId = requestInfoUtil.generateDeviceId(ipAddress, userAgent);
     * → Returns: "device_abc123def456"
     * 
     * @param ipAddress - Client IP address
     * @param userAgent - User-Agent string
     * @return Unique device ID
     */
    public String generateDeviceId(String ipAddress, String userAgent) {
        try {
            if (ipAddress == null || ipAddress.isEmpty()) {
                ipAddress = "0.0.0.0";
            }
            if (userAgent == null || userAgent.isEmpty()) {
                userAgent = "Unknown";
            }
            
            // Combine IP + User-Agent
            String combinedString = ipAddress + "|" + userAgent;
            
            // Hash it using SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combinedString.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            // Return first 16 characters of hash (enough for uniqueness)
            return "device_" + hexString.toString().substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error generating device ID: " + e.getMessage());
            return "device_unknown";
        }
    }
    
    /**
     * Overload: Generate device ID from request directly
     * 
     * @param request - HTTP request
     * @return Unique device ID
     */
    public String generateDeviceId(HttpServletRequest request) {
        if (request == null) {
            return "device_unknown";
        }
        
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        return generateDeviceId(ipAddress, userAgent);
    }
    
    /**
     * Overload: Generate device ID from IP only (simple version)
     * 
     * @param ipAddress - Client IP address
     * @return Device ID based on IP hash
     */
    public String generateDeviceId(String ipAddress) {
        return generateDeviceId(ipAddress, "default");
    }
    
    /**
     * Get complete device information from request
     * 
     * Combines IP, browser, OS, and device type
     * 
     * Usage:
     * String deviceInfo = requestInfoUtil.getCompleteDeviceInfo(request);
     * → Returns: "Chrome 120 on Windows 10 (Desktop) from 192.168.1.1"
     * 
     * @param request - HTTP request
     * @return Human-readable device info string
     */
    public String getCompleteDeviceInfo(HttpServletRequest request) {
        try {
            String ipAddress = getClientIpAddress(request);
            Map<String, String> deviceInfo = parseUserAgent(request);
            
            String browser = deviceInfo.getOrDefault("browser", "Unknown");
            String browserVersion = deviceInfo.getOrDefault("browserVersion", "Unknown");
            String os = deviceInfo.getOrDefault("os", "Unknown");
            String osVersion = deviceInfo.getOrDefault("osVersion", "Unknown");
            String deviceType = deviceInfo.getOrDefault("deviceType", "Unknown");
            
            return String.format(
                "%s %s on %s %s (%s) from %s",
                browser, browserVersion, os, osVersion, deviceType, ipAddress
            );
        } catch (Exception e) {
            System.err.println("Error getting device info: " + e.getMessage());
            return "Unknown device";
        }
    }
}
