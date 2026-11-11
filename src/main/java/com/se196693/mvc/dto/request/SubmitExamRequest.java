package com.se196693.mvc.dto.request;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SubmitExamRequest {
    // Map: Key là questionId, Value là danh sách optionId người dùng đã chọn
    private Map<Long, List<Long>> answers;
}