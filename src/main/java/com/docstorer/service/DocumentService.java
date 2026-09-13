package com.docstorer.service;

import com.docstorer.dto.request.DocumentUploadRequest;
import com.docstorer.dto.request.ShareDocumentRequest;
import com.docstorer.dto.response.DocumentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {
    DocumentResponse uploadDocument(MultipartFile file, DocumentUploadRequest request, String userEmail);
    DocumentResponse getDocumentById(Long id, String userEmail);
    byte[] downloadDocument(Long id, String userEmail);
    void deleteDocument(Long id, String userEmail);
    Page<DocumentResponse> getUserDocuments(String userEmail, Pageable pageable);
    Page<DocumentResponse> searchDocuments(String userEmail, String keyword, String fileType,
                                           String startDate, String endDate, Pageable pageable);
    DocumentResponse shareDocument(Long id, ShareDocumentRequest request, String userEmail);
    DocumentResponse getSharedDocument(String shareToken);
    byte[] downloadSharedDocument(String shareToken);
    List<DocumentResponse> getDocumentVersions(Long id, String userEmail);
    DocumentResponse restoreVersion(Long documentId, Long versionId, String userEmail);
    DocumentResponse updateDocumentMetadata(Long id, DocumentUploadRequest request, String userEmail);
    void moveToFolder(Long documentId, Long folderId, String userEmail);
    List<DocumentResponse> getRecentDocuments(String userEmail, int limit);
}
