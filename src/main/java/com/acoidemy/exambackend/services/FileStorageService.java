package com.acoidemy.exambackend.services;

import com.acoidemy.exambackend.enums.AttachmentType;
import com.acoidemy.exambackend.dtos.UploadResultDTO;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class FileStorageService {

    private final Cloudinary cloudinary;

    private static final long MAX_SIZE = 50L * 1024 * 1024; // 50 Mo

    private static final Map<String, AttachmentType> ALLOWED_TYPES = Map.ofEntries(
            Map.entry("application/pdf", AttachmentType.PDF),
            Map.entry("application/msword", AttachmentType.WORD),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", AttachmentType.WORD),
            Map.entry("text/plain", AttachmentType.TEXT),
            Map.entry("image/jpeg", AttachmentType.IMAGE),
            Map.entry("image/png", AttachmentType.IMAGE),
            Map.entry("image/gif", AttachmentType.IMAGE),
            Map.entry("image/webp", AttachmentType.IMAGE),
            Map.entry("image/bmp", AttachmentType.IMAGE),
            Map.entry("image/heic", AttachmentType.IMAGE),
            Map.entry("image/heif", AttachmentType.IMAGE),
            Map.entry("video/mp4", AttachmentType.VIDEO),
            Map.entry("video/webm", AttachmentType.VIDEO),
            Map.entry("video/quicktime", AttachmentType.VIDEO),
            Map.entry("video/x-matroska", AttachmentType.VIDEO),
            Map.entry("video/3gpp", AttachmentType.VIDEO),
            Map.entry("audio/mpeg", AttachmentType.AUDIO),
            Map.entry("audio/mp3", AttachmentType.AUDIO),
            Map.entry("audio/wav", AttachmentType.AUDIO),
            Map.entry("audio/x-wav", AttachmentType.AUDIO),
            Map.entry("audio/ogg", AttachmentType.AUDIO),
            Map.entry("audio/mp4", AttachmentType.AUDIO),
            Map.entry("audio/x-m4a", AttachmentType.AUDIO),
            Map.entry("audio/webm", AttachmentType.AUDIO),
            Map.entry("audio/aac", AttachmentType.AUDIO),
            Map.entry("audio/3gpp", AttachmentType.AUDIO)
    );

    public FileStorageService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret
    ) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

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

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "fichier"
        );

        String resourceType = switch (type) {
            case IMAGE -> "image";
            case VIDEO, AUDIO -> "video";
            default -> "raw";
        };

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", resourceType,
                "folder", "exam-attachments"
        ));
        String url = (String) uploadResult.get("secure_url");
        return new UploadResultDTO(url, type, originalName);
    }
}