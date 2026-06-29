package com.example.smartmeetbe.service;

public interface MeetingExportService {
    byte[] exportSummaryPdf(String roomId) throws Exception;

    byte[] exportSummaryDocx(String roomId) throws Exception;
}
