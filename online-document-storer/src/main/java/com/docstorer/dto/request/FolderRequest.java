package com.docstorer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FolderRequest {
    @NotBlank @Size(max = 100) private String name;
    @Size(max = 500) private String description;
    private String color = "#6366f1";
    private Long parentId;
}
