package com.eldercare.platform.dto.response;

import com.eldercare.platform.enums.DocumentCategory;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class DocumentResponse {
    private UUID id;
    private UUID elderlyUserId;
    private String title;
    private DocumentCategory category;
    private LocalDate documentDate;
    private String doctorName;
    private String hospitalName;
    private String notes;
    private String fileName;
    private String fileFormat;
    private long fileSizeBytes;
    private String ocrStatus;
    private Instant createdAt;
    private Instant updatedAt;
}
