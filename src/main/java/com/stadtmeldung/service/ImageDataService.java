package com.stadtmeldung.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class ImageDataService {
    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final Path UPLOAD_DIRECTORY =
            Path.of("uploads").toAbsolutePath().normalize();

    public String saveDataUrl(String dataUrl) throws IOException {
        String[] parts = dataUrl.split(",", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Ungültiges Foto.");
        }
        String extension = switch (parts[0]) {
            case "data:image/jpeg;base64" -> "jpg";
            case "data:image/png;base64" -> "png";
            case "data:image/webp;base64" -> "webp";
            case "data:image/gif;base64" -> "gif";
            default -> throw new IllegalArgumentException(
                    "Nur JPG-, PNG-, GIF- oder WEBP-Fotos sind erlaubt.");
        };

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(parts[1]);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Ungültiges Foto.");
        }
        if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException(
                    "Fotos dürfen höchstens 5 MB groß sein.");
        }

        Files.createDirectories(UPLOAD_DIRECTORY);
        Path destination = UPLOAD_DIRECTORY.resolve(
                UUID.randomUUID() + "." + extension);
        Files.write(destination, bytes);
        return "/uploads/" + destination.getFileName();
    }

    public void deleteSavedFile(String publicPath) throws IOException {
        if (publicPath == null || !publicPath.startsWith("/uploads/")) {
            return;
        }
        Path file = UPLOAD_DIRECTORY.resolve(
                publicPath.substring("/uploads/".length())).normalize();
        if (file.startsWith(UPLOAD_DIRECTORY)) {
            Files.deleteIfExists(file);
        }
    }
}
