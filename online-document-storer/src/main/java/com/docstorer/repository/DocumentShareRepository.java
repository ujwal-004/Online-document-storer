package com.docstorer.repository;

import com.docstorer.entity.Document;
import com.docstorer.entity.DocumentShare;
import com.docstorer.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentShareRepository extends JpaRepository<DocumentShare, Long> {
    List<DocumentShare> findByDocument(Document document);
    List<DocumentShare> findBySharedWith(User user);
    boolean existsByDocumentAndSharedWith(Document document, User user);
}
