package com.se196693.mvc.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminSubjectRequest {
    @NotBlank(message = "Subject code is required")
    @Pattern(
            regexp = "^[A-Z]{3}[0-9]{3}[a-z]?$",
            message = "Subject code must be inlcude 3 uppercase letters, 3 digits and 1 letter(optional). E.g: DBI202"
    )
    private String subjectCode;

    @NotBlank(message = "Subject Name is required")
    private String subjectName;

    @NotNull(message = "Term number is required")
    @Range(min = 1, max = 9, message = "Term must be between 1 and 9")
    private Integer termNumber;
}
