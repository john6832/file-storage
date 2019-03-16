package io.metadata.filestorage.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Object encapsulating the response provided by the system for a new upload/update")
public class FileResponseDTO {

    public FileResponseDTO() {
    }

    public FileResponseDTO(String name, Integer version, String contentType, Long size) {
        this.name = name;
        this.version = version;
        this.contentType = contentType;
        this.size = size;
    }

    @ApiModelProperty(value = "Name of the file", example = "test.json")
    private String name;

    @ApiModelProperty(value = "Version number recorded on system", example = "2")
    private Integer version;

    @ApiModelProperty(value = "Download link", example = "http://localhost:8080/files/download/test.json?version=2")
    private String downloadURI;

    @ApiModelProperty(value = "Mime Type of the uploaded file", example = "application/json")
    private String contentType;

    @ApiModelProperty(value = "Size of bytes stored on the system", example = "50145")
    private Long size;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getDownloadURI() {
        return downloadURI;
    }

    public void setDownloadURI(String downloadURI) {
        this.downloadURI = downloadURI;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
