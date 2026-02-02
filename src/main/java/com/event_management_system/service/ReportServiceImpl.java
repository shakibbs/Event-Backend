package com.event_management_system.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.event_management_system.util.JrxmlTemplateGenerator;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Service
public class ReportServiceImpl implements ReportService {
    @Override
    public byte[] generateEventsPdf(List<?> events, Map<String, Object> parameters) throws JRException {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("Events list is empty");
        }
        Class<?> dtoClass = events.get(0).getClass();
        String[] selectedFields = {"title", "startTime", "endTime", "location", "attendees", "visibility", "eventStatus"};
        String[] fieldLabels = {"Event Name", "Start Date", "End Date", "Location", "Attendees", "Visibility", "Status"};
        String jrxml = JrxmlTemplateGenerator.generateJrxmlForFields(dtoClass, selectedFields, fieldLabels, "Events Report");
        InputStream jrxmlStream = new ByteArrayInputStream(jrxml.getBytes(StandardCharsets.UTF_8));
        JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(events);
        if (parameters == null) parameters = new HashMap<>();
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

    @Override
    public byte[] generateUsersPdf(List<?> users, Map<String, Object> parameters) throws JRException {
        if (users == null || users.isEmpty()) {
            throw new IllegalArgumentException("Users list is empty");
        }
        Class<?> dtoClass = users.get(0).getClass();
        String[] selectedFields = {"fullName", "email", "role"};
        String[] fieldLabels = {"Name", "Email", "User Role"};
        String jrxml = JrxmlTemplateGenerator.generateJrxmlForFields(dtoClass, selectedFields, fieldLabels, "Users List");
        InputStream jrxmlStream = new ByteArrayInputStream(jrxml.getBytes(StandardCharsets.UTF_8));
        JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(users);
        if (parameters == null) parameters = new HashMap<>();
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

    @Override
    public byte[] generateActivityPdf(List<?> activities, Map<String, Object> parameters) throws JRException {
        if (activities == null || activities.isEmpty()) {
            throw new IllegalArgumentException("Activity list is empty");
        }
        Class<?> dtoClass = activities.get(0).getClass();
        String[] selectedFields = {"activityTypeName", "username", "userGroup", "description", "activityDate", "ip"};
        String[] fieldLabels = {"Activity Type", "Username", "Role", "Description", "Activity Date", "IP"};
        String jrxml = JrxmlTemplateGenerator.generateJrxmlForFields(dtoClass, selectedFields, fieldLabels, "Activity History");
        InputStream jrxmlStream = new ByteArrayInputStream(jrxml.getBytes(StandardCharsets.UTF_8));
        JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(activities);
        if (parameters == null) parameters = new HashMap<>();
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }
}
