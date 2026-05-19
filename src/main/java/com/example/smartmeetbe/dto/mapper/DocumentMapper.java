package com.example.smartmeetbe.dto.mapper;

import com.example.smartmeetbe.dto.response.DocumentResponse;
import com.example.smartmeetbe.entity.Document;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentMapper {
    DocumentResponse toResponse(Document document);
    Document toDocument(DocumentResponse response);
}

