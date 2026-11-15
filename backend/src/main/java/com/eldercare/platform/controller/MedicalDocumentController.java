package com.eldercare.platform.controller;

import com.eldercare.platform.dto.response.DocumentResponse;
import com.eldercare.platform.security.UserPrincipal;
import com.eldercare.platform.service.MedicalDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/medical-documents")
@RequiredArgsConstructor
@Tag(name = "Medical Vault", description = "Medical document upload, retrieval, and management")
public class MedicalDocumentController {

    private final MedicalDocumentService documentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a medical document")
    public ResponseEntity<DocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("category") String category,
            @RequestParam(value = "documentDate", required = false) String documentDate,
            @RequestParam(value = "doctorName", required = false) String doctorName,
            @RequestParam(value = "hospitalName", required = false) String hospitalName,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "elderlyUserId", required = false) UUID elderlyUserId,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {

        DocumentResponse response = documentService.uploadDocument(
                file, title, category, documentDate, doctorName, hospitalName, notes, elderlyUserId, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List medical documents")
    public ResponseEntity<List<DocumentResponse>> list(
            @RequestParam(value = "elderlyUserId", required = false) UUID elderlyUserId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.getDocuments(principal, elderlyUserId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document metadata")
    public ResponseEntity<DocumentResponse> get(@PathVariable UUID id,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.getDocument(id, principal));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download document file")
    public ResponseEntity<byte[]> download(@PathVariable UUID id,
                                            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        DocumentResponse doc = documentService.getDocument(id, principal);
        byte[] bytes = documentService.downloadDocument(id, principal);

        String contentType = switch (doc.getFileFormat().toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            default -> "application/octet-stream";
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getFileName() + "\"")
                .body(bytes);
    }

    @PutMapping("/{id}/metadata")
    @Operation(summary = "Update document metadata")
    public ResponseEntity<DocumentResponse> updateMetadata(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.updateMetadata(
                id,
                body.get("title"),
                body.get("category"),
                body.get("documentDate"),
                body.get("doctorName"),
                body.get("hospitalName"),
                body.get("notes"),
                principal));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID id,
                                                       @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        documentService.deleteDocument(id, principal);
        return ResponseEntity.ok(Map.of("message", "Document deleted"));
    }

    @GetMapping("/history")
    @Operation(summary = "Medical history timeline")
    public ResponseEntity<List<DocumentResponse>> history(
            @RequestParam(value = "elderlyUserId", required = false) UUID elderlyUserId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.getMedicalHistory(principal, elderlyUserId));
    }

    @GetMapping("/search")
    @Operation(summary = "Search medical documents")
    public ResponseEntity<List<DocumentResponse>> search(
            @RequestParam("q") String query,
            @RequestParam(value = "elderlyUserId", required = false) UUID elderlyUserId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.searchDocuments(query, principal, elderlyUserId));
    }
}
