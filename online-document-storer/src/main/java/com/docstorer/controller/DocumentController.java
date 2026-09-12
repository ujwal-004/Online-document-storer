package com.docstorer.controller;

import com.docstorer.dto.request.DocumentUploadRequest;
import com.docstorer.dto.request.ShareDocumentRequest;
import com.docstorer.dto.response.*;
import com.docstorer.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Upload, download, search, manage documents")
public class DocumentController {

    private final DocumentService documentService;
    private static final int MAX_PAGE_SIZE = 100;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document")
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart(value = "metadata", required = false) DocumentUploadRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (request == null) request = new DocumentUploadRequest();
        DocumentResponse response = documentService.uploadDocument(file, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document uploaded successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all documents for logged-in user")
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> getAll(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        if (!java.util.Set.of("createdAt", "updatedAt", "originalName", "fileSize", "fileType").contains(sortBy)) {
            throw new IllegalArgumentException("Unsupported sort field: " + sortBy);
        }
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = pageRequest(page, size, sort);
        return ResponseEntity.ok(ApiResponse.success(
                documentService.getUserDocuments(userDetails.getUsername(), pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document by ID")
    public ResponseEntity<ApiResponse<DocumentResponse>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                documentService.getDocumentById(id, userDetails.getUsername())));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download a document")
    public ResponseEntity<byte[]> download(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        DocumentResponse doc = documentService.getDocumentById(id, userDetails.getUsername());
        byte[] data = documentService.downloadDocument(id, userDetails.getUsername());
        String encodedName = URLEncoder.encode(doc.getOriginalName(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.parseMediaType(
                        doc.getMimeType() != null ? doc.getMimeType() : "application/octet-stream"))
                .body(data);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        documentService.deleteDocument(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Document deleted", null));
    }

    @GetMapping("/search")
    @Operation(summary = "Search documents")
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> search(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = pageRequest(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                documentService.searchDocuments(
                        userDetails.getUsername(), keyword, fileType, startDate, endDate, pageable)));
    }

    @PostMapping("/{id}/share")
    @Operation(summary = "Share a document")
    public ResponseEntity<ApiResponse<DocumentResponse>> share(
            @PathVariable Long id,
            @Valid @RequestBody ShareDocumentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Document shared successfully",
                documentService.shareDocument(id, request, userDetails.getUsername())));
    }

    @GetMapping("/shared/{token}")
    @Operation(summary = "Get shared document by token (public)")
    public ResponseEntity<ApiResponse<DocumentResponse>> getShared(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.success(documentService.getSharedDocument(token)));
    }

    @GetMapping("/shared/{token}/download")
    @Operation(summary = "Download shared document (public)")
    public ResponseEntity<byte[]> downloadShared(@PathVariable String token) {
        DocumentResponse doc = documentService.getSharedDocument(token);
        byte[] data = documentService.downloadSharedDocument(token);
        String encodedName = URLEncoder.encode(doc.getOriginalName(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.parseMediaType(
                        doc.getMimeType() != null ? doc.getMimeType() : "application/octet-stream"))
                .body(data);
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "Get document version history")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getVersions(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                documentService.getDocumentVersions(id, userDetails.getUsername())));
    }

    @PostMapping("/{documentId}/versions/{versionId}/restore")
    @Operation(summary = "Restore a previous version")
    public ResponseEntity<ApiResponse<DocumentResponse>> restoreVersion(
            @PathVariable Long documentId,
            @PathVariable Long versionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Version restored",
                documentService.restoreVersion(documentId, versionId, userDetails.getUsername())));
    }

    @PutMapping("/{id}/metadata")
    @Operation(summary = "Update document metadata")
    public ResponseEntity<ApiResponse<DocumentResponse>> updateMetadata(
            @PathVariable Long id,
            @RequestBody DocumentUploadRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Metadata updated",
                documentService.updateDocumentMetadata(id, request, userDetails.getUsername())));
    }

    @PutMapping("/{id}/move")
    @Operation(summary = "Move document to folder")
    public ResponseEntity<ApiResponse<Void>> move(
            @PathVariable Long id,
            @RequestParam(required = false) Long folderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        documentService.moveToFolder(id, folderId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Document moved", null));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> recent(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "5") int limit) {
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_PAGE_SIZE);
        }
        return ResponseEntity.ok(ApiResponse.success(
                documentService.getRecentDocuments(userDetails.getUsername(), limit)));
    }

    private Pageable pageRequest(int page, int size, Sort sort) {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return PageRequest.of(page, size, sort);
    }
}
