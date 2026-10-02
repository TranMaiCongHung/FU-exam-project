package com.se196693.mvc.dto.response;

import com.se196693.mvc.enums.SubjectStatus;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubjectResponse {
    private String subjectCode;

    private String subjectName;

    private Integer termNumber;

    private SubjectStatus status;
}
