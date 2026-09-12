package com.docstorer.repository;

import com.docstorer.entity.Document;
import com.docstorer.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Page<Document> findByOwnerAndStatusNot(User owner, Document.DocumentStatus status, Pageable pageable);

    Page<Document> findByOwner(User owner, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.owner = :owner AND d.status != 'DELETED' AND " +
           "(LOWER(d.originalName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Document> searchByKeyword(@Param("owner") User owner,
                                   @Param("keyword") String keyword,
                                   Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.owner = :owner AND d.status != 'DELETED' AND " +
           "d.fileType = :fileType")
    Page<Document> findByOwnerAndFileType(@Param("owner") User owner,
                                          @Param("fileType") String fileType,
                                          Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.owner = :owner AND d.status != 'DELETED' AND " +
           "d.createdAt BETWEEN :startDate AND :endDate")
    Page<Document> findByOwnerAndDateRange(@Param("owner") User owner,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate,
                                           Pageable pageable);

    Optional<Document> findByShareToken(String shareToken);

    List<Document> findByOwnerAndFolderIsNull(User owner);

    @Query("SELECT SUM(d.fileSize) FROM Document d WHERE d.owner = :owner AND d.status != 'DELETED'")
    Long sumFileSizeByOwner(@Param("owner") User owner);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.status != 'DELETED'")
    long countActiveDocs();

    @Query("SELECT d FROM Document d WHERE d.status != 'DELETED' ORDER BY d.createdAt DESC")
    List<Document> findRecentDocuments(Pageable pageable);

    @Query("SELECT d.fileType, COUNT(d) FROM Document d WHERE d.owner = :owner AND d.status != 'DELETED' GROUP BY d.fileType")
    List<Object[]> countByFileTypeForOwner(@Param("owner") User owner);

    @Query("SELECT d FROM Document d WHERE d.isShared = true AND d.shareToken = :token AND " +
           "(d.shareExpiry IS NULL OR d.shareExpiry > :now)")
    Optional<Document> findActiveSharedDocument(@Param("token") String token, @Param("now") LocalDateTime now);
}
