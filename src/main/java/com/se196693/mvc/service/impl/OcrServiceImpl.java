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


import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class OcrServiceImpl implements OcrService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    // Jackson ObjectMapper dùng để biến json (string) thành java object và ngược lại
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

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
                // Tự động nhận diện, crop và resize ảnh để giảm tối đa token
                byte[] optimizedBytes = optimizeImage(file.getBytes());
                String base64Image = Base64.getEncoder().encodeToString(optimizedBytes);

                partsBuilder.append(String.format(",{\"inlineData\": {\"mimeType\": \"image/jpeg\", \"data\": \"%s\"}}", base64Image));
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
      ],
      "generationConfig": {
        "mediaResolution": "MEDIA_RESOLUTION_MEDIUM"
      }
    }
    """, partsBuilder.toString());
            //tạo http client và gửi request đi
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(geminiApiUrl + "?key=" + geminiApiKey))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", geminiApiKey)
                    .header("Authorization", "Bearer " + geminiApiKey)
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
        } catch (Exception e) {
            log.error("ERROE WHEN CALL OCR GEMINI API", e);
            throw new RuntimeException("CANNOT EXTRACT IMAGE BY OCR" + e.getMessage());
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

    /**
     * Hàm tối ưu ảnh: tự phát hiện ảnh chụp màn hình để crop, trim khoảng trắng và in log
     */
    private byte[] optimizeImage(byte[] originalBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(originalBytes));
            if (image == null) {
                return originalBytes;
            }

            log.info("📸 [BƯỚC 1] Kích thước ảnh gốc tải lên: {}x{} px", image.getWidth(), image.getHeight());

            // 1. Nếu là ảnh chụp màn hình có viền đen xung quanh -> Cắt bỏ viền đen
            if (isScreenshotWithDarkSidebar(image)) {
                image = cropWhiteContentBox(image);
                log.info("✂️ [BƯỚC 2] Đã cắt bỏ thanh đen giao diện web: {}x{} px", image.getWidth(), image.getHeight());
            }

            // 2. CẮT SÁT NỘI DUNG (Ôm trọn câu hỏi, đáp án và ảnh phụ nếu có, bỏ 100% khoảng trắng thừa)
            image = trimContentBoundingBox(image);
            log.info("🎯 [BƯỚC 3] Đã cắt sát nội dung (Chữ + Ảnh phụ): {}x{} px", image.getWidth(), image.getHeight());

            // 3. Resize nếu kích thước vượt ngưỡng chuẩn và nén JPEG
            return resizeAndCompress(image);
        } catch (Exception e) {
            log.warn("Không thể tối ưu ảnh, sử dụng ảnh gốc: {}", e.getMessage());
            return originalBytes;
        }
    }

    /**
     * Thuật toán tìm khung bao nội dung: Dò tìm toàn bộ chữ và hình ảnh minh họa phụ
     */
    private BufferedImage trimContentBoundingBox(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        int minY = 0, maxY = h - 1;
        int minX = 0, maxX = w - 1;

        // Dò mép trên (minY) - nơi bắt đầu câu hỏi
        for (int y = 0; y < h; y++) {
            if (countContentPixelsInRow(img, y, 0, w) >= 3) {
                minY = y;
                break;
            }
        }

        // Dò mép dưới (maxY) - đáy của đáp án cuối cùng HOẶC đáy của ảnh phụ
        for (int y = h - 1; y >= minY; y--) {
            if (countContentPixelsInRow(img, y, 0, w) >= 3) {
                maxY = y;
                break;
            }
        }

        // Dò mép trái (minX)
        for (int x = 0; x < w; x++) {
            if (countContentPixelsInCol(img, x, minY, maxY) >= 3) {
                minX = x;
                break;
            }
        }

        // Dò mép phải (maxX) - mép chữ hoặc mép ngoài cùng của ảnh phụ
        for (int x = w - 1; x >= minX; x--) {
            // Đặt ngưỡng >= 5 để tránh nhận nhầm icon mũi tên điều hướng mờ ở góc phải
            if (countContentPixelsInCol(img, x, minY, maxY) >= 5) {
                maxX = x;
                break;
            }
        }

        // Cộng thêm lề an toàn (padding 15px) để nét chữ/ảnh không bị dính sát mép cắt
        int padding = 15;
        minX = Math.max(0, minX - padding);
        minY = Math.max(0, minY - padding);
        maxX = Math.min(w - 1, maxX + padding);
        maxY = Math.min(h - 1, maxY + padding);

        int cropW = maxX - minX;
        int cropH = maxY - minY;

        if (cropW > 100 && cropH > 60) {
            return img.getSubimage(minX, minY, cropW, cropH);
        }
        return img;
    }

    private int countContentPixelsInRow(BufferedImage img, int y, int startX, int endX) {
        int count = 0;
        for (int x = startX; x < endX; x++) {
            if (isContentPixel(img, x, y)) {
                count++;
                if (count >= 10) return count;
            }
        }
        return count;
    }

    private int countContentPixelsInCol(BufferedImage img, int x, int startY, int endY) {
        int count = 0;
        for (int y = startY; y < endY; y++) {
            if (isContentPixel(img, x, y)) {
                count++;
                if (count >= 10) return count;
            }
        }
        return count;
    }

    /**
     * Pixel nội dung: Bất kỳ pixel nào không phải màu trắng nền (chữ đen, màu vẽ sơ đồ, ảnh phụ,...)
     */
    private boolean isContentPixel(BufferedImage img, int x, int y) {
        int rgb = img.getRGB(x, y);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int luminance = (int) (0.299 * r + 0.587 * g + 0.114 * b);
        return luminance < 235; // Độ sáng < 235 nghĩa là có nội dung
    }

    private boolean isScreenshotWithDarkSidebar(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        if (w < 800) return false;

        int sampleX = (int) (w * 0.92);
        int darkPixels = 0;
        int totalSampled = 0;

        for (int y = (int) (h * 0.2); y < (int) (h * 0.8); y += 15) {
            int rgb = img.getRGB(sampleX, y);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            int luminance = (int) (0.299 * r + 0.587 * g + 0.114 * b);
            if (luminance < 100) {
                darkPixels++;
            }
            totalSampled++;
        }
        return totalSampled > 0 && ((double) darkPixels / totalSampled) > 0.7;
    }

    private BufferedImage cropWhiteContentBox(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int centerX = (int) (w * 0.4);
        int centerY = (int) (h * 0.4);

        int maxX = w - 1;
        for (int x = centerX; x < w; x++) {
            if (isDarkPixel(img, x, centerY)) {
                maxX = Math.max(centerX, x - 5);
                break;
            }
        }

        int maxY = h - 1;
        for (int y = centerY; y < h; y++) {
            if (isDarkPixel(img, centerX, y)) {
                maxY = Math.max(centerY, y - 5);
                break;
            }
        }

        int minY = 0;
        for (int y = centerY; y >= 0; y--) {
            if (isDarkPixel(img, centerX, y)) {
                minY = Math.min(centerY, y + 5);
                break;
            }
        }

        int minX = 0;
        for (int x = centerX; x >= 0; x--) {
            if (isDarkPixel(img, x, centerY)) {
                minX = Math.min(centerX, x + 5);
                break;
            }
        }

        int cropW = maxX - minX;
        int cropH = maxY - minY;

        if (cropW > 200 && cropH > 150) {
            return img.getSubimage(minX, minY, cropW, cropH);
        }
        return img;
    }

    private boolean isDarkPixel(BufferedImage img, int x, int y) {
        int rgb = img.getRGB(x, y);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int luminance = (int) (0.299 * r + 0.587 * g + 0.114 * b);
        return luminance < 100;
    }

    /**
     * Scale ảnh theo tỉ lệ chuẩn và in log kích cỡ gửi AI
     */
    private byte[] resizeAndCompress(BufferedImage img) throws Exception {
        int maxDim = 768;
        int w = img.getWidth();
        int h = img.getHeight();

        // Nếu có chiều nào vượt quá 768px thì scale lại theo tỷ lệ
        if (w > maxDim || h > maxDim) {
            if (w >= h) {
                h = (int) ((double) h * maxDim / w);
                w = maxDim;
            } else {
                w = (int) ((double) w * maxDim / h);
                h = maxDim;
            }

            BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resized.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(img, 0, 0, w, h, null);
            g2d.dispose();
            img = resized;
        }

        int estimatedTokens = (img.getWidth() <= 384 && img.getHeight() <= 384) ? 258 : 1032;
        log.info("🚀 [BƯỚC 4] Kích thước CUỐI CÙNG gửi tới AI: {}x{} px (Ước tính token ảnh: {} tokens)",
                img.getWidth(), img.getHeight(), estimatedTokens);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }
}
