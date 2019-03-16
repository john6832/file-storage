package io.metadata.filestorage.repository;

import io.metadata.filestorage.model.Version;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VersionRepository extends JpaRepository<Version, Long> {

    @Query("select max(v.versionNumber) from Version v where v.file.name =:fileName")
    Integer findLatestVersionNumber(@Param("fileName") String fileName);

    @Query("select v from Version v where v.file.name =:fileName and v.versionNumber =:versionNumber")
    Version findByFileNameAndVersionNumber(@Param("fileName") String fileName, @Param("versionNumber") Integer versionNumber);
}
