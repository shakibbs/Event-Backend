package com.event_management_system.service;

import java.util.List;
import java.util.Map;

import net.sf.jasperreports.engine.JRException;

public interface ReportService {
    byte[] generateEventsPdf(List<?> events, Map<String, Object> parameters) throws JRException;
    byte[] generateUsersPdf(List<?> users, Map<String, Object> parameters) throws JRException;
    byte[] generateActivityPdf(List<?> activities, Map<String, Object> parameters) throws JRException;
}