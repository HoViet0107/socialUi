package personal.social.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service để xử lý việc lưu trữ và quản lý file trong hệ thống chat
 */
@Service
public interface FileStorageService {

    // TODO: load file to s3 -> {file: url, status: DELETED/ACTIVE}


    String storeFile(MultipartFile file);

    Resource loadFile(String fileName);

    boolean deleteFile(String fileName);

    String getFileUrl(String fileName, String baseUrl);
}