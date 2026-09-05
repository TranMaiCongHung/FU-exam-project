package com.se196693.mvc.dto.response;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubjectResponse {
    private String subjectCode;

    private String subjectName;

    private Integer termNumber;
}
