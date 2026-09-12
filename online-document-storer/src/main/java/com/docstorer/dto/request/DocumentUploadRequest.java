package com.docstorer.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DocumentUploadRequest {

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    private Long folderId;

    @Size(max = 100, message = "Category cannot exceed 100 characters")
    private String category;

    private String tags; // comma-separated tag names

    private boolean encrypt = false;
}
