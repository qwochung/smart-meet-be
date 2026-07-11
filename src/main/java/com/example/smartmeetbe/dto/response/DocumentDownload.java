package com.example.smartmeetbe.dto.response;

import java.io.InputStream;

/** Kết quả tải tài liệu: stream nội dung kèm tên file gốc để set Content-Disposition. */
public record DocumentDownload(InputStream stream, String fileName, long fileSize) {
}
