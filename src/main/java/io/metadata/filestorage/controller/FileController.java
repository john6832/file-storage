package io.metadata.filestorage.controller;

import io.metadata.filestorage.model.dto.FileDTO;
import io.metadata.filestorage.model.dto.FileResponseDTO;
import io.metadata.filestorage.service.FileService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/files")
public class FileController {


    private static final Logger LOGGER = LoggerFactory.getLogger(FileController.class);

    private final FileService fileService;

    private final String DOWNLOAD_URI_TEMPLATE = "/files/download/{fileName}";

    @Autowired
    public FileController(FileService fileService) {
        this.fileService = fileService;
    }


    @GetMapping("/")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(httpMethod = "GET", value = "View a list of files stored on the system", response = FileDTO.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully retrieved list")
    }
    )
    public List<FileDTO> findAll() {
        return fileService.findAll().stream().peek(fileDTO -> {
            String downloadURI =
                    ServletUriComponentsBuilder.fromCurrentContextPath().path(DOWNLOAD_URI_TEMPLATE)
                            .queryParam("version", fileDTO.getLatestVersion())
                            .buildAndExpand(fileDTO.getName())
                            .toUri().toASCIIString();
            fileDTO.setDownloadLink(downloadURI);
        }).collect(Collectors.toList());
    }


    @GetMapping("/download/{fileName:.+}")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(httpMethod = "GET", value = "Download file stored on the system", response = Resource.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "File successfully retrieved for download"),
            @ApiResponse(code = 404, message = "File not found on records")
    }
    )
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String fileName,
            @RequestParam(required = false) Integer version,
            HttpServletRequest request) {

        Resource resource = fileService.getResourceFile(fileName, version);

        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            LOGGER.info("Could not determine content type.");
        }

        // Setting default content type
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(httpMethod = "POST", value = "Upload file to the system using multipart parameters", response = FileResponseDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "File successfully stored on the system"),
            @ApiResponse(code = 400, message = "Invalid file or path")
    }
    )
    public FileResponseDTO uploadFile(@RequestPart MultipartFile file) {
        FileResponseDTO fileResponseDTO = fileService.save(file);

        fileResponseDTO.setDownloadURI(getDownloadURI(fileResponseDTO));

        return fileResponseDTO;
    }

    @PostMapping("/upload/{fileName}")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(
            httpMethod = "POST",
            value = "Upload file to the system using a file name and binary body content",
            response = FileResponseDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "File successfully stored on the system"),
            @ApiResponse(code = 400, message = "Invalid file or path")
    }
    )
    public FileResponseDTO uploadBinaryFile(@PathVariable("fileName") String fileName, @RequestBody ByteArrayResource data) {
        FileResponseDTO fileResponseDTO = fileService.save(fileName, data);

        fileResponseDTO.setDownloadURI(getDownloadURI(fileResponseDTO));

        return fileResponseDTO;
    }

    @PostMapping("/update")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(httpMethod = "POST",
            value = "Update a specific version of the file using multipart parameters",
            notes = "In case you want to update the file and create a new version, please use the upload end point",
            response = FileResponseDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "File successfully updated"),
            @ApiResponse(code = 404, message = "File not found"),
            @ApiResponse(code = 400, message = "Invalid file or path")
    }
    )
    public FileResponseDTO updateFileVersion(@RequestPart MultipartFile file, @RequestParam Integer version) {
        FileResponseDTO fileResponseDTO = fileService.updateVersion(file, version);

        fileResponseDTO.setDownloadURI(getDownloadURI(fileResponseDTO));

        return fileResponseDTO;
    }

    @PostMapping("/update/{fileName}")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(httpMethod = "POST",
            value = "Update a specific version of the file using a file name and binary body content",
            notes = "In case you want to update the file and create a new version, please use the upload end point",
            response = FileResponseDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "File successfully updated"),
            @ApiResponse(code = 404, message = "File not found"),
            @ApiResponse(code = 400, message = "Invalid file or path")
    }
    )
    public FileResponseDTO updateBinaryFileVersion(
            @PathVariable("fileName") String fileName,
            @RequestBody ByteArrayResource data,
            @RequestParam Integer version) {
        FileResponseDTO fileResponseDTO = fileService.updateVersion(fileName, data, version);

        fileResponseDTO.setDownloadURI(getDownloadURI(fileResponseDTO));

        return fileResponseDTO;
    }

    @DeleteMapping("/")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(httpMethod = "DELETE",
            value = "Delete a file or a specific version of the file",
            notes = "If th version is not specified, then the entire file with all its versions are deleted")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "File successfully deleted"),
            @ApiResponse(code = 404, message = "File not found"),
            @ApiResponse(code = 400, message = "File was not deleted with provided data")
    }
    )
    public ResponseEntity deleteFile(@RequestParam String fileName, @RequestParam(required = false) Integer version) {

        if (version != null) {
            // Delete only an specific version
            fileService.delete(fileName, version);
        } else {
            // Delete the entire file
            fileService.delete(fileName);
        }

        return ResponseEntity.ok().build();
    }

    private String getDownloadURI(FileResponseDTO fileResponseDTO) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(DOWNLOAD_URI_TEMPLATE).queryParam("version", fileResponseDTO.getVersion())
                .buildAndExpand(fileResponseDTO.getName())
                .toUri().toASCIIString();
    }

}
