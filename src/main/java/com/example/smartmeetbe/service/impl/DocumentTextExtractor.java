package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.constant.ErrorCode;
import com.example.smartmeetbe.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.sl.extractor.SlideShowExtractor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Trích xuất text thô từ các định dạng tài liệu được phép upload
 * (pdf, doc, docx, txt, ppt, pptx) để đưa vào Gemini tóm tắt.
 */
@Slf4j
@Component
public class DocumentTextExtractor {

    public String extractText(InputStream inputStream, String fileType) {
        String type = fileType == null ? "" : fileType.toLowerCase();
        try (InputStream in = inputStream) {
            return switch (type) {
                case "pdf" -> extractPdf(in);
                case "docx" -> extractDocx(in);
                case "doc" -> extractDoc(in);
                case "pptx" -> extractPptx(in);
                case "ppt" -> extractPpt(in);
                case "txt" -> new String(in.readAllBytes(), StandardCharsets.UTF_8);
                default -> throw new AppException(ErrorCode.DOCUMENT_INVALID);
            };
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to extract text from {} document: {}", type, e.getMessage(), e);
            throw new AppException(ErrorCode.DOCUMENT_INVALID);
        }
    }

    private String extractPdf(InputStream in) throws IOException {
        try (PDDocument document = PDDocument.load(in)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractDocx(InputStream in) throws IOException {
        try (XWPFDocument document = new XWPFDocument(in);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractDoc(InputStream in) throws IOException {
        try (HWPFDocument document = new HWPFDocument(in);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractPptx(InputStream in) throws IOException {
        try (XMLSlideShow slideShow = new XMLSlideShow(in);
             SlideShowExtractor<?, ?> extractor = new SlideShowExtractor<>(slideShow)) {
            return extractor.getText();
        }
    }

    private String extractPpt(InputStream in) throws IOException {
        try (HSLFSlideShow slideShow = new HSLFSlideShow(in);
             SlideShowExtractor<?, ?> extractor = new SlideShowExtractor<>(slideShow)) {
            return extractor.getText();
        }
    }
}
