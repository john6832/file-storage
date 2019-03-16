package io.metadata.filestorage.service;

import io.metadata.filestorage.FileStorageApplication;
import io.metadata.filestorage.exception.InvalidPathException;
import io.metadata.filestorage.model.dto.FileDTO;
import io.metadata.filestorage.model.dto.FileResponseDTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = {FileStorageApplication.class, FileService.class})
public class FileServiceTest {

    @Autowired
    private FileService fileService;

    private static final MockMultipartFile JSON_MULTIPART_FILE = new MockMultipartFile(
           "file",
           "test1.json",
           "application/json", "{\"key1\": \"value1\"}".getBytes());


    @Test
    public void testFindAll() {

        MockMultipartFile jsonFile1 = new MockMultipartFile("file", "test1.json", "application/json", "{\"key1\": \"value1\"}".getBytes());
        MockMultipartFile jsonFile2 = new MockMultipartFile("file", "test2.json", "application/json", "{\"key1\": \"value2\"}".getBytes());
        MockMultipartFile jsonFile3 = new MockMultipartFile("file", "test3.json", "application/json", "{\"key1\": \"value3\"}".getBytes());

        fileService.save(jsonFile1);
        fileService.save(jsonFile2);
        fileService.save(jsonFile3);

        List<FileDTO> fileDTOS = fileService.findAll();

        assertEquals(3, fileDTOS.size());

    }

    @Test
    public void testSave() {

        FileResponseDTO fileResponseDTO = fileService.save(JSON_MULTIPART_FILE);

        assertEquals(JSON_MULTIPART_FILE.getOriginalFilename(), fileResponseDTO.getName());
        assertEquals(JSON_MULTIPART_FILE.getContentType(), fileResponseDTO.getContentType());
        assertEquals(1, (int) fileResponseDTO.getVersion());

    }

    @Test(expected = InvalidPathException.class)
    public void testSaveFileWithInvalidPath() {

        MockMultipartFile jsonFile = new MockMultipartFile(
                "file",
                "/../../test1.json",
                "application/json", "{\"key1\": \"value1\"}".getBytes());

        fileService.save(jsonFile);
    }


    @Test(expected = InvalidPathException.class)
    public void testSaveNoFile() {
        fileService.save(null);
    }

    @Test
    public void testSaveMultipleVersions() {

        FileResponseDTO fileResponseDTO = fileService.save(JSON_MULTIPART_FILE);

        assertEquals(JSON_MULTIPART_FILE.getOriginalFilename(), fileResponseDTO.getName());
        assertEquals(JSON_MULTIPART_FILE.getContentType(), fileResponseDTO.getContentType());
        assertEquals(1, (int) fileResponseDTO.getVersion());

        fileResponseDTO = fileService.save(JSON_MULTIPART_FILE);

        assertEquals(JSON_MULTIPART_FILE.getOriginalFilename(), fileResponseDTO.getName());
        assertEquals(JSON_MULTIPART_FILE.getContentType(), fileResponseDTO.getContentType());
        assertEquals(2, (int) fileResponseDTO.getVersion());

    }


    @Test
    public void testSaveBinaryFile() {

        FileResponseDTO fileResponseDTO = fileService.save("test.json", new ByteArrayResource("This is a test".getBytes()));

        assertEquals("test.json", fileResponseDTO.getName());
        assertEquals(1, (int) fileResponseDTO.getVersion());

    }

    @Test
    public void testSaveBinaryFileMultipleVersions() {

        FileResponseDTO fileResponseDTO = fileService.save("test.json", new ByteArrayResource("This is a test".getBytes()));

        assertEquals("test.json", fileResponseDTO.getName());
        assertEquals(1, (int) fileResponseDTO.getVersion());

        fileResponseDTO = fileService.save("test.json", new ByteArrayResource("This is a test".getBytes()));

        assertEquals("test.json", fileResponseDTO.getName());
        assertEquals(2, (int) fileResponseDTO.getVersion());

    }


    @Test
    public void testUpdateVersion() {

        FileResponseDTO fileResponseDTO = fileService.save(JSON_MULTIPART_FILE);

        assertEquals(JSON_MULTIPART_FILE.getOriginalFilename(), fileResponseDTO.getName());
        assertEquals(JSON_MULTIPART_FILE.getContentType(), fileResponseDTO.getContentType());
        assertEquals(1, (int) fileResponseDTO.getVersion());

        fileResponseDTO = fileService.updateVersion(JSON_MULTIPART_FILE, 1);

        assertEquals(JSON_MULTIPART_FILE.getOriginalFilename(), fileResponseDTO.getName());
        assertEquals(JSON_MULTIPART_FILE.getContentType(), fileResponseDTO.getContentType());
        assertEquals(1, (int) fileResponseDTO.getVersion());

        List<FileDTO> fileDTOS = fileService.findAll();

        assertEquals(1, fileDTOS.size());

    }

    @Test
    public void testUpdateBinaryVersion() {

        fileService.save("test.json", new ByteArrayResource("This is a test".getBytes()));

        FileResponseDTO fileResponseDTO = fileService.updateVersion(
                "test.json",
                new ByteArrayResource("This is a test".getBytes()),
                1);

        assertEquals("test.json", fileResponseDTO.getName());
        assertEquals(1, (int) fileResponseDTO.getVersion());

        fileResponseDTO = fileService.updateVersion(
                "test.json",
                new ByteArrayResource("This is a second test".getBytes()),
                1);

        assertEquals("test.json", fileResponseDTO.getName());
        assertEquals(1, (int) fileResponseDTO.getVersion());

        List<FileDTO> fileDTOS = fileService.findAll();

        assertEquals(1, fileDTOS.size());



    }

    @Test
    public void testDelete() {

        fileService.save(JSON_MULTIPART_FILE);

        fileService.save(JSON_MULTIPART_FILE);

        fileService.delete("test1.json");

        List<FileDTO> fileDTOS = fileService.findAll();

        assertEquals(0, fileDTOS.size());

    }

    @Test
    public void testDeleteVersion() {



        fileService.save(JSON_MULTIPART_FILE);

        fileService.save(JSON_MULTIPART_FILE);

        fileService.delete("test1.json", 2);

        List<FileDTO> fileDTOS = fileService.findAll();

        assertEquals(1, fileDTOS.size());
        assertEquals(1, (int) fileDTOS.get(0).getLatestVersion());

    }

    @Test
    public void testGetResourceFile() {

        fileService.save(JSON_MULTIPART_FILE);

        Resource resource = fileService.getResourceFile("test1.json", null);

        assertNotNull(resource);
        assertEquals("1test1.json", resource.getFilename());

    }

}
