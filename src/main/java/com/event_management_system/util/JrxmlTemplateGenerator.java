
package com.event_management_system.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;


public class JrxmlTemplateGenerator {

    public static void main(String[] args) throws Exception {
        String reportType = "event"; 
        String[] selectedFields;
        String[] fieldLabels;
        String dtoClassName;
        String reportTitle;
        switch (reportType) {
            case "user":
                dtoClassName = "com.event_management_system.dto.UserResponseDTO";
                selectedFields = new String[]{"fullName", "email", "role"};
                fieldLabels = new String[]{"Name", "Email", "User Role"};
                reportTitle = "Users List";
                break;
            case "activity":
                dtoClassName = "com.event_management_system.dto.UserActivityHistoryResponseDTO";
                selectedFields = new String[]{"activityTypeName", "username", "userGroup", "description", "activityDate", "ip"};
                fieldLabels = new String[]{"Activity Type", "Username", "Role", "Description", "Activity Date", "IP"};
                reportTitle = "Activity History";
                break;
            default:
                dtoClassName = "com.event_management_system.dto.EventResponseDTO";
                selectedFields = new String[]{"title", "startTime", "endTime", "location", "attendees", "visibility", "eventStatus"};
                fieldLabels = new String[]{"Event Name", "Start Date", "End Date", "Location", "Attendees", "Visibility", "Status"};
                reportTitle = "Events List";
        }
        String filePath = "src/main/resources/dynamic_export.jrxml";
        generateAndWriteJrxmlForFields(
            Class.forName(dtoClassName),
            selectedFields,
            fieldLabels,
            reportTitle,
            filePath
        );
        System.out.println("JRXML generated at: " + filePath);
    }

