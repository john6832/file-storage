package io.metadata.filestorage.service;

import io.metadata.filestorage.exception.DeletingFileException;
import io.metadata.filestorage.exception.FileNotFoundException;
import io.metadata.filestorage.exception.InvalidPathException;
import io.metadata.filestorage.exception.PersistingFileException;
import io.metadata.filestorage.model.File;
import io.metadata.filestorage.model.Version;
import io.metadata.filestorage.model.dto.FileDTO;
import io.metadata.filestorage.model.dto.FileResponseDTO;
import io.metadata.filestorage.repository.FileRepository;
import io.metadata.filestorage.repository.VersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class FileService {

    private final FileRepository fileRepository;

    private final VersionRepository versionRepository;

    @Value("${file.database.path}")
    private String fileDatabasePath;

    private Path databasePath;

    @Autowired
    public FileService(FileRepository fileRepository, VersionRepository versionRepository) {
        this.fileRepository = fileRepository;
        this.versionRepository = versionRepository;
    }

    @PostConstruct
    public void init() {
        databasePath = Paths.get(fileDatabasePath).toAbsolutePath().normalize();

        if (!Files.exists(databasePath)) {
            try {
                Files.createDirectories(databasePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public List<FileDTO> findAll() {
        return fileRepository.findAllFiles();
    }

    public FileResponseDTO save(MultipartFile file) {

        if (isNotValid(file)) {
            throw new InvalidPathException();
        }

        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        Integer newVersionNumber = getNewVersionNumber(fileName);

        File dbFile = createDatabaseFile(fileName, newVersionNumber);

        try {
            persistFileOnFileSystem(fileName, newVersionNumber, file.getInputStream());
        } catch (IOException e) {
            throw new PersistingFileException(e);
        }

        dbFile = fileRepository.save(dbFile);

        return new FileResponseDTO(dbFile.getName(), newVersionNumber, file.getContentType(), file.getSize());

    }

    public FileResponseDTO save(String fileName, ByteArrayResource data) {

        fileName = StringUtils.cleanPath(fileName);

        Integer newVersionNumber = getNewVersionNumber(fileName);

        File dbFile = createDatabaseFile(fileName, newVersionNumber);

        try {
            persistFileOnFileSystem(fileName, newVersionNumber, data.getInputStream());
        } catch (IOException e) {
            throw new PersistingFileException(e);
        }

        dbFile = fileRepository.save(dbFile);

        return new FileResponseDTO(dbFile.getName(), newVersionNumber, null, data.contentLength());

    }

    public FileResponseDTO updateVersion(MultipartFile file, Integer version) {

        if (isNotValid(file)) {
            throw new InvalidPathException();
        }

        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            persistFileOnFileSystem(fileName, version, file.getInputStream());
        } catch (IOException e) {
            throw new PersistingFileException(e);
        }

        Version databaseVersion = versionRepository.findByFileNameAndVersionNumber(fileName, version);

        databaseVersion.setLastModificationDate(LocalDateTime.now());

        versionRepository.save(databaseVersion);

        return new FileResponseDTO(fileName, version, file.getContentType(), file.getSize());

    }

    public FileResponseDTO updateVersion(String fileName, ByteArrayResource data, Integer version) {

        fileName = StringUtils.cleanPath(fileName);

        try {
            persistFileOnFileSystem(fileName, version, data.getInputStream());
        } catch (IOException e) {
            throw new PersistingFileException(e);
        }

        Version databaseVersion = versionRepository.findByFileNameAndVersionNumber(fileName, version);

        if(databaseVersion == null){
            throw new FileNotFoundException(fileName + " with version "+version);
        }

        databaseVersion.setLastModificationDate(LocalDateTime.now());

        versionRepository.save(databaseVersion);

        return new FileResponseDTO(fileName, version, null, data.contentLength());

    }

    public void delete(String fileName) {

        File file = fileRepository.findByName(fileName);

        if(file == null){
            throw new FileNotFoundException(fileName);
        }

        for (Version version : file.getVersions()) {
            removeFromFileSystem(fileName, version.getVersionNumber());
        }

        fileRepository.delete(file);

    }

    public void delete(String fileName, Integer version) {

        File file = fileRepository.findByName(fileName);

        if(file == null){
            throw new FileNotFoundException(fileName);
        }

        Optional<Version> versionToRemove = file.getVersions().stream().filter(version1 -> version1.getVersionNumber().equals(version)).findFirst();

        if (versionToRemove.isPresent()) {
            removeFromFileSystem(fileName, versionToRemove.get().getVersionNumber());
            file.getVersions().remove(versionToRemove.get());
            versionRepository.deleteById(versionToRemove.get().getId());
        }else{
            throw new FileNotFoundException(fileName + " with version "+version);
        }

        fileRepository.save(file);

    }


    private boolean isNotValid(MultipartFile file) {

        if (file == null) {
            return true;
        }

        //Normalize the original file name

        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        //Validate path is valid

        return fileName.contains("..");
    }

    private Integer getNewVersionNumber(String fileName) {

        Integer latestVersionNumber = versionRepository.findLatestVersionNumber(fileName);

        if (latestVersionNumber == null) {
            latestVersionNumber = 0;
        }

        //Increase version number

        return ++latestVersionNumber;
    }

    private File createDatabaseFile(String fileName, Integer newVersionNumber) {
        File dbFile = fileRepository.findByName(fileName);

        if (dbFile == null) {

            //Create new file on database and add first version

            dbFile = new File();
            dbFile.setName(fileName);
            List<Version> versions = new ArrayList<>();
            versions.add(new Version(dbFile, newVersionNumber, LocalDateTime.now()));
            dbFile.setVersions(versions);

        } else {

            //Add new version to existing file

            List<Version> versions = dbFile.getVersions();

            if (versions != null && !versions.isEmpty()) {
                versions.add(new Version(dbFile, newVersionNumber, LocalDateTime.now()));
                dbFile.setVersions(versions);
            } else {
                versions = new ArrayList<>();
                versions.add(new Version(dbFile, newVersionNumber, LocalDateTime.now()));
                dbFile.setVersions(versions);
            }

        }

        return dbFile;
    }

    private void persistFileOnFileSystem(String fileName, Integer latestVersionNumber, InputStream inputStream) {
        Path destinationPath = this.databasePath.resolve(latestVersionNumber + fileName);
        try {
            Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new PersistingFileException(e);
        }
    }

    private void removeFromFileSystem(String fileName, Integer version) {
        Path destinationPath = this.databasePath.resolve(version + fileName);
        try {
            Files.deleteIfExists(destinationPath);
        } catch (IOException e) {
            throw new DeletingFileException(e);
        }
    }

    public Resource getResourceFile(String fileName, Integer version) {

        File file = fileRepository.findByName(fileName);

        if (file == null){
            throw new FileNotFoundException(fileName);
        }

        if(version == null) {
            version = versionRepository.findLatestVersionNumber(fileName);

            if (version == null){
                throw new FileNotFoundException(fileName + " with latest version ");
            }
        }

        return getResourceFile(version + fileName);
    }

    private Resource getResourceFile(String fileName) {
        try {
            Path filePath = databasePath.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new FileNotFoundException(fileName);
            }
        } catch (MalformedURLException ex) {
            throw new FileNotFoundException(fileName, ex);
        }
    }
}
