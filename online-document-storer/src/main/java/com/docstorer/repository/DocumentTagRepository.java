package com.docstorer.repository;

import com.docstorer.entity.Document;
import com.docstorer.entity.DocumentTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentTagRepository extends JpaRepository<DocumentTag, Long> {
    List<DocumentTag> findByDocument(Document document);
    List<DocumentTag> findByTagName(String tagName);
    void deleteByDocument(Document document);
}
