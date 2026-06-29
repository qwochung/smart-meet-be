package com.example.smartmeetbe.service;

import com.example.smartmeetbe.document.MeetingSummary;
import com.example.smartmeetbe.dto.response.MasterMeetingSummaryDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/**
 * Sinh biên bản cuộc họp dưới định dạng Word (.docx) bằng Apache POI.
 * Dùng chung cho mọi loại cuộc họp vì cấu trúc dữ liệu tóm tắt giống nhau;
 * nhận cùng templateData mà {@code MeetingExportServiceImpl} đã dựng cho PDF.
 */
@Slf4j
@Service
public class DocxExportService {

    @SuppressWarnings("unchecked")
    public byte[] generateMeetingDocx(Map<String, Object> data) throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            String meetingName = stringOf(data.get("meetingName"), "Biên bản cuộc họp");
            addTitle(document, meetingName);

            addMeta(document, "Thời gian: ", stringOf(data.get("meetingDate"), "Chưa cập nhật"));
            addMeta(document, "Chủ trì: ", stringOf(data.get("hostName"), "Chưa cập nhật"));

            Object attendeesObj = data.get("attendees");
            if (attendeesObj instanceof List<?> attendees && !attendees.isEmpty()) {
                addMeta(document, "Thành viên tham dự: ", String.join(", ",
                        attendees.stream().map(String::valueOf).toList()));
            }

            MeetingSummary summary = (MeetingSummary) data.get("summary");
            if (summary != null) {
                writeSummary(document, summary);
            }

            document.write(out);
            return out.toByteArray();
        }
    }

    private void writeSummary(XWPFDocument doc, MeetingSummary s) {
        addParagraphSection(doc, "Tổng kết điều hành", s.getExecutiveSummary());
        addBulletSection(doc, "Chủ đề thảo luận", s.getDiscussionTopics());
        addBulletSection(doc, "Các quyết định", s.getDecisionsMade());

        List<MasterMeetingSummaryDto.ActionItemDto> actions = s.getActionItems();
        if (actions != null && !actions.isEmpty()) {
            addHeading(doc, "Công việc theo dõi");
            XWPFTable table = doc.createTable();
            setRowText(table.getRow(0), "Công việc", "Phụ trách", "Hạn");
            for (MasterMeetingSummaryDto.ActionItemDto item : actions) {
                XWPFTableRow row = table.createRow();
                setRowText(row,
                        nullSafe(item.getTask()),
                        nullSafe(item.getAssignee()),
                        item.getDeadline() == null || item.getDeadline().isBlank() ? "—" : item.getDeadline());
            }
        }

        List<MasterMeetingSummaryDto.QaPairDto> qa = s.getQaPairs();
        if (qa != null && !qa.isEmpty()) {
            addHeading(doc, "Hỏi & Đáp");
            for (MasterMeetingSummaryDto.QaPairDto pair : qa) {
                addLabeled(doc, "Hỏi: ", nullSafe(pair.getQuestion()));
                addLabeled(doc, "Đáp: ", nullSafe(pair.getAnswer()));
            }
        }

        addBulletSection(doc, "Điểm khó khăn", s.getPainPoints());

        List<MasterMeetingSummaryDto.ProConDto> pcs = s.getProsAndCons();
        if (pcs != null && !pcs.isEmpty()) {
            addHeading(doc, "Ưu / Nhược điểm");
            for (MasterMeetingSummaryDto.ProConDto pc : pcs) {
                addLabeled(doc, "Ý tưởng: ", nullSafe(pc.getIdea()));
                addLabeled(doc, "Ưu điểm: ", nullSafe(pc.getPros()));
                addLabeled(doc, "Nhược điểm: ", nullSafe(pc.getCons()));
            }
        }
    }

    private void addTitle(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(18);
        run.addBreak();
    }

    private void addMeta(XWPFDocument doc, String label, String value) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun labelRun = p.createRun();
        labelRun.setBold(true);
        labelRun.setText(label);
        XWPFRun valueRun = p.createRun();
        valueRun.setText(value);
    }

    private void addHeading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(200);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(14);
    }

    private void addParagraphSection(XWPFDocument doc, String heading, String body) {
        if (body == null || body.isBlank()) return;
        addHeading(doc, heading);
        XWPFParagraph p = doc.createParagraph();
        p.createRun().setText(body);
    }

    private void addBulletSection(XWPFDocument doc, String heading, List<String> items) {
        if (items == null || items.isEmpty()) return;
        addHeading(doc, heading);
        for (String item : items) {
            XWPFParagraph p = doc.createParagraph();
            p.setIndentationLeft(360);
            p.createRun().setText("• " + nullSafe(item));
        }
    }

    private void addLabeled(XWPFDocument doc, String label, String value) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun labelRun = p.createRun();
        labelRun.setBold(true);
        labelRun.setText(label);
        XWPFRun valueRun = p.createRun();
        valueRun.setText(value);
    }

    private void setRowText(XWPFTableRow row, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (row.getCell(i) == null) {
                row.addNewTableCell();
            }
            row.getCell(i).setText(cells[i]);
        }
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private String stringOf(Object o, String fallback) {
        return o == null ? fallback : String.valueOf(o);
    }
}
