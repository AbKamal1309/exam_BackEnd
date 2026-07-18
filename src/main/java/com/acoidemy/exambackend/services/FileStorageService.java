package com.acoidemy.exambackend.services;

import com.acoidemy.exambackend.enums.AttachmentType;
import com.acoidemy.exambackend.dtos.UploadResultDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final long MAX_SIZE = 50L * 1024 * 1024; // 50 Mo

    private static final Map<String, AttachmentType> ALLOWED_TYPES = Map.ofEntries(
            Map.entry("application/pdf", AttachmentType.PDF),
            Map.entry("application/msword", AttachmentType.WORD),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", AttachmentType.WORD),
            Map.entry("image/jpeg", AttachmentType.IMAGE),
            Map.entry("image/png", AttachmentType.IMAGE),
            Map.entry("image/gif", AttachmentType.IMAGE),
            Map.entry("image/webp", AttachmentType.IMAGE),
            Map.entry("video/mp4", AttachmentType.VIDEO),
            Map.entry("video/webm", AttachmentType.VIDEO),
            Map.entry("video/quicktime", AttachmentType.VIDEO),
            Map.entry("audio/mpeg", AttachmentType.AUDIO),
            Map.entry("audio/mp3", AttachmentType.AUDIO),
            Map.entry("audio/wav", AttachmentType.AUDIO),
            Map.entry("audio/x-wav", AttachmentType.AUDIO),
            Map.entry("audio/ogg", AttachmentType.AUDIO),
            Map.entry("audio/mp4", AttachmentType.AUDIO),      // .m4a est souvent envoyé avec ce type
            Map.entry("audio/x-m4a", AttachmentType.AUDIO),
            Map.entry("audio/webm", AttachmentType.AUDIO)
    );

    public UploadResultDTO store(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Fichier vide.");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("Fichier trop volumineux (max 50 Mo).");
        }

        String contentType = file.getContentType();
        AttachmentType type = ALLOWED_TYPES.get(contentType);
        if (type == null) {
            throw new IllegalArgumentException("Type de fichier non autorisé : " + contentType);
        }

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "fichier"
        );
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex);
        }

        String storedName = UUID.randomUUID() + extension;
        Path targetPath = uploadPath.resolve(storedName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        String url = "/uploads/" + storedName;
        return new UploadResultDTO(url, type, originalName);
    }
}