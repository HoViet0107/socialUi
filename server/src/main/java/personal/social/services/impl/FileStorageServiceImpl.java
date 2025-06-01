package personal.social.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import personal.social.services.FileStorageService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {
    private final Path fileStorageLocation;

    /**
     * Khởi tạo service với đường dẫn lưu trữ file từ application.properties
     *
     * @param fileStorageLocation Đường dẫn thư mục lưu trữ file
     */
    @Autowired
    public FileStorageServiceImpl(@Value("${file.upload-dir:uploads}") String fileStorageLocation) {
        this.fileStorageLocation = Paths.get(fileStorageLocation).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Không thể tạo thư mục để lưu trữ file", ex);
        }
    }

    /**
     * Lưu trữ file được tải lên
     *
     * @param file File cần lưu trữ
     * @return Tên file đã được lưu trữ
     */
    @Override
    public String storeFile(MultipartFile file) {
        // Chuẩn hóa tên file
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        // Kiểm tra tên file hợp lệ
        if (originalFilename.contains("..")) {
            throw new RuntimeException("Tên file không hợp lệ: " + originalFilename);
        }

        // Tạo tên file duy nhất để tránh trùng lặp
        String fileExtension = "";
        if (originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + fileExtension;

        try {
            // Lưu file vào thư mục đích
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Không thể lưu trữ file " + originalFilename, ex);
        }
    }

    /**
     * Tải file từ hệ thống lưu trữ
     *
     * @param fileName T��n file cần tải
     * @return Resource chứa nội dung file
     */
    @Override
    public Resource loadFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File không tồn tại: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File không tồn tại: " + fileName, ex);
        }
    }

    /**
     * Xóa file từ hệ thống lưu trữ
     *
     * @param fileName Tên file cần xóa
     * @return true nếu xóa thành công, false nếu không
     */
    @Override
    public boolean deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            return Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Không thể xóa file: " + fileName, ex);
        }
    }

    /**
     * Lấy URL để truy cập file
     *
     * @param fileName Tên file
     * @param baseUrl  URL cơ sở của ứng dụng
     * @return URL đầy đủ để truy cập file
     */
    @Override
    public String getFileUrl(String fileName, String baseUrl) {
        return baseUrl + "/api/files/" + fileName;
    }

    // TODO: Implement actual file upload logic
    /**
     * 📤 Upload file to storage service (S3, CloudFront, etc.)
     */
    private String uploadFileToStorage(MultipartFile file) throws IOException {
        // TODO: Implement actual file upload logic
        // Ví dụ với S3:
        /*
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        String bucketName = "your-bucket-name";

        // Upload to S3
        s3Client.putObject(PutObjectRequest.builder()
            .bucket(bucketName)
            .key(fileName)
            .contentType(file.getContentType())
            .contentLength(file.getSize())
            .build(),
            RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return "https://" + bucketName + ".s3.amazonaws.com/" + fileName;
        */

        // 🔧 TEMPORARY: Return mock URL for development
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        return "https://your-cdn.com/uploads/" + fileName;
    }

    /**
     * 🎭 Determine media type from file
     */
    private String determineMediaType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) return "FILE";

        if (contentType.startsWith("image/")) return "IMAGE";
        if (contentType.startsWith("video/")) return "VIDEO";
        if (contentType.startsWith("audio/")) return "AUDIO";

        // 📄 Specific document types
        if (contentType.contains("pdf")) return "PDF";
        if (contentType.contains("word")) return "DOCUMENT";
        if (contentType.contains("excel") || contentType.contains("spreadsheet")) return "SPREADSHEET";

        return "FILE";
    }

    /**
     * 🖼️ Generate thumbnail URL for media
     */
    private String generateThumbnailUrl(String mediaUrl, String mediaType) {
        if ("IMAGE".equals(mediaType)) {
            // For images, return smaller version or same URL
            return mediaUrl.replace("/uploads/", "/thumbnails/");
        }
        if ("VIDEO".equals(mediaType)) {
            // For videos, return video thumbnail
            return mediaUrl.replace(".mp4", "_thumb.jpg").replace("/uploads/", "/thumbnails/");
        }

        // For other types, return default icon or same URL
        return mediaUrl;
    }
}
