package io.metadata.filestorage.repository;

import io.metadata.filestorage.model.File;
import io.metadata.filestorage.model.dto.FileDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {

    @Query("select new io.metadata.filestorage.model.dto.FileDTO(f.name, max(v.versionNumber), max(v.lastModificationDate)) from File f inner join f.versions v group by f.id")
    List<FileDTO> findAllFiles();

    File findByName(String name);
}