    public static void generateAndWriteJrxmlForFields(Class<?> dtoClass, String[] selectedFields, String[] fieldLabels, String reportTitle, String filePath) throws IOException {
        String jrxml = generateJrxmlForFields(dtoClass, selectedFields, fieldLabels, reportTitle);
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(jrxml);
        }
    }

    public static String generateJrxmlForFields(Class<?> dtoClass, String[] selectedFields, String[] fieldLabels, String reportTitle) {
        StringBuilder jrxml = new StringBuilder();
        String className = dtoClass.getSimpleName();
        jrxml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        jrxml.append("<jasperReport xmlns=\"http://jasperreports.sourceforge.net/jasperreports\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://jasperreports.sourceforge.net/jasperreports http://jasperreports.sourceforge.net/xsd/jasperreport.xsd\" name=\"")
            .append(className)
            .append("Report\" pageWidth=\"595\" pageHeight=\"842\" columnWidth=\"555\" leftMargin=\"20\" rightMargin=\"20\" topMargin=\"20\" bottomMargin=\"20\" uuid=\"")
            .append(java.util.UUID.randomUUID())
            .append("\">\n");

        for (String fieldName : selectedFields) {
            try {
                Field field = dtoClass.getDeclaredField(fieldName);
                jrxml.append("    <field name=\"").append(field.getName()).append("\" class=\"")
                    .append(field.getType().getName()).append("\"/>\n");
            } catch (NoSuchFieldException e) {
            }
        }

        jrxml.append("    <title>\n        <band height=\"60\">\n");
        jrxml.append("            <staticText>\n                <reportElement x=\"0\" y=\"0\" width=\"555\" height=\"30\"/>\n                <textElement textAlignment=\"Center\" verticalAlignment=\"Middle\">\n                    <font size=\"18\" isBold=\"true\"/>\n                </textElement>\n                <text><![CDATA[Event Flow]]></text>\n            </staticText>\n");
        jrxml.append("            <staticText>\n                <reportElement x=\"0\" y=\"30\" width=\"555\" height=\"20\"/>\n                <textElement textAlignment=\"Center\"/>\n                <text><![CDATA[Mohakhali, Dhaka]]></text>\n            </staticText>\n");
        jrxml.append("            <staticText>\n                <reportElement x=\"0\" y=\"50\" width=\"555\" height=\"10\"/>\n                <textElement textAlignment=\"Center\"/>");
        jrxml.append("                <text><![CDATA[").append(reportTitle).append("]]></text>\n            </staticText>\n");
        jrxml.append("        </band>\n    </title>\n");

        jrxml.append("    <columnHeader>\n        <band height=\"20\">\n");
        int x = 0;
        int colWidth = 555 / Math.max(selectedFields.length, 1);
        for (int i = 0; i < selectedFields.length; i++) {
            jrxml.append("            <staticText>\n                <reportElement x=\"").append(x).append("\" y=\"0\" width=\"")
                .append(colWidth).append("\" height=\"20\"/>");
            jrxml.append("\n                <box>\n                    <pen lineWidth=\"0.5\"/>\n                </box>\n                <textElement textAlignment=\"Center\" verticalAlignment=\"Middle\"/>");
            jrxml.append("\n                <text><![CDATA[").append(fieldLabels[i]).append("]]></text>\n            </staticText>\n");
            x += colWidth;
        }
        jrxml.append("        </band>\n    </columnHeader>\n");

        jrxml.append("    <detail>\n        <band height=\"20\">\n");
        x = 0;
        for (String fieldName : selectedFields) {
            jrxml.append("            <textField>\n                <reportElement x=\"").append(x).append("\" y=\"0\" width=\"")
                .append(colWidth).append("\" height=\"20\"/>");
            jrxml.append("\n                <box>\n                    <pen lineWidth=\"0.5\"/>\n                </box>\n                <textElement verticalAlignment=\"Middle\"/>");
            jrxml.append("\n                <textFieldExpression><![CDATA[$F{")
                .append(fieldName).append("}]]></textFieldExpression>\n            </textField>\n");
            x += colWidth;
        }
        jrxml.append("        </band>\n    </detail>\n");

        jrxml.append("    <pageFooter>\n        <band height=\"20\">\n");
        jrxml.append("            <textField pattern=\"yyyy-MM-dd HH:mm\">\n                <reportElement x=\"0\" y=\"0\" width=\"180\" height=\"20\"/>");
        jrxml.append("\n                <textElement verticalAlignment=\"Middle\"/>");
        jrxml.append("\n                <textFieldExpression><![CDATA[new java.util.Date()]]></textFieldExpression>\n            </textField>\n");
        jrxml.append("            <textField>\n                <reportElement x=\"180\" y=\"0\" width=\"100\" height=\"20\"/>");
        jrxml.append("\n                <textElement textAlignment=\"Center\" verticalAlignment=\"Middle\"/>");
        jrxml.append("\n                <textFieldExpression><![CDATA[\"Page: \" + $V{PAGE_NUMBER}]]></textFieldExpression>\n            </textField>\n");
        jrxml.append("            <staticText>\n                <reportElement x=\"280\" y=\"0\" width=\"275\" height=\"20\"/>");
        jrxml.append("\n                <textElement textAlignment=\"Right\" verticalAlignment=\"Middle\"/>");
        jrxml.append("\n                <text><![CDATA[bseventmanagement23@gmail.com]]></text>\n            </staticText>\n");
        jrxml.append("        </band>\n    </pageFooter>\n");

        jrxml.append("</jasperReport>\n");
        return jrxml.toString();
    }

        public static void generateAndWriteJrxml(Class<?> dtoClass, String reportTitle, String filePath) throws IOException {
            String jrxml = generateJrxmlForDto(dtoClass, reportTitle);
            File file = new File(filePath);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(jrxml);
            }
        }
    
    public static String generateJrxmlForDto(Class<?> dtoClass, String reportTitle) {
        StringBuilder jrxml = new StringBuilder();
        String className = dtoClass.getSimpleName();
        Field[] fields = dtoClass.getDeclaredFields();

        jrxml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        jrxml.append("<jasperReport xmlns=\"http://jasperreports.sourceforge.net/jasperreports\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://jasperreports.sourceforge.net/jasperreports http://jasperreports.sourceforge.net/xsd/jasperreport.xsd\" name=\"")
            .append(className)
            .append("Report\" pageWidth=\"595\" pageHeight=\"842\" columnWidth=\"555\" leftMargin=\"20\" rightMargin=\"20\" topMargin=\"20\" bottomMargin=\"20\" uuid=\"")
            .append(java.util.UUID.randomUUID())
            .append("\">\n");

        for (Field field : fields) {
            jrxml.append("    <field name=\"").append(field.getName()).append("\" class=\"")
                .append(field.getType().getName()).append("\"/>\n");
        }

        jrxml.append("    <title>\n        <band height=\"60\">\n");
        jrxml.append("            <staticText>\n                <reportElement x=\"0\" y=\"0\" width=\"555\" height=\"30\"/>\n                <textElement textAlignment=\"Center\" verticalAlignment=\"Middle\">\n                    <font size=\"18\" isBold=\"true\"/>\n                </textElement>\n                <text><![CDATA[Event Flow]]></text>\n            </staticText>\n");
        jrxml.append("            <staticText>\n                <reportElement x=\"0\" y=\"30\" width=\"555\" height=\"20\"/>\n                <textElement textAlignment=\"Center\"/>\n                <text><![CDATA[Mohakhali, Dhaka]]></text>\n            </staticText>\n");
        jrxml.append("            <staticText>\n                <reportElement x=\"0\" y=\"50\" width=\"555\" height=\"10\"/>\n                <textElement textAlignment=\"Center\"/>");
        jrxml.append("                <text><![CDATA[").append(reportTitle).append("]]></text>\n            </staticText>\n");
        jrxml.append("        </band>\n    </title>\n");

        jrxml.append("    <columnHeader>\n        <band height=\"20\">\n");
        int x = 0;
        int colWidth = 555 / Math.max(fields.length, 1);
        for (Field field : fields) {
            jrxml.append("            <staticText>\n                <reportElement x=\"").append(x).append("\" y=\"0\" width=\"")
                .append(colWidth).append("\" height=\"20\"/>");
            jrxml.append("\n                <box>\n                    <pen lineWidth=\"0.5\"/>");
            jrxml.append("\n                </box>\n                <textElement textAlignment=\"Center\" verticalAlignment=\"Middle\"/>");
            jrxml.append("\n                <text><![CDATA[").append(field.getName()).append("]]></text>\n            </staticText>\n");
            x += colWidth;
        }
        jrxml.append("        </band>\n    </columnHeader>\n");

        jrxml.append("    <detail>\n        <band height=\"20\">\n");
        x = 0;
        for (Field field : fields) {
            jrxml.append("            <textField>\n                <reportElement x=\"").append(x).append("\" y=\"0\" width=\"")
                .append(colWidth).append("\" height=\"20\"/>");
            jrxml.append("\n                <box>\n                    <pen lineWidth=\"0.5\"/>");
            jrxml.append("\n                </box>\n                <textElement verticalAlignment=\"Middle\"/>");
            jrxml.append("\n                <textFieldExpression><![CDATA[$F{")
                .append(field.getName()).append("}]]></textFieldExpression>\n            </textField>\n");
            x += colWidth;
        }
        jrxml.append("        </band>\n    </detail>\n");

        jrxml.append("    <pageFooter>\n        <band height=\"20\">\n");
        jrxml.append("            <textField pattern=\"yyyy-MM-dd HH:mm\">\n                <reportElement x=\"0\" y=\"0\" width=\"180\" height=\"20\"/>");
        jrxml.append("\n                <textElement verticalAlignment=\"Middle\"/>");
        jrxml.append("\n                <textFieldExpression><![CDATA[new java.util.Date()]]></textFieldExpression>\n            </textField>\n");
        jrxml.append("            <textField>\n                <reportElement x=\"180\" y=\"0\" width=\"100\" height=\"20\"/>");
        jrxml.append("\n                <textElement textAlignment=\"Center\" verticalAlignment=\"Middle\"/>");
        jrxml.append("\n                <textFieldExpression><![CDATA[\"Page: \" + $V{PAGE_NUMBER}]]></textFieldExpression>\n            </textField>\n");
        jrxml.append("            <staticText>\n                <reportElement x=\"280\" y=\"0\" width=\"275\" height=\"20\"/>");
        jrxml.append("\n                <textElement textAlignment=\"Right\" verticalAlignment=\"Middle\"/>");
        jrxml.append("\n                <text><![CDATA[bseventmanagement23@gmail.com]]></text>\n            </staticText>\n");
        jrxml.append("        </band>\n    </pageFooter>\n");

        jrxml.append("</jasperReport>\n");
        return jrxml.toString();
    }


}