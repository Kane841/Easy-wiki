package com.easywiki.service;

import com.easywiki.config.UploadProperties;
import com.easywiki.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class FileServiceTest {

    @TempDir
    static Path tempDir;

    @Autowired FileService fileService;
    @Autowired UploadProperties uploadProperties;

    @DynamicPropertySource
    static void uploadPath(DynamicPropertyRegistry registry) {
        registry.add("easywiki.upload.path", () -> tempDir.toString() + "/");
    }

    @BeforeEach
    void setup() {
        uploadProperties.setPath(tempDir.toString() + "/");
    }

    @Test
    void saveImageStoresFileAndReturnsUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", new byte[]{1, 2, 3});

        String url = fileService.saveImage(42L, file);

        assertThat(url).startsWith("/uploads/42/");
        assertThat(url).endsWith(".png");
        Path saved = tempDir.resolve("42").resolve(url.substring(url.lastIndexOf('/') + 1));
        assertThat(Files.exists(saved)).isTrue();
        assertThat(Files.size(saved)).isEqualTo(3);
    }

    @Test
    void saveImageRejectsInvalidExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> fileService.saveImage(1L, file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("jpg");
    }

    @Test
    void saveImageRejectsOversizedFile() {
        byte[] large = new byte[(int) (5 * 1024 * 1024) + 1];
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.jpg", "image/jpeg", large);

        assertThatThrownBy(() -> fileService.saveImage(1L, file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("5MB");
    }
}
