package com.easywiki.service;

import com.easywiki.config.UploadProperties;
import com.easywiki.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final UploadProperties uploadProperties;

    public FileService(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    public String saveImage(Long groupId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException(400, "文件大小不能超过 5MB");
        }

        String ext = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(400, "仅支持 jpg、png、gif、webp 格式");
        }

        String normalizedExt = "jpeg".equals(ext) ? "jpg" : ext;
        String filename = UUID.randomUUID() + "." + normalizedExt;
        Path targetDir = Paths.get(uploadProperties.getPath(), String.valueOf(groupId));
        Path targetFile = targetDir.resolve(filename);

        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetFile.toFile());
        } catch (IOException e) {
            throw new BusinessException(500, "文件保存失败");
        }

        return "/uploads/" + groupId + "/" + filename;
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BusinessException(400, "无效的文件名");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
