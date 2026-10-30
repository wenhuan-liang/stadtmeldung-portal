package service.interfaces;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface FileStorageService {

    String saveFile(
            InputStream inputStream,
            String originalFilename,
            String contentType
    ) throws IOException;

    Path loadFile(String storedFilename) throws IOException;

    void deleteFile(String storedFilename) throws IOException;
}