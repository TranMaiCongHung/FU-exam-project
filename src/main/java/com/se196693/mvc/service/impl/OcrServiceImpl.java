package com.se196693.mvc.service.impl;

import com.se196693.mvc.dto.request.QuestionRequest;
import com.se196693.mvc.service.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OcrServiceImpl implements OcrService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    // Jackson ObjectMapper dùng để biến json (string) thành java object và ngược lại
    private final ObjectMapper objectMapper;

    //URL của gg gemini 1.5 flash
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent";
    @Override
    public List<QuestionRequest> extractQuestionsFromImage(List<MultipartFile> files) {

        try {

            /*
            * prompt cho gemini trả v chuẩn nhuexng gì muốn trả về*/
            String prompt = "Trong ảnh là hình ảnh của một bài thi trắc nghiệm."
                    + "Hãy trích xuất toàn bộ câu hỏi và các câu trả lời có trong hình, dựa vào câu hỏi và đáp án, hãy chọn ra các đáp án chính xác, câu 1 đáp án thì chọn 1, câu nhiều đáp án thì trả về nhiều đáp án đúng, bỏ qua các chi tiết khác không liên quan."
                    + "KẾT QUẢ TRẢ VỀ BẮT BUỘC PHẢI LÀ ĐỊNH DẠNG JSON ARRAY KHỚP VỚI CẤU TRÚC JAVA SAU, KHÔNG CẦN GIẢI THÍCH GÌ THÊM:"
                    + "[{ \\\"questionNumber\\\": 1, \\\"content\\\": \\\"Nội dung câu hỏi\\\", \\\"questionType\\\": \\\"SINGLE_CHOICE\\\", \\\"answerOption\\\": [ { \\\"optionLabel\\\": \\\"A\\\", \\\"content\\\": \\\"Đáp án A\\\", \\\"correct\\\": false } ] }]";

            // Khởi tạo phần tử đầu tiên của mảng 'parts' chứa câu lệnh text
            StringBuilder partsBuilder = new StringBuilder();
            partsBuilder.append("{\"text\": \"").append(prompt).append("\"}");

            // Dùng vòng lặp biến từng ảnh thành Base64 và nối vào mảng 'parts'
            for (MultipartFile file : files) {
                String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
                String mimeType = file.getContentType();
                partsBuilder.append(String.format(",{\"inlineData\": {\"mimeType\": \"%s\", \"data\": \"%s\"}}", mimeType, base64Image));
            }

            // Đưa toàn bộ mảng parts vào Request Body
            String requestBody = String.format("""
                {
                  "contents": [
                    {
                      "parts": [
                        %s
                      ]
                    }
                  ]
                }
                """, partsBuilder.toString());
            //tạo http client và gửi request đi
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_API_URL + "?key=" + geminiApiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            //chờ phản hồi từ gg
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            /*
            * kết quả của gg trả về thường bị bọc trong chuỗi markdown
            * nên cần lọc bỏ ``` json đó đi để ObjectMapper parse */
            String responseText = extractTextFromGeminiResponse(response.body());
            String cleanJson = responseText.replaceAll("```json", "").replaceAll("```", "").trim();

            return objectMapper.readValue(cleanJson, new TypeReference<List<QuestionRequest>>() {
            });
        }catch (Exception e){
            log.error("ERROE WHEN CALL OCR GEMINI API", e);
                throw new RuntimeException("CANNOT EXTRACT IMAGE BY OCR"+ e.getMessage());
        }
    }

    //hàm bóc tách text ừ response của gg
    private String extractTextFromGeminiResponse(String rawResponseBody) throws Exception {
        System.out.println("GOOGLE TRẢ VỀ: " + rawResponseBody);
        //biến cục json của gg thành JsonNode để dễ bới móc
        var rootNode = objectMapper.readTree(rawResponseBody);
        return rootNode.path("candidates").get(0)
                .path("content")
                .path("parts").get(0)
                .path("text").asText();
    }
}
