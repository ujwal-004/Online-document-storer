package com.docstorer.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ShareDocumentRequest {
    @Email private String sharedWithEmail;
    private String permission = "VIEW"; // VIEW, DOWNLOAD, EDIT
    private LocalDateTime expiryDate;
}
