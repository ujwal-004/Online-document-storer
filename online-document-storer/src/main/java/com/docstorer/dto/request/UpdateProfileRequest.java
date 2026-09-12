package com.docstorer.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(max = 50) private String firstName;
    @Size(max = 50) private String lastName;
    @Size(max = 20) private String phoneNumber;
    private Boolean darkMode;
}
