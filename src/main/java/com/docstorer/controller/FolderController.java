package com.docstorer.controller;

import com.docstorer.dto.request.FolderRequest;
import com.docstorer.dto.response.*;
import com.docstorer.service.FolderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
@Tag(name = "Folders", description = "Folder management")
public class FolderController {

    private final FolderService folderService;

    @PostMapping
    @Operation(summary = "Create a folder")
    public ResponseEntity<ApiResponse<FolderResponse>> create(
            @Valid @RequestBody FolderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Folder created",
                folderService.createFolder(request, userDetails.getUsername())));
    }

    @GetMapping
    @Operation(summary = "Get all root folders")
    public ResponseEntity<ApiResponse<List<FolderResponse>>> getAll(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                folderService.getUserFolders(userDetails.getUsername())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get folder by ID")
    public ResponseEntity<ApiResponse<FolderResponse>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                folderService.getFolderById(id, userDetails.getUsername())));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update folder")
    public ResponseEntity<ApiResponse<FolderResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody FolderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Folder updated",
                folderService.updateFolder(id, request, userDetails.getUsername())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete folder")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        folderService.deleteFolder(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Folder deleted", null));
    }
}
