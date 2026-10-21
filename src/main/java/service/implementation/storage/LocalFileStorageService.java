package service.implementation.storage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

import service.interfaces.FileStorageService;

public class LocalFileStorageService implements FileStorageService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png"
    );

    private final Path uploadDirectory;

    public LocalFileStorageService(Path uploadDirectory)
            throws IOException {

        this.uploadDirectory =
                uploadDirectory.toAbsolutePath().normalize();

        Files.createDirectories(this.uploadDirectory);
    }

    @Override
    public String saveFile(
            InputStream inputStream,
            String originalFilename,
            String contentType) throws IOException {

        if (inputStream == null) {
            throw new IllegalArgumentException(
                    "File input stream must not be null");
        }

        String normalizedType = contentType == null
                ? ""
                : contentType.toLowerCase(Locale.ROOT).trim();

        if (!ALLOWED_TYPES.contains(normalizedType)) {
            throw new IllegalArgumentException(
                    "Only JPEG and PNG images are allowed");
        }

        // Maximal 5 MB lesen
        byte[] bytes = inputStream.readNBytes(
                (int) MAX_FILE_SIZE + 1
        );

        if (bytes.length == 0) {
            throw new IllegalArgumentException(
                    "File must not be empty");
        }

        if (bytes.length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File must not exceed 5 MB");
        }

        // Dateisignatur prüfen
        boolean jpeg = isJpeg(bytes);
        boolean png = isPng(bytes);

        if (!jpeg && !png) {
            throw new IllegalArgumentException(
                    "Invalid image file");
        }

        if (jpeg && !normalizedType.equals("image/jpeg")) {
            throw new IllegalArgumentException(
                    "Content type does not match JPEG file");
        }

        if (png && !normalizedType.equals("image/png")) {
            throw new IllegalArgumentException(
                    "Content type does not match PNG file");
        }

        // Sicherstellen, dass Bild dekodierbar ist
        BufferedImage image = ImageIO.read(
                new ByteArrayInputStream(bytes)
        );

        if (image == null) {
            throw new IllegalArgumentException(
                    "File is not a valid image");
        }

        // Zufälliger, serverseitig erzeugter Dateiname
        String extension = jpeg ? ".jpg" : ".png";

        String storedFilename =
                UUID.randomUUID() + extension;

        Path target = resolveStoredFile(storedFilename);

        Files.write(
                target,
                bytes,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );

        return storedFilename;
    }

    @Override
    public Path loadFile(String storedFilename) throws IOException {

        Path file = resolveStoredFile(storedFilename);

        if (!Files.isRegularFile(file)) {
            throw new IOException(
                    "File not found: " + storedFilename);
        }

        return file;
    }

    @Override
    public void deleteFile(String storedFilename)
            throws IOException {

        Path file = resolveStoredFile(storedFilename);

        Files.deleteIfExists(file);
    }

    private Path resolveStoredFile(String storedFilename) {

        if (storedFilename == null ||
                !storedFilename.matches(
                        "[0-9a-fA-F-]{36}\\.(jpg|png)")) {

            throw new IllegalArgumentException(
                    "Invalid stored filename");
        }

        Path target = uploadDirectory
                .resolve(storedFilename)
                .normalize();

        if (!target.getParent().equals(uploadDirectory)) {
            throw new SecurityException(
                    "Invalid file location");
        }

        return target;
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] bytes) {

        byte[] signature = {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };

        if (bytes.length < signature.length) {
            return false;
        }

        for (int i = 0; i < signature.length; i++) {
            if (bytes[i] != signature[i]) {
                return false;
            }
        }

        return true;
    }
}
