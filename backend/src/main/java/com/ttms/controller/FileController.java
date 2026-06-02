package com.ttms.controller;

import com.ttms.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器
 * 处理图片等文件上传
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class FileController {

    @Value("${file.upload.path:./uploads/}")
    private String uploadPath;

    /**
     * 上传文件
     * POST /api/upload
     *
     * @param file 上传的文件
     * @return 文件访问URL
     */
    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.error("请选择文件");
        }

        // Validate file type: 1) Content-Type header (快速初步检查)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ApiResponse.error("只允许上传图片文件");
        }

        // Validate file type: 2) Magic bytes (文件魔数，防止Content-Type伪造)
        if (!isValidImageFile(file)) {
            log.warn("检测到伪造图片文件: Content-Type={}, 文件名={}", contentType, file.getOriginalFilename());
            return ApiResponse.error("文件类型校验失败，仅支持JPG/PNG/GIF/BMP/WebP格式");
        }

        // Validate file size (10MB max)
        if (file.getSize() > 10 * 1024 * 1024) {
            return ApiResponse.error("文件大小不能超过10MB");
        }

        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String filename = UUID.randomUUID().toString().replace("-", "") + extension;

            // Create directory: uploads/posters/yyyyMMdd/
            String dirPath = uploadPath + "posters/" + dateDir + "/";
            File dir = new File(dirPath);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    log.error("创建上传目录失败: {}", dirPath);
                    return ApiResponse.error("上传服务异常，请稍后重试");
                }
            }

            // Save file
            Path filePath = Paths.get(dirPath + filename);
            Files.write(filePath, file.getBytes());

            // Return accessible URL
            String fileUrl = "/uploads/posters/" + dateDir + "/" + filename;
            log.info("文件上传成功: {} -> {}", originalFilename, fileUrl);

            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            return ApiResponse.success("上传成功", result);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return ApiResponse.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 通过文件魔数（Magic Bytes）验证是否为合法的图片文件
     * 防止攻击者伪造 Content-Type 头绕过类型检查上传恶意文件
     *
     * @param file 上传的文件
     * @return true 表示是合法的图片文件
     */
    private boolean isValidImageFile(MultipartFile file) {
        try {
            byte[] header = file.getBytes();
            if (header.length < 4) {
                return false;
            }

            // JPEG: FF D8 FF
            if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
                return true;
            }
            // PNG: 89 50 4E 47
            if (header[0] == (byte) 0x89 && header[1] == (byte) 0x50
                && header[2] == (byte) 0x4E && header[3] == (byte) 0x47) {
                return true;
            }
            // GIF: 47 49 46 38 (GIF8)
            if (header[0] == (byte) 0x47 && header[1] == (byte) 0x49
                && header[2] == (byte) 0x46 && header[3] == (byte) 0x38) {
                return true;
            }
            // BMP: 42 4D
            if (header[0] == (byte) 0x42 && header[1] == (byte) 0x4D) {
                return true;
            }
            // WebP: 52 49 46 46 (RIFF) + WEBP at offset 8
            if (header.length >= 12
                && header[0] == (byte) 0x52 && header[1] == (byte) 0x49
                && header[2] == (byte) 0x46 && header[3] == (byte) 0x46
                && header[8] == (byte) 0x57 && header[9] == (byte) 0x45
                && header[10] == (byte) 0x42 && header[11] == (byte) 0x50) {
                return true;
            }

            return false;
        } catch (IOException e) {
            log.warn("文件魔数读取失败: {}", e.getMessage());
            return false;
        }
    }
}
