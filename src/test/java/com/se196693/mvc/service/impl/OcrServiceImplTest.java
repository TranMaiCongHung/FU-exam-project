package com.se196693.mvc.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class OcrServiceImplTest {

    private final OcrServiceImpl ocrService = new OcrServiceImpl(new ObjectMapper());

    @Test
    void preprocessQuestionImage_twoColumnScreenshotWithRedDivider_keepsTopLeftNoteAndFullQuestion() {
        int w = 1280, h = 720;
        BufferedImage screenshot = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = screenshot.createGraphics();

        // Dark browser viewer UI & right comment sidebar
        g.setColor(new Color(24, 24, 28));
        g.fillRect(0, 0, w, h);

        // Bottom white thumbnails (distractors that must not merge with main card)
        g.setColor(Color.WHITE);
        for (int i = 0; i < 5; i++) {
            g.fillRect(360 + i * 110, 640, 95, 55);
        }

        // Main white question card
        int cardX = 10, cardY = 55, cardW = 980, cardH = 470;
        g.setColor(Color.WHITE);
        g.fillRect(cardX, cardY, cardW, cardH);

        // Red vertical divider line (~33% of screenshot width, inside card)
        int redX = cardX + 410;
        g.setColor(new Color(230, 20, 20));
        g.fillRect(redX, cardY, 2, cardH);

        // Top-left metadata in left panel
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.drawString("Kizspy | Question: 27", cardX + 15, cardY + 28);
        g.drawString("(Choose 2 answers)", cardX + 15, cardY + 62);

        // Bottom-left FUO badge (should be dropped)
        g.setColor(new Color(120, 120, 120));
        g.fillRect(cardX, cardY + cardH - 45, 95, 45);

        // Light-gray navigation arrows '<' and '>' (should be ignored)
        g.setColor(new Color(200, 200, 200));
        g.drawString("<", cardX + 70, cardY + 240);
        g.drawString(">", cardX + cardW - 60, cardY + 240);

        // Right panel question & options crossing x = 0.4 * w (512)
        g.setColor(Color.BLACK);
        g.drawString("The Standard Dialect in Thymeleaf provides a set of:", redX + 18, cardY + 28);
        g.drawString("A. Database drivers.", redX + 18, cardY + 60);
        g.drawString("B. HTML attributes and elements.", redX + 18, cardY + 92);
        g.drawString("C. Security protocols.", redX + 18, cardY + 124);
        g.drawString("D. Network configurations.", redX + 18, cardY + 156);
        g.dispose();

        BufferedImage result = ocrService.preprocessQuestionImage(screenshot);

        // Must be tightly compacted (much smaller than 980x470 card)
        assertTrue(result.getWidth() < 600, "Expected compact width, got " + result.getWidth());
        assertTrue(result.getHeight() < 300, "Expected compact height, got " + result.getHeight());

        // Red divider must be gone
        assertFalse(containsRedVerticalLine(result), "Red divider line should be removed");

        // Top section must contain dark text pixels (from left header) and bottom section (from question & options)
        assertTrue(hasDarkPixelsInBand(result, 0, result.getHeight() / 2), "Top metadata should be preserved");
        assertTrue(hasDarkPixelsInBand(result, result.getHeight() / 2, result.getHeight()), "Question & options should be preserved");
    }

    @Test
    void preprocessQuestionImage_singleColumnWithTextAndDarkCodeBlockCrossingCenter_doesNotChopTop() {
        int w = 1280, h = 720;
        BufferedImage screenshot = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = screenshot.createGraphics();

        g.setColor(new Color(24, 24, 28));
        g.fillRect(0, 0, w, h);

        int cardX = 20, cardY = 60, cardW = 940, cardH = 500;
        g.setColor(Color.WHITE);
        g.fillRect(cardX, cardY, cardW, cardH);

        // Text right over centerX = 0.4 * w (512) near the top
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("Which output is produced by the following Java snippet at x=512?", cardX + 30, cardY + 35);

        // Dark code block inside the card (tests margin fallback)
        g.setColor(new Color(30, 30, 35));
        g.fillRect(cardX + 60, cardY + 70, 760, 140);

        g.setColor(Color.BLACK);
        g.drawString("A. 10", cardX + 30, cardY + 250);
        g.drawString("B. 20", cardX + 30, cardY + 285);
        g.dispose();

        BufferedImage result = ocrService.preprocessQuestionImage(screenshot);

        // Height must include top question text + dark code block + options A and B (> 220px)
        assertTrue(result.getHeight() >= 250, "Top text or code block was unexpectedly cut off: h=" + result.getHeight());
        assertTrue(hasDarkPixelsInBand(result, 0, 50), "Top question line must not be cut off");
    }

    @Test
    void preprocessQuestionImage_twoColumnRawCardWithoutDarkSidebar_compactsCorrectly() {
        int cardW = 980, cardH = 470;
        BufferedImage card = new BufferedImage(cardW, cardH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = card.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, cardW, cardH);

        int redX = 410;
        g.setColor(new Color(230, 20, 20));
        g.fillRect(redX, 0, 2, cardH);

        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.drawString("Kizspy | Question: 12", 15, 28);
        g.drawString("(Choose 1 answer)", 15, 60);

        g.setColor(new Color(120, 120, 120));
        g.fillRect(0, cardH - 45, 95, 45);

        g.setColor(Color.BLACK);
        g.drawString("Which annotation marks a Spring MVC controller?", redX + 18, 28);
        g.drawString("A. @Service", redX + 18, 60);
        g.drawString("B. @Controller", redX + 18, 92);
        g.dispose();

        BufferedImage result = ocrService.preprocessQuestionImage(card);

        assertFalse(containsRedVerticalLine(result), "Red divider line should be removed");
        assertTrue(result.getWidth() < 600, "Expected compact width, got " + result.getWidth());
        assertTrue(result.getHeight() < 260, "Expected compact height, got " + result.getHeight());
        assertTrue(hasDarkPixelsInBand(result, 0, result.getHeight() / 2));
        assertTrue(hasDarkPixelsInBand(result, result.getHeight() / 2, result.getHeight()));
    }

    @Test
    void optimizeImage_argbPngSmallAndUnsupportedWebp_handlesMimeAndJpegEncodingSafely() throws Exception {
        BufferedImage argb = new BufferedImage(320, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = argb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 320, 200);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.drawString("What is JPA?", 20, 40);
        g.drawString("A. Java Persistence API", 20, 80);
        g.dispose();

        ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
        ImageIO.write(argb, "png", pngOut);
        MockMultipartFile pngFile = new MockMultipartFile("file", "q1.png", "image/png", pngOut.toByteArray());

        OcrServiceImpl.ProcessedImage processedPng = ocrService.optimizeImage(pngFile);
        assertEquals("image/jpeg", processedPng.mimeType());
        assertTrue(processedPng.bytes().length > 0);
        BufferedImage decodedJpeg = ImageIO.read(new ByteArrayInputStream(processedPng.bytes()));
        assertNotNull(decodedJpeg, "ARGB PNG should be cleanly encoded as readable JPEG");

        byte[] dummyWebp = new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
        MockMultipartFile webpFile = new MockMultipartFile("file", "Q27.webp", "image/webp", dummyWebp);
        OcrServiceImpl.ProcessedImage processedWebp = ocrService.optimizeImage(webpFile);
        assertEquals("image/webp", processedWebp.mimeType());
        assertArrayEquals(dummyWebp, processedWebp.bytes());
    }

    private boolean containsRedVerticalLine(BufferedImage img) {
        for (int x = 0; x < img.getWidth(); x++) {
            int red = 0;
            for (int y = 0; y < img.getHeight(); y++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                if (r > 140 && (r - g) > 60 && (r - b) > 60) red++;
            }
            if (red > img.getHeight() * 0.5) return true;
        }
        return false;
    }

    private boolean hasDarkPixelsInBand(BufferedImage img, int startY, int endY) {
        int dark = 0;
        for (int y = startY; y < endY; y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                if ((0.299 * r + 0.587 * g + 0.114 * b) < 120) {
                    dark++;
                    if (dark >= 10) return true;
                }
            }
        }
        return false;
    }
}
