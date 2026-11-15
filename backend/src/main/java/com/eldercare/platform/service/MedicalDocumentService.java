package com.eldercare.platform.service;

import com.eldercare.platform.dto.response.DocumentResponse;
import com.eldercare.platform.entity.MedicalDocument;
import com.eldercare.platform.entity.User;
import com.eldercare.platform.enums.DocumentCategory;
import com.eldercare.platform.enums.UserRole;
import com.eldercare.platform.exception.BadRequestException;
import com.eldercare.platform.exception.ResourceNotFoundException;
import com.eldercare.platform.exception.UnauthorizedException;
import com.eldercare.platform.repository.CaregiverRelationshipRepository;
import com.eldercare.platform.repository.MedicalDocumentRepository;
import com.eldercare.platform.repository.UserRepository;
import com.eldercare.platform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalDocumentService {

    private final MedicalDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final CaregiverRelationshipRepository relationshipRepository;
    private final DocumentStorageService storageService;

    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file,
                                           String title,
                                           String categoryStr,
                                           String documentDateStr,
                                           String doctorName,
                                           String hospitalName,
                                           String notes,
                                           UUID elderlyUserId,
                                           UserPrincipal principal) throws IOException {

        User uploader = userRepository.findById(principal.getId()).orElseThrow();
        User elderlyUser = resolveElderlyUser(principal, elderlyUserId);

        DocumentCategory category;
        try {
            category = DocumentCategory.valueOf(categoryStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid document category: " + categoryStr);
        }

        DocumentStorageService.StorageResult stored = storageService.store(file, elderlyUser.getId());

        LocalDate docDate = null;
        if (documentDateStr != null && !documentDateStr.isBlank()) {
            try {
                docDate = LocalDate.parse(documentDateStr);
            } catch (Exception e) {
                throw new BadRequestException("Invalid document date format. Use yyyy-MM-dd");
            }
        }

        MedicalDocument doc = MedicalDocument.builder()
                .elderlyUser(elderlyUser)
                .uploadedBy(uploader)
                .title(title)
                .category(category)
                .documentDate(docDate)
                .doctorName(doctorName)
                .hospitalName(hospitalName)
                .notes(notes)
                .fileName(stored.originalFileName())
                .fileStorageKey(stored.storageKey())
                .fileFormat(stored.fileFormat())
                .fileSizeBytes(stored.fileSizeBytes())
                .sha256Checksum(stored.sha256Checksum())
                .ocrStatus("NOT_PROCESSED")
                .deleted(false)
                .build();

        documentRepository.save(doc);
        log.info("Document uploaded: {} for elderly {}", doc.getId(), elderlyUser.getId());

        return toResponse(doc);
    }

    public List<DocumentResponse> getDocuments(UserPrincipal principal, UUID elderlyUserId) {
        User elderlyUser = resolveElderlyUser(principal, elderlyUserId);
        return documentRepository.findByElderlyUserIdNotDeleted(elderlyUser.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public DocumentResponse getDocument(UUID id, UserPrincipal principal) {
        MedicalDocument doc = documentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
        checkDocumentAccess(doc, principal);
        return toResponse(doc);
    }

    public byte[] downloadDocument(UUID id, UserPrincipal principal) throws IOException {
        MedicalDocument doc = documentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
        checkDocumentAccess(doc, principal);
        return storageService.retrieve(doc.getFileStorageKey());
    }

    @Transactional
    public DocumentResponse updateMetadata(UUID id,
                                            String title,
                                            String categoryStr,
                                            String documentDateStr,
                                            String doctorName,
                                            String hospitalName,
                                            String notes,
                                            UserPrincipal principal) {
        MedicalDocument doc = documentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
        checkDocumentOwnership(doc, principal);

        if (title != null) doc.setTitle(title);
        if (categoryStr != null) {
            try {
                doc.setCategory(DocumentCategory.valueOf(categoryStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid category");
            }
        }
        if (documentDateStr != null && !documentDateStr.isBlank()) {
            doc.setDocumentDate(LocalDate.parse(documentDateStr));
        }
        if (doctorName != null) doc.setDoctorName(doctorName);
        if (hospitalName != null) doc.setHospitalName(hospitalName);
        if (notes != null) doc.setNotes(notes);

        return toResponse(documentRepository.save(doc));
    }

    @Transactional
    public void deleteDocument(UUID id, UserPrincipal principal) throws IOException {
        MedicalDocument doc = documentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
        checkDocumentOwnership(doc, principal);

        doc.setDeleted(true);
        documentRepository.save(doc);
        log.info("Document soft-deleted: {}", id);
    }

    public List<DocumentResponse> searchDocuments(String query, UserPrincipal principal, UUID elderlyUserId) {
        User elderlyUser = resolveElderlyUser(principal, elderlyUserId);
        return documentRepository.searchDocuments(elderlyUser.getId(), query)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<DocumentResponse> getMedicalHistory(UserPrincipal principal, UUID elderlyUserId) {
        User elderlyUser = resolveElderlyUser(principal, elderlyUserId);
        return documentRepository.findByElderlyUserIdNotDeleted(elderlyUser.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private User resolveElderlyUser(UserPrincipal principal, UUID requestedId) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        if (user.getRole() == UserRole.ELDERLY) {
            return user;
        }
        if (requestedId == null) {
            if (user.getRole() == UserRole.ADMIN) {
                throw new BadRequestException("elderlyUserId is required for admin access");
            }
            List<com.eldercare.platform.entity.CaregiverRelationship> relationships = relationshipRepository
                    .findByRelatedUserIdAndActiveTrue(principal.getId());
            if (relationships.size() == 1) {
                UUID fallbackId = relationships.get(0).getElderlyUser().getId();
                return userRepository.findById(fallbackId)
                        .orElseThrow(() -> new ResourceNotFoundException("User", fallbackId));
            }
            if (relationships.size() > 1) {
                throw new BadRequestException("Multiple elderly profiles are connected; please specify elderlyUserId");
            }
            throw new BadRequestException("elderlyUserId is required");
        }
        boolean hasAccess = relationshipRepository
                .existsByElderlyUserIdAndRelatedUserIdAndActiveTrue(requestedId, principal.getId());
        if (!hasAccess && user.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("No authorized relationship");
        }
        return userRepository.findById(requestedId)
                .orElseThrow(() -> new ResourceNotFoundException("User", requestedId));
    }

    private void checkDocumentAccess(MedicalDocument doc, UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        UUID elderlyId = doc.getElderlyUser().getId();
        if (user.getRole() == UserRole.ELDERLY && !user.getId().equals(elderlyId)) {
            throw new UnauthorizedException("Access denied to this document");
        }
        if (user.getRole() != UserRole.ELDERLY && user.getRole() != UserRole.ADMIN) {
            boolean hasAccess = relationshipRepository
                    .existsByElderlyUserIdAndRelatedUserIdAndActiveTrue(elderlyId, principal.getId());
            if (!hasAccess) {
                throw new UnauthorizedException("No authorized access to this document");
            }
        }
    }

    private void checkDocumentOwnership(MedicalDocument doc, UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        if (user.getRole() == UserRole.ELDERLY && !user.getId().equals(doc.getElderlyUser().getId())) {
            throw new UnauthorizedException("Cannot modify this document");
        }
        if (user.getRole() == UserRole.CAREGIVER || user.getRole() == UserRole.FAMILY_MEMBER) {
            boolean hasAccess = relationshipRepository
                    .existsByElderlyUserIdAndRelatedUserIdAndActiveTrue(doc.getElderlyUser().getId(), principal.getId());
            if (!hasAccess) {
                throw new UnauthorizedException("No authorized access");
            }
        }
    }

    private DocumentResponse toResponse(MedicalDocument d) {
        return DocumentResponse.builder()
                .id(d.getId())
                .elderlyUserId(d.getElderlyUser().getId())
                .title(d.getTitle())
                .category(d.getCategory())
                .documentDate(d.getDocumentDate())
                .doctorName(d.getDoctorName())
                .hospitalName(d.getHospitalName())
                .notes(d.getNotes())
                .fileName(d.getFileName())
                .fileFormat(d.getFileFormat())
                .fileSizeBytes(d.getFileSizeBytes())
                .ocrStatus(d.getOcrStatus())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
