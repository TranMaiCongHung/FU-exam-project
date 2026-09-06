package com.se196693.mvc.dto.request;

import com.se196693.mvc.enums.ExamType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExamFilterRequest {
    private Long subjectId;
    private ExamType examType;
}
