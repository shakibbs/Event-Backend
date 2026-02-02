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


@Component
public class RequestInfoUtil {
    
  
    public String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }
        
        try {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
               
                String[] ips = xForwardedFor.split(",");
                String clientIp = ips[0].trim();
                if (isValidIp(clientIp)) {
                    return clientIp;
                }
            }
            
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty() && isValidIp(xRealIp)) {
                return xRealIp;
            }
            
            String cfConnectingIp = request.getHeader("Cf-Connecting-IP");
            if (cfConnectingIp != null && !cfConnectingIp.isEmpty() && isValidIp(cfConnectingIp)) {
                return cfConnectingIp;
            }
            
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
    
   
    private boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty() || ip.equals("unknown")) {
            return false;
        }
        
        String ipv4Pattern = "^(\\d{1,3}\\.){3}\\d{1,3}$";
        String ipv6Pattern = "^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$";
        
        Pattern p4 = Pattern.compile(ipv4Pattern);
        Pattern p6 = Pattern.compile(ipv6Pattern);
        
        return p4.matcher(ip).matches() || p6.matcher(ip).matches();
    }
    
   
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
    
   
    public String generateDeviceId(String ipAddress, String userAgent) {
        try {
            if (ipAddress == null || ipAddress.isEmpty()) {
                ipAddress = "0.0.0.0";
            }
            if (userAgent == null || userAgent.isEmpty()) {
                userAgent = "Unknown";
            }
            
            String combinedString = ipAddress + "|" + userAgent;
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combinedString.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return "device_" + hexString.toString().substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error generating device ID: " + e.getMessage());
            return "device_unknown";
        }
    }
    
    
    public String generateDeviceId(HttpServletRequest request) {
        if (request == null) {
            return "device_unknown";
        }
        
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        return generateDeviceId(ipAddress, userAgent);
    }
    
    
    public String generateDeviceId(String ipAddress) {
        return generateDeviceId(ipAddress, "default");
    }
    
   
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
