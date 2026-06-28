package com.example.smartmeetbe.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final SpringTemplateEngine templateEngine;

    public byte[] generateMeetingPdf(String meetingType, Map<String, Object> data) throws Exception {
        // 1. Đổ dữ liệu vào Thymeleaf Context
        Context context = new Context();
        context.setVariables(data);

        // 2. Định vị template phù hợp dựa trên loại cuộc họp (Scrum, Brainstorming, ...)
        String templateName = "pdf/" + meetingType.toLowerCase() + "_meeting";

        // 3. Render HTML thành String chứa dữ liệu thực tế
        String htmlContent = templateEngine.process(templateName, context);

        // 4. Cấu hình chuyển đổi HTML -> PDF
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContent, null);

            // Đăng ký Font Roboto hỗ trợ tiếng Việt UTF-8
            File fontFile = getFontFile();
            if (fontFile != null && fontFile.exists()) {
                builder.useFont(fontFile, "Roboto");
            } else {
                log.warn("Roboto font file is missing. Fallback to default system font (which may cause UTF-8 issues).");
            }

            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        }
    }

    /**
     * Helper để trích xuất font Roboto-Regular.ttf ra file tạm (temp file)
     * Đảm bảo hoạt động tốt cả khi chạy local lẫn khi đóng gói file JAR trên Remote server.
     */
    private File getFontFile() {
        try {
            // 1. Ưu tiên tìm file local trực tiếp (rất tốt khi chạy trong IDE để debug/test local)
            File localFont = new File("src/main/resources/fonts/Roboto-Regular.ttf");
            if (localFont.exists()) {
                log.info("Using local font file directly: {}", localFont.getAbsolutePath());
                return localFont;
            }

            // 2. Fallback tìm trong target classes (nếu chạy từ target build)
            File targetFont = new File("target/classes/fonts/Roboto-Regular.ttf");
            if (targetFont.exists()) {
                log.info("Using target font file directly: {}", targetFont.getAbsolutePath());
                return targetFont;
            }

            // 3. Nếu chạy từ tệp JAR (classloader resources), copy ra file tạm
            try (InputStream is = getClass().getResourceAsStream("/fonts/Roboto-Regular.ttf")) {
                if (is == null) {
                    log.error("Font Roboto-Regular.ttf not found in classpath or local resources.");
                    return null;
                }

                File tempFontFile = File.createTempFile("Roboto-Regular", ".ttf");
                tempFontFile.deleteOnExit();

                try (FileOutputStream os = new FileOutputStream(tempFontFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }
                log.info("Extracted font from JAR resources to temp file: {}", tempFontFile.getAbsolutePath());
                return tempFontFile;
            }
        } catch (Exception e) {
            log.error("Error loading and creating temp font file: {}", e.getMessage(), e);
            return null;
        }
    }
}
