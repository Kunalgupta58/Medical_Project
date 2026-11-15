package com.eldercare.platform.repository;

import com.eldercare.platform.entity.MedicalDocument;
import com.eldercare.platform.enums.DocumentCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicalDocumentRepository extends JpaRepository<MedicalDocument, UUID> {

    @Query("SELECT d FROM MedicalDocument d WHERE d.elderlyUser.id = :elderlyUserId " +
           "AND d.deleted = false ORDER BY d.documentDate DESC, d.createdAt DESC")
    List<MedicalDocument> findByElderlyUserIdNotDeleted(UUID elderlyUserId);

    @Query("SELECT d FROM MedicalDocument d WHERE d.elderlyUser.id = :elderlyUserId " +
           "AND d.deleted = false ORDER BY d.documentDate DESC, d.createdAt DESC")
    Page<MedicalDocument> findByElderlyUserIdNotDeleted(UUID elderlyUserId, Pageable pageable);

    @Query("SELECT d FROM MedicalDocument d WHERE d.elderlyUser.id = :elderlyUserId " +
           "AND d.category = :category AND d.deleted = false ORDER BY d.documentDate DESC")
    List<MedicalDocument> findByCategoryNotDeleted(UUID elderlyUserId, DocumentCategory category);

    @Query("SELECT d FROM MedicalDocument d WHERE d.elderlyUser.id = :elderlyUserId " +
           "AND d.deleted = false " +
           "AND (LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(d.doctorName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(d.hospitalName) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<MedicalDocument> searchDocuments(UUID elderlyUserId, String query);

    Optional<MedicalDocument> findByIdAndDeletedFalse(UUID id);

    long countByElderlyUserIdAndDeletedFalse(UUID elderlyUserId);

    @Query("SELECT d FROM MedicalDocument d WHERE d.elderlyUser.id = :elderlyUserId " +
           "AND d.deleted = false AND d.documentDate BETWEEN :from AND :to " +
           "ORDER BY d.documentDate DESC")
    List<MedicalDocument> findByDateRange(UUID elderlyUserId, LocalDate from, LocalDate to);
}
