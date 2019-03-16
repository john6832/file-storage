package io.metadata.filestorage.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.time.LocalDateTime;

@ApiModel(description = "Object encapsulating the info of every item on the file list")
public class FileDTO implements Serializable {

    public FileDTO() {
    }

    public FileDTO(String name, Integer latestVersion, LocalDateTime lastModificationDate) {
        this.name = name;
        this.latestVersion = latestVersion;
        this.lastModificationDate = lastModificationDate;
    }

    @ApiModelProperty(value = "Name of the file", example = "test.json")
    private String name;

    @ApiModelProperty(value = "Latest version number recorded on system", example = "2")
    private Integer latestVersion;

    @ApiModelProperty(value = "Latest time a new version was added on the system", example = "2019-02-02T00:00:00")
    private LocalDateTime lastModificationDate;

    @ApiModelProperty(value = "Download link", example = "http://localhost:8080/files/download/test.json?version=2")
    private String downloadLink;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(Integer latestVersion) {
        this.latestVersion = latestVersion;
    }

    public String getDownloadLink() {
        return downloadLink;
    }

    public void setDownloadLink(String downloadLink) {
        this.downloadLink = downloadLink;
    }

    public LocalDateTime getLastModificationDate() {
        return lastModificationDate;
    }

    public void setLastModificationDate(LocalDateTime lastModificationDate) {
        this.lastModificationDate = lastModificationDate;
    }
}
