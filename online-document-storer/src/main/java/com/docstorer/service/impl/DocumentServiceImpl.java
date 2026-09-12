package com.docstorer.service.impl;

import com.docstorer.dto.request.DocumentUploadRequest;
import com.docstorer.dto.request.ShareDocumentRequest;
import com.docstorer.dto.response.DocumentResponse;
import com.docstorer.dto.response.UserResponse;
import com.docstorer.entity.*;
import com.docstorer.exception.ResourceNotFoundException;
import com.docstorer.exception.UnauthorizedException;
import com.docstorer.repository.*;
import com.docstorer.service.DocumentService;
import com.docstorer.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final DocumentVersionRepository versionRepository;
    private final DocumentShareRepository shareRepository;
    private final ActivityLogRepository activityLogRepository;
    private final FileStorageUtil fileStorageUtil;

    @Override
    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, DocumentUploadRequest req, String userEmail) {
        User user = getUser(userEmail);
        String checksum = fileStorageUtil.computeChecksum(file);
        String filePath = req.isEncrypt()
                ? fileStorageUtil.storeEncryptedFile(file, "documents")
                : fileStorageUtil.storeFile(file, "documents");

        String originalName = file.getOriginalFilename();
        String fileType = getExtension(originalName);

        Document doc = Document.builder()
                .fileName(filePath.substring(filePath.lastIndexOf("/") + 1))
                .originalName(originalName)
                .fileType(fileType)
                .mimeType(file.getContentType())
                .fileSize(file.getSize())
                .filePath(filePath)
                .description(req.getDescription())
                .isEncrypted(req.isEncrypt())
                .checksum(checksum)
                .status(Document.DocumentStatus.ACTIVE)
                .owner(user)
                .version(1)
                .build();

        if (req.getFolderId() != null) {
            Folder folder = folderRepository.findById(req.getFolderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", req.getFolderId()));
            if (!folder.getOwner().getId().equals(user.getId()))
                throw new UnauthorizedException("You do not own this folder");
            doc.setFolder(folder);
        }

        doc = documentRepository.save(doc);

        // Metadata
        DocumentMetadata meta = DocumentMetadata.builder()
                .document(doc)
                .category(req.getCategory())
                .build();
        doc.setMetadata(meta);

        // Tags
        if (req.getTags() != null && !req.getTags().isBlank()) {
            String[] tagNames = req.getTags().split(",");
            for (String tagName : tagNames) {
                if (!tagName.isBlank()) {
                    DocumentTag tag = DocumentTag.builder()
                            .document(doc).tagName(tagName.trim()).build();
                    doc.getTags().add(tag);
                }
            }
        }

        // Version history - save v1
        DocumentVersion v1 = DocumentVersion.builder()
                .document(doc).versionNumber(1).filePath(filePath)
                .fileSize(file.getSize()).changeNotes("Initial upload").createdBy(user).build();
        versionRepository.save(v1);

        // Update user storage
        user.setStorageUsed(user.getStorageUsed() + file.getSize());
        userRepository.save(user);

        logActivity(user, ActivityLog.Action.UPLOAD_DOCUMENT, doc.getId(), doc.getOriginalName(),
                "Document uploaded: " + originalName);

        return buildResponse(doc);
    }

    @Override
    @Transactional
    public DocumentResponse getDocumentById(Long id, String userEmail) {
        User user = getUser(userEmail);
        Document doc = getDocument(id);
        if (!doc.getOwner().getId().equals(user.getId()) && !isAdmin(user))
            throw new UnauthorizedException("Access denied to this document");
        doc.setViewCount(doc.getViewCount() + 1);
        documentRepository.save(doc);
        return buildResponse(doc);
    }

    @Override
    @Transactional
    public byte[] downloadDocument(Long id, String userEmail) {
        User user = getUser(userEmail);
        Document doc = getDocument(id);
        if (!doc.getOwner().getId().equals(user.getId()) && !isAdmin(user))
            throw new UnauthorizedException("Access denied");
        doc.setDownloadCount(doc.getDownloadCount() + 1);
        documentRepository.save(doc);
        logActivity(user, ActivityLog.Action.DOWNLOAD_DOCUMENT, doc.getId(), doc.getOriginalName(),
                "Document downloaded");
        return doc.getIsEncrypted()
                ? fileStorageUtil.loadDecryptedFile(doc.getFilePath())
                : fileStorageUtil.loadFile(doc.getFilePath());
    }

    @Override
    @Transactional
    public void deleteDocument(Long id, String userEmail) {
        User user = getUser(userEmail);
        Document doc = getDocument(id);
        if (!doc.getOwner().getId().equals(user.getId()) && !isAdmin(user))
            throw new UnauthorizedException("Access denied");
        doc.setStatus(Document.DocumentStatus.DELETED);
        documentRepository.save(doc);
        user.setStorageUsed(Math.max(0, user.getStorageUsed() - doc.getFileSize()));
        userRepository.save(user);
        logActivity(user, ActivityLog.Action.DELETE_DOCUMENT, doc.getId(), doc.getOriginalName(),
                "Document deleted");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponse> getUserDocuments(String userEmail, Pageable pageable) {
        User user = getUser(userEmail);
        return documentRepository.findByOwnerAndStatusNot(user, Document.DocumentStatus.DELETED, pageable)
                .map(this::buildResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponse> searchDocuments(String userEmail, String keyword, String fileType,
                                                   String startDate, String endDate, Pageable pageable) {
        User user = getUser(userEmail);
        if (keyword != null && !keyword.isBlank())
            return documentRepository.searchByKeyword(user, keyword, pageable).map(this::buildResponse);
        if (fileType != null && !fileType.isBlank())
            return documentRepository.findByOwnerAndFileType(user, fileType, pageable).map(this::buildResponse);
        if ((startDate == null) != (endDate == null)) {
            throw new IllegalArgumentException("Both startDate and endDate are required when filtering by date");
        }
        if (startDate != null) {
            LocalDateTime start;
            LocalDateTime end;
            try {
                start = LocalDateTime.parse(startDate + "T00:00:00");
                end = LocalDateTime.parse(endDate + "T23:59:59");
            } catch (java.time.format.DateTimeParseException ex) {
                throw new IllegalArgumentException("Dates must use the yyyy-MM-dd format", ex);
            }
            if (start.isAfter(end)) {
                throw new IllegalArgumentException("startDate must not be after endDate");
            }
            return documentRepository.findByOwnerAndDateRange(user, start, end, pageable).map(this::buildResponse);
        }
        return getUserDocuments(userEmail, pageable);
    }

    @Override
    @Transactional
    public DocumentResponse shareDocument(Long id, ShareDocumentRequest req, String userEmail) {
        User user = getUser(userEmail);
        Document doc = getDocument(id);
        if (!doc.getOwner().getId().equals(user.getId()))
            throw new UnauthorizedException("Only owner can share documents");

        String shareToken = UUID.randomUUID().toString().replace("-", "");
        doc.setIsShared(true);
        doc.setShareToken(shareToken);
        doc.setShareExpiry(req.getExpiryDate());

        if (req.getSharedWithEmail() != null) {
            User sharedWith = userRepository.findByEmail(req.getSharedWithEmail()).orElse(null);
            DocumentShare share = DocumentShare.builder()
                    .document(doc).sharedBy(user).sharedWith(sharedWith)
                    .sharedEmail(req.getSharedWithEmail())
                    .permission(DocumentShare.Permission.valueOf(req.getPermission()))
                    .expiryDate(req.getExpiryDate()).isActive(true).build();
            shareRepository.save(share);
        }

        documentRepository.save(doc);
        logActivity(user, ActivityLog.Action.SHARE_DOCUMENT, doc.getId(), doc.getOriginalName(),
                "Document shared with: " + req.getSharedWithEmail());
        return buildResponse(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getSharedDocument(String shareToken) {
        Document doc = documentRepository.findActiveSharedDocument(shareToken, LocalDateTime.now())
                .orElseThrow(() -> new ResourceNotFoundException("Shared document not found or expired"));
        return buildResponse(doc);
    }

    @Override
    @Transactional
    public byte[] downloadSharedDocument(String shareToken) {
        Document doc = documentRepository.findActiveSharedDocument(shareToken, LocalDateTime.now())
                .orElseThrow(() -> new ResourceNotFoundException("Shared document not found or expired"));
        doc.setDownloadCount(doc.getDownloadCount() + 1);
        documentRepository.save(doc);
        return doc.getIsEncrypted()
                ? fileStorageUtil.loadDecryptedFile(doc.getFilePath())
                : fileStorageUtil.loadFile(doc.getFilePath());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentVersions(Long id, String userEmail) {
        User user = getUser(userEmail);
        Document doc = getDocument(id);
        if (!doc.getOwner().getId().equals(user.getId()))
            throw new UnauthorizedException("Access denied");
        return versionRepository.findByDocumentOrderByVersionNumberDesc(doc).stream()
                .map(v -> DocumentResponse.builder()
                        .id(v.getId()).originalName("v" + v.getVersionNumber() + " - " + doc.getOriginalName())
                        .fileSize(v.getFileSize()).createdAt(v.getCreatedAt()).build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DocumentResponse restoreVersion(Long documentId, Long versionId, String userEmail) {
        User user = getUser(userEmail);
        Document doc = getDocument(documentId);
        if (!doc.getOwner().getId().equals(user.getId()))
            throw new UnauthorizedException("Access denied");
        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version", "id", versionId));
        if (!version.getDocument().getId().equals(doc.getId()))
            throw new UnauthorizedException("This version does not belong to the specified document");
        // Save current as new version
        int newVersionNum = doc.getVersion() + 1;
        String versionPath = fileStorageUtil.copyFileAsVersion(doc.getFilePath(), newVersionNum);
        DocumentVersion newVersion = DocumentVersion.builder()
                .document(doc).versionNumber(newVersionNum).filePath(versionPath)
                .fileSize(doc.getFileSize()).changeNotes("Restored from v" + version.getVersionNumber())
                .createdBy(user).build();
        versionRepository.save(newVersion);
        doc.setFilePath(version.getFilePath());
        doc.setVersion(newVersionNum);
        documentRepository.save(doc);
        logActivity(user, ActivityLog.Action.RESTORE_DOCUMENT, doc.getId(), doc.getOriginalName(),
                "Restored to version " + version.getVersionNumber());
        return buildResponse(doc);
    }

    @Override
    @Transactional
    public DocumentResponse updateDocumentMetadata(Long id, DocumentUploadRequest req, String userEmail) {
        User user = getUser(userEmail);
        Document doc = getDocument(id);
        if (!doc.getOwner().getId().equals(user.getId()))
            throw new UnauthorizedException("Access denied");
        if (req.getDescription() != null) doc.setDescription(req.getDescription());
        documentRepository.save(doc);
        return buildResponse(doc);
    }

    @Override
    @Transactional
    public void moveToFolder(Long documentId, Long folderId, String userEmail) {
        User user = getUser(userEmail);
        Document doc = getDocument(documentId);
        if (!doc.getOwner().getId().equals(user.getId()))
            throw new UnauthorizedException("Access denied");
        if (folderId == null) {
            doc.setFolder(null);
        } else {
            Folder folder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));
            if (!folder.getOwner().getId().equals(user.getId()))
                throw new UnauthorizedException("You do not own this folder");
            doc.setFolder(folder);
        }
        documentRepository.save(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getRecentDocuments(String userEmail, int limit) {
        User user = getUser(userEmail);
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        return documentRepository.findByOwnerAndStatusNot(user, Document.DocumentStatus.DELETED, pageable)
                .map(this::buildResponse).getContent();
    }

    // ---- helpers ----

    private DocumentResponse buildResponse(Document doc) {
        List<String> tags = doc.getTags().stream()
                .map(DocumentTag::getTagName).collect(Collectors.toList());
        return DocumentResponse.builder()
                .id(doc.getId())
                .fileName(doc.getFileName())
                .originalName(doc.getOriginalName())
                .fileType(doc.getFileType())
                .mimeType(doc.getMimeType())
                .fileSize(doc.getFileSize())
                .fileSizeFormatted(DocumentResponse.formatFileSize(doc.getFileSize() != null ? doc.getFileSize() : 0))
                .description(doc.getDescription())
                .status(doc.getStatus())
                .isEncrypted(doc.getIsEncrypted())
                .isShared(doc.getIsShared())
                .shareToken(doc.getShareToken())
                .shareExpiry(doc.getShareExpiry())
                .downloadCount(doc.getDownloadCount())
                .viewCount(doc.getViewCount())
                .version(doc.getVersion())
                .category(doc.getMetadata() != null ? doc.getMetadata().getCategory() : null)
                .tags(tags)
                .folderId(doc.getFolder() != null ? doc.getFolder().getId() : null)
                .folderName(doc.getFolder() != null ? doc.getFolder().getName() : null)
                .owner(UserResponse.from(doc.getOwner()))
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    private User getUser(String email) {
        return userRepository.findByEmailOrUsername(email)
                .or(() -> userRepository.findByEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private Document getDocument(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "unknown";
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private boolean isAdmin(User user) {
        return user.getRole() == User.Role.ADMIN;
    }

    private void logActivity(User user, ActivityLog.Action action, Long entityId, String entityName, String desc) {
        ActivityLog log = ActivityLog.builder()
                .user(user).action(action).entityType("Document")
                .entityId(entityId).entityName(entityName).description(desc).isSuccess(true).build();
        activityLogRepository.save(log);
    }
}
