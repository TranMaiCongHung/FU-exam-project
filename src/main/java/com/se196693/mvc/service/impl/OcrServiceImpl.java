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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class OcrServiceImpl implements OcrService {

    record ProcessedImage(byte[] bytes, String mimeType) {
    }

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
                    + " Nếu có chú thích số lượng đáp án cần chọn (ví dụ Choose 1 answer -> SINGLE_CHOICE; Choose 2/3/... answers -> MULTIPLE_CHOICE) thì dùng đúng questionType tương ứng."
                    + "KẾT QUẢ TRẢ VỀ BẮT BUỘC PHẢI LÀ ĐỊNH DẠNG JSON ARRAY KHỚP VỚI CẤU TRÚC JAVA SAU, KHÔNG CẦN GIẢI THÍCH GÌ THÊM:"
                    + "[{ \\\"questionNumber\\\": 1, \\\"content\\\": \\\"Nội dung câu hỏi\\\", \\\"questionType\\\": \\\"SINGLE_CHOICE\\\", \\\"answerOption\\\": [ { \\\"optionLabel\\\": \\\"A\\\", \\\"content\\\": \\\"Đáp án A\\\", \\\"correct\\\": false } ] }]";

            // Khởi tạo phần tử đầu tiên của mảng 'parts' chứa câu lệnh text
            StringBuilder partsBuilder = new StringBuilder();
            partsBuilder.append("{\"text\": \"").append(prompt).append("\"}");

            // Dùng vòng lặp biến từng ảnh thành Base64 và nối vào mảng 'parts'
            for (MultipartFile file : files) {
                // Tự động nhận diện, crop và resize ảnh để giảm tối đa token
                ProcessedImage processed = optimizeImage(file);
                String base64Image = Base64.getEncoder().encodeToString(processed.bytes());

                partsBuilder.append(String.format(",{\"inlineData\": {\"mimeType\": \"%s\", \"data\": \"%s\"}}",
                        processed.mimeType(), base64Image));
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
     * Hàm tối ưu ảnh: tự phát hiện ảnh chụp màn hình để crop, xử lý đề 2 cột, trim khoảng trắng và in log
     */
    ProcessedImage optimizeImage(MultipartFile file) {
        String fallbackMimeType = (file.getContentType() != null && !file.getContentType().isBlank())
                ? file.getContentType()
                : "image/jpeg";
        try {
            byte[] originalBytes = file.getBytes();
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(originalBytes));
            if (image == null) {
                log.warn("Không thể giải mã ảnh bằng ImageIO (mimeType={}), sử dụng ảnh gốc", fallbackMimeType);
                return new ProcessedImage(originalBytes, fallbackMimeType);
            }

            log.info("📸 [BƯỚC 1] Kích thước ảnh gốc tải lên: {}x{} px", image.getWidth(), image.getHeight());
            BufferedImage processed = preprocessQuestionImage(image);
            return new ProcessedImage(resizeAndCompress(processed), "image/jpeg");
        } catch (Exception e) {
            log.warn("Không thể tối ưu ảnh, sử dụng ảnh gốc: {}", e.getMessage());
            try {
                return new ProcessedImage(file.getBytes(), fallbackMimeType);
            } catch (Exception ignored) {
                throw new RuntimeException("CANNOT READ IMAGE BYTES", e);
            }
        }
    }

    BufferedImage preprocessQuestionImage(BufferedImage image) {
        // 1. Nếu là ảnh chụp màn hình có viền tối xung quanh -> Cắt bỏ giao diện web
        if (isScreenshotWithDarkSidebar(image)) {
            image = cropWhiteContentBox(image);
            log.info("✂️ [BƯỚC 2] Đã cắt bỏ thanh đen giao diện web: {}x{} px", image.getWidth(), image.getHeight());
        }

        // 2. Nếu là đề 2 cột có vạch đỏ dọc -> Giữ chú thích trên-trái (số câu, Choose X answer) ghép gọn lên trên câu hỏi
        int[] divider = findVerticalRedDivider(image);
        if (divider != null) {
            image = compactTwoColumnQuestion(image, divider[0], divider[1]);
            log.info("🧩 [BƯỚC 3] Đã xử lý bố cục 2 cột (giữ chú thích trên-trái + câu hỏi): {}x{} px",
                    image.getWidth(), image.getHeight());
            return image;
        }

        // 3. Cắt sát nội dung (Ôm trọn câu hỏi, đáp án và ảnh phụ nếu có, bỏ khoảng trắng thừa)
        image = trimContentBoundingBox(image, 15, 100, 60);
        log.info("🎯 [BƯỚC 3] Đã cắt sát nội dung (Chữ + Ảnh phụ): {}x{} px", image.getWidth(), image.getHeight());
        return image;
    }

    private BufferedImage compactTwoColumnQuestion(BufferedImage card, int dividerLeft, int dividerRight) {
        int w = card.getWidth();
        int h = card.getHeight();
        int leftEnd = Math.max(1, dividerLeft - 4);
        int rightStart = Math.min(w - 1, dividerRight + 4);

        BufferedImage leftRaw = card.getSubimage(0, 0, leftEnd, h);
        BufferedImage rightRaw = card.getSubimage(rightStart, 0, w - rightStart, h);

        BufferedImage leftHeader = extractLeftHeader(leftRaw);
        BufferedImage rightContent = trimContentBoundingBox(rightRaw, 15, 60, 30);

        if (leftHeader != null && rightContent != null) {
            int pad = 12;
            int gap = 10;
            int outW = Math.max(leftHeader.getWidth(), rightContent.getWidth()) + pad * 2;
            int outH = leftHeader.getHeight() + gap + rightContent.getHeight() + pad * 2;

            BufferedImage combined = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = combined.createGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, outW, outH);
            g2d.drawImage(leftHeader, pad, pad, null);
            g2d.setColor(new Color(220, 220, 220));
            int lineY = pad + leftHeader.getHeight() + gap / 2;
            g2d.drawLine(pad, lineY, outW - pad, lineY);
            g2d.drawImage(rightContent, pad, pad + leftHeader.getHeight() + gap, null);
            g2d.dispose();
            return combined;
        }
        return rightContent != null ? rightContent : card;
    }

    /**
     * Tách phần chú thích góc trên-trái (vd: "Kizspy | Question: 27", "(Choose 1 answer)"),
     * loại bỏ logo FUO ở đáy và khoảng trắng lớn bên dưới.
     */
    private BufferedImage extractLeftHeader(BufferedImage leftRaw) {
        int w = leftRaw.getWidth();
        int h = leftRaw.getHeight();
        int maxScanY = (int) (h * 0.78);
        int maxGap = Math.max(40, (int) (h * 0.20));

        int minY = -1;
        int maxY = -1;
        int emptyStreak = 0;

        for (int y = 0; y < maxScanY; y++) {
            if (countContentPixelsInRow(leftRaw, y, 0, w) >= 2) {
                if (minY == -1) {
                    minY = y;
                }
                maxY = y;
                emptyStreak = 0;
            } else if (minY != -1) {
                emptyStreak++;
                if (emptyStreak > maxGap) {
                    break;
                }
            }
        }
        if (minY == -1) {
            return null;
        }

        int minX = -1;
        int maxX = -1;
        for (int x = 0; x < w; x++) {
            if (countContentPixelsInCol(leftRaw, x, minY, maxY + 1) >= 2) {
                minX = x;
                break;
            }
        }
        for (int x = w - 1; x >= Math.max(0, minX); x--) {
            if (countContentPixelsInCol(leftRaw, x, minY, maxY + 1) >= 2) {
                maxX = x;
                break;
            }
        }
        if (minX == -1 || maxX == -1) {
            return null;
        }

        int padding = 10;
        minX = Math.max(0, minX - padding);
        minY = Math.max(0, minY - padding);
        maxX = Math.min(w - 1, maxX + padding);
        maxY = Math.min(h - 1, maxY + padding);

        int cropW = maxX - minX + 1;
        int cropH = maxY - minY + 1;
        if (cropW > 20 && cropH > 10) {
            return leftRaw.getSubimage(minX, minY, cropW, cropH);
        }
        return null;
    }

    private int[] findVerticalRedDivider(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int startX = (int) (w * 0.15);
        int endX = (int) (w * 0.65);

        int curLeft = -1;
        for (int x = startX; x <= endX; x++) {
            int redCount = 0;
            for (int y = 0; y < h; y++) {
                if (isRedDividerPixel(img.getRGB(x, y))) {
                    redCount++;
                }
            }
            if (redCount > h * 0.75) {
                if (curLeft == -1) {
                    curLeft = x;
                }
            } else if (curLeft != -1) {
                int dividerWidth = x - curLeft;
                if (dividerWidth <= 12) {
                    return new int[]{curLeft, x - 1};
                }
                curLeft = -1;
            }
        }
        if (curLeft != -1 && (endX - curLeft + 1) <= 12) {
            return new int[]{curLeft, endX};
        }
        return null;
    }

    private boolean isRedDividerPixel(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return r > 140 && (r - g) > 60 && (r - b) > 60;
    }

    /**
     * Thuật toán tìm khung bao nội dung: Dò tìm toàn bộ chữ và hình ảnh minh họa phụ
     */
    private BufferedImage trimContentBoundingBox(BufferedImage img, int padding, int minCropW, int minCropH) {
        int w = img.getWidth();
        int h = img.getHeight();

        int minY = 0, maxY = h - 1;
        int minX = 0, maxX = w - 1;

        for (int y = 0; y < h; y++) {
            if (countContentPixelsInRow(img, y, 0, w) >= 3) {
                minY = y;
                break;
            }
        }

        for (int y = h - 1; y >= minY; y--) {
            if (countContentPixelsInRow(img, y, 0, w) >= 3) {
                maxY = y;
                break;
            }
        }

        for (int x = 0; x < w; x++) {
            if (countContentPixelsInCol(img, x, minY, maxY + 1) >= 3) {
                minX = x;
                break;
            }
        }

        for (int x = w - 1; x >= minX; x--) {
            if (countContentPixelsInCol(img, x, minY, maxY + 1) >= 3) {
                maxX = x;
                break;
            }
        }

        minX = Math.max(0, minX - padding);
        minY = Math.max(0, minY - padding);
        maxX = Math.min(w - 1, maxX + padding);
        maxY = Math.min(h - 1, maxY + padding);

        int cropW = maxX - minX + 1;
        int cropH = maxY - minY + 1;

        if (cropW > minCropW && cropH > minCropH) {
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
     * Pixel nội dung: chữ tối/nét vẽ (luminance < 180) hoặc vùng có màu sắc (sơ đồ, ảnh phụ),
     * đồng thời bỏ qua mũi tên điều hướng xám nhạt (<, >) và watermark mờ.
     */
    private boolean isContentPixel(BufferedImage img, int x, int y) {
        int rgb = img.getRGB(x, y);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int lum = luminance(rgb);
        int saturation = Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b));
        return lum < 180 || (saturation > 20 && lum < 240);
    }

    private boolean isScreenshotWithDarkSidebar(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        if (w < 800 || h < 300) return false;

        int sampleX = (int) (w * 0.92);
        int darkPixels = 0;
        int totalSampled = 0;

        for (int y = (int) (h * 0.2); y < (int) (h * 0.8); y += 15) {
            if (luminance(img.getRGB(sampleX, y)) < 100) {
                darkPixels++;
            }
            totalSampled++;
        }
        if (totalSampled > 0 && ((double) darkPixels / totalSampled) > 0.7) {
            return true;
        }

        // Trường hợp đóng sidebar bình luận nhưng vẫn có thanh tối trên + dưới trình xem ảnh
        return isMostlyDarkRow(img, (int) (h * 0.02)) && isMostlyDarkRow(img, (int) (h * 0.96));
    }

    private boolean isMostlyDarkRow(BufferedImage img, int y) {
        int w = img.getWidth();
        int dark = 0;
        int sampled = 0;
        for (int x = 0; x < w; x += 20) {
            if (luminance(img.getRGB(x, y)) < 100) {
                dark++;
            }
            sampled++;
        }
        return sampled > 0 && ((double) dark / sampled) > 0.65;
    }

    /**
     * Tách khung nội dung nền trắng khỏi giao diện tối bằng mật độ pixel sáng theo hàng/cột,
     * tránh bắn tia từ tâm bị va vào chữ hoặc vạch chia cột làm cắt mất đầu câu hỏi.
     */
    private BufferedImage cropWhiteContentBox(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        int[] rowWhite = new int[h];
        int maxRowWhite = 0;
        for (int y = 0; y < h; y++) {
            int count = 0;
            for (int x = 0; x < w; x++) {
                if (luminance(img.getRGB(x, y)) >= 200) {
                    count++;
                }
            }
            rowWhite[y] = count;
            if (count > maxRowWhite) {
                maxRowWhite = count;
            }
        }
        if (maxRowWhite < w * 0.25) {
            return img;
        }

        int brightRowThreshold = Math.max((int) (w * 0.25), (int) (maxRowWhite * 0.70));
        int brightRowCount = 0;
        for (int y = 0; y < h; y++) {
            if (rowWhite[y] >= brightRowThreshold) {
                brightRowCount++;
            }
        }

        int[] xSpan = findWidestWhiteHorizontalSpan(img, rowWhite, brightRowThreshold, brightRowCount);
        if (xSpan == null) {
            return img;
        }
        int minX = xSpan[0];
        int maxX = xSpan[1];

        int[] ySpan = findTallestWhiteVerticalSpan(img, minX, maxX);
        if (ySpan == null) {
            return img;
        }
        int minY = ySpan[0];
        int maxY = ySpan[1];

        int inset = 4;
        int rx = Math.min(w - 1, minX + inset);
        int ry = Math.min(h - 1, minY + inset);
        int cropW = Math.max(1, (maxX - inset) - rx + 1);
        int cropH = Math.max(1, (maxY - inset) - ry + 1);

        if (cropW > 200 && cropH > 150) {
            return img.getSubimage(rx, ry, cropW, cropH);
        }
        return img;
    }

    private int[] findWidestWhiteHorizontalSpan(BufferedImage img, int[] rowWhite, int brightRowThreshold, int brightRowCount) {
        int w = img.getWidth();
        int h = img.getHeight();
        int minX = -1, maxX = -1, curStartX = -1, gap = 0;

        for (int x = 0; x < w; x++) {
            int whiteInBrightRows = 0;
            for (int y = 0; y < h; y++) {
                if (rowWhite[y] >= brightRowThreshold && luminance(img.getRGB(x, y)) >= 200) {
                    whiteInBrightRows++;
                }
            }
            boolean isCardCol = brightRowCount > 0 && ((double) whiteInBrightRows / brightRowCount) >= 0.50;
            if (isCardCol) {
                if (curStartX == -1) curStartX = x;
                gap = 0;
            } else if (curStartX != -1) {
                gap++;
                if (gap > 5) {
                    int endX = x - gap;
                    if (endX - curStartX > maxX - minX) {
                        minX = curStartX;
                        maxX = endX;
                    }
                    curStartX = -1;
                    gap = 0;
                }
            }
        }
        if (curStartX != -1) {
            int endX = w - 1 - gap;
            if (endX - curStartX > maxX - minX) {
                minX = curStartX;
                maxX = endX;
            }
        }
        return (minX >= 0 && (maxX - minX + 1) > 200) ? new int[]{minX, maxX} : null;
    }

    private int[] findTallestWhiteVerticalSpan(BufferedImage img, int minX, int maxX) {
        int h = img.getHeight();
        int cardW = maxX - minX + 1;
        int marginW = Math.min(15, Math.max(5, cardW / 20));
        int minY = -1, maxY = -1, curStartY = -1, gap = 0;

        for (int y = 0; y < h; y++) {
            int white = 0;
            for (int x = minX; x <= maxX; x++) {
                if (luminance(img.getRGB(x, y)) >= 200) {
                    white++;
                }
            }
            boolean isCardRow = ((double) white / cardW) > 0.25;
            if (!isCardRow) {
                // Giữ các hàng có code/ảnh tối bên trong nếu hai mép trái/phải của card vẫn là nền trắng
                int leftWhite = 0, rightWhite = 0;
                for (int x = minX + 2; x < minX + 2 + marginW; x++) {
                    if (luminance(img.getRGB(x, y)) >= 200) leftWhite++;
                }
                for (int x = maxX - 2 - marginW + 1; x <= maxX - 2; x++) {
                    if (luminance(img.getRGB(x, y)) >= 200) rightWhite++;
                }
                isCardRow = leftWhite >= marginW * 0.6 && rightWhite >= marginW * 0.6;
            }

            if (isCardRow) {
                if (curStartY == -1) curStartY = y;
                gap = 0;
            } else if (curStartY != -1) {
                gap++;
                if (gap > 3) {
                    int endY = y - gap;
                    if (endY - curStartY > maxY - minY) {
                        minY = curStartY;
                        maxY = endY;
                    }
                    curStartY = -1;
                    gap = 0;
                }
            }
        }
        if (curStartY != -1) {
            int endY = h - 1 - gap;
            if (endY - curStartY > maxY - minY) {
                minY = curStartY;
                maxY = endY;
            }
        }
        return (minY >= 0 && (maxY - minY + 1) > 150) ? new int[]{minY, maxY} : null;
    }

    private int luminance(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (int) (0.299 * r + 0.587 * g + 0.114 * b);
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
        }

        // Luôn chuyển sang TYPE_INT_RGB nền trắng để tránh lỗi kênh alpha khi ghi JPEG từ ảnh PNG
        if (img.getType() != BufferedImage.TYPE_INT_RGB || w != img.getWidth() || h != img.getHeight()) {
            BufferedImage rgbImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = rgbImage.createGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, w, h);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(img, 0, 0, w, h, null);
            g2d.dispose();
            img = rgbImage;
        }

        int estimatedTokens = (img.getWidth() <= 384 && img.getHeight() <= 384) ? 258 : 1032;
        log.info("🚀 [BƯỚC 4] Kích thước CUỐI CÙNG gửi tới AI: {}x{} px (Ước tính token ảnh: {} tokens)",
                img.getWidth(), img.getHeight(), estimatedTokens);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }
}
