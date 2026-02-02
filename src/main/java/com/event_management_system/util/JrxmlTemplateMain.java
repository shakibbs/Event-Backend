package com.event_management_system.util;

public class JrxmlTemplateMain {
    public static void main(String[] args) throws Exception {
        // Change the DTO and title as needed
        Class<?> dtoClass = Class.forName("com.event_management_system.dto.UserActivityHistoryResponseDTO");
        String reportTitle = "Activity History Report";
        String filePath = "src/main/resources/dynamic_export.jrxml";
        JrxmlTemplateGenerator.generateAndWriteJrxml(dtoClass, reportTitle, filePath);
        System.out.println("JRXML generated at: " + filePath);
    }
}
