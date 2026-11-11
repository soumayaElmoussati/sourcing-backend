package app_sourcing.sellerModule.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public String storeFile(MultipartFile file, String subDirectory) throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString() + "_" + originalFileName;

        Path targetLocation = Paths.get(uploadDir).resolve(subDirectory).resolve(fileName);

        Files.createDirectories(targetLocation.getParent());

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return subDirectory + "/" + fileName;
    }

    public List<String> storeFiles(List<MultipartFile> images,String subDirectory){
        List<String> paths=new ArrayList<>();
        images.forEach(img->{
            try {
                String path=storeFile(img,subDirectory);
                paths.add(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return  paths;
    }

    public boolean deleteFile(String filePath) {
        try {
            Path fullPath = Paths.get(uploadDir).resolve(filePath);
            return Files.deleteIfExists(fullPath);
        } catch (IOException e) {
            return false;
        }
    }

    public byte[] loadFile(String filePath) throws IOException {
        Path fullPath = Paths.get(uploadDir).resolve(filePath);
        return Files.readAllBytes(fullPath);
    }
}