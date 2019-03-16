package io.metadata.filestorage.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
public class Version {

    public Version() {
    }

    public Version(File file, Integer versionNumber, LocalDateTime lastModificationDate) {
        this.versionNumber = versionNumber;
        this.file = file;
        this.lastModificationDate = lastModificationDate;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer versionNumber;

    private LocalDateTime lastModificationDate;

    @ManyToOne
    private File file;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public LocalDateTime getLastModificationDate() {
        return lastModificationDate;
    }

    public void setLastModificationDate(LocalDateTime lastModificationDate) {
        this.lastModificationDate = lastModificationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version version = (Version) o;
        return Objects.equals(getId(), version.getId()) &&
                Objects.equals(getVersionNumber(), version.getVersionNumber()) &&
                Objects.equals(getFile(), version.getFile());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getVersionNumber(), getFile());
    }
}
