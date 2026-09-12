package com.docstorer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_metadata")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class DocumentMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false, unique = true)
    private Document document;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "author", length = 100)
    private String author;

    @Column(name = "subject", length = 255)
    private String subject;

    @Column(name = "keywords", length = 500)
    private String keywords;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "language", length = 20)
    private String language;

    @Column(name = "resolution", length = 20)
    private String resolution;

    @Column(name = "color_mode", length = 20)
    private String colorMode;

    @Column(name = "document_date")
    private LocalDateTime documentDate;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
