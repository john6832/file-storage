package io.metadata.filestorage.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.metadata.filestorage.exception.DeletingFileException;
import io.metadata.filestorage.exception.FileNotFoundException;
import io.metadata.filestorage.exception.PersistingFileException;
import io.metadata.filestorage.model.dto.FileDTO;
import io.metadata.filestorage.model.dto.FileResponseDTO;
import io.metadata.filestorage.service.FileService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@WebMvcTest(value = FileController.class, secure = false)
public class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper jsonObjectMapper;

    @MockBean
    private FileService fileService;

    @Value("${file.database.path}")
    private String fileDatabasePath;

    private FileDTO fileDTO1;
    private FileResponseDTO fileResponseDTO;

    private final LocalDateTime dateTime = LocalDate.of(2018, 3, 15).atStartOfDay();

    @Before
    public void setUp() {
        fileDTO1 = new FileDTO("test.json", 3, dateTime);
        fileResponseDTO = new FileResponseDTO("test.json", 1, "application/json", 18L);
    }


    @Test
    public void testFindAllFiles() throws Exception {

        when(
                fileService.findAll()).thenReturn(Collections.singletonList(fileDTO1));

        RequestBuilder requestBuilder = MockMvcRequestBuilders.get(
                "/files/").accept(
                MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        List<FileDTO> resultList = jsonObjectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<FileDTO>>() {
        });
        FileDTO resultObject = resultList.get(0);

        assertNotNull(resultObject.getDownloadLink());

        assertEquals(fileDTO1.getName(), resultObject.getName());
        assertEquals(fileDTO1.getLastModificationDate(), resultObject.getLastModificationDate());
        assertEquals(fileDTO1.getLatestVersion(), resultObject.getLatestVersion());
    }

    @Test
    public void testDownloadFile() throws Exception {

        String content = "This is a test";

        when(
                fileService.getResourceFile(anyString(), any())).thenReturn(new InputStreamResource(new ByteArrayInputStream(content.getBytes())));

        RequestBuilder requestBuilder = MockMvcRequestBuilders.get(
                "/files/download/test.txt").accept(
                MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        String resultString = result.getResponse().getContentAsString();

        assertEquals(content, resultString);
    }

    @Test
    public void testDownloadNonExistentFile() throws Exception {

        when(fileService.getResourceFile(anyString(), any())).thenThrow(new FileNotFoundException("test.txt"));

        RequestBuilder requestBuilder = MockMvcRequestBuilders.get(
                "/files/download/test.txt").accept(
                MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        result.getResponse().getContentAsString();

        assertEquals(404, result.getResponse().getStatus());
        assertEquals(FileNotFoundException.class, Objects.requireNonNull(result.getResolvedException()).getClass());
        assertEquals("The file test.txt cannot be found", result.getResolvedException().getMessage());
    }

    @Test
    public void testUploadFile() throws Exception {

        MockMultipartFile jsonFile = new MockMultipartFile("file", "test.json", "application/json", "{\"key1\": \"value1\"}".getBytes());

        fileResponseDTO.setContentType(jsonFile.getContentType());
        fileResponseDTO.setSize(jsonFile.getSize());
        fileResponseDTO.setVersion(2);
        fileResponseDTO.setName(jsonFile.getOriginalFilename());

        when(
                fileService.save(any())).thenReturn(fileResponseDTO);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.multipart(
                "/files/upload").file(jsonFile).accept(
                MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        FileResponseDTO resultObject = jsonObjectMapper.readValue(result.getResponse().getContentAsString(), FileResponseDTO.class);

        assertNotNull(resultObject.getDownloadURI());
        assertTrue(resultObject.getDownloadURI().contains(fileResponseDTO.getVersion() + ""));

        assertEquals(fileResponseDTO.getName(), resultObject.getName());
        assertEquals(201, result.getResponse().getStatus());
    }

    @Test
    public void testUploadFileException() throws Exception {

        MockMultipartFile jsonFile = new MockMultipartFile("file", "test.json", "application/json", "{\"key1\": \"value1\"}".getBytes());

        when(
                fileService.save(any())).thenThrow(new PersistingFileException(new Exception()));

        RequestBuilder requestBuilder = MockMvcRequestBuilders.multipart(
                "/files/upload").file(jsonFile).accept(
                MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals(PersistingFileException.class, Objects.requireNonNull(result.getResolvedException()).getClass());

    }

    @Test
    public void testUploadBinaryFile() throws Exception {

        String content = "{\"key1\": \"value1\"}";

        fileResponseDTO.setSize((long) content.getBytes().length);
        fileResponseDTO.setVersion(2);
        fileResponseDTO.setName("test.json");

        when(
                fileService.save(anyString(), any())).thenReturn(fileResponseDTO);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.post(
                "/files/upload/test.json").content(content.getBytes()).accept(
                MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        FileResponseDTO resultObject = jsonObjectMapper.readValue(result.getResponse().getContentAsString(), FileResponseDTO.class);

        assertNotNull(resultObject.getDownloadURI());
        assertTrue(resultObject.getDownloadURI().contains(fileResponseDTO.getVersion() + ""));

        assertEquals(fileResponseDTO.getName(), resultObject.getName());
        assertEquals(201, result.getResponse().getStatus());

    }

    @Test
    public void testUpdateFileVersion() throws Exception {

        String content = "{\"key1\": \"value2\"}";

        fileResponseDTO.setSize((long) content.getBytes().length);
        fileResponseDTO.setVersion(2);
        fileResponseDTO.setName("test.json");

        MockMultipartFile jsonFile = new MockMultipartFile("file", "test.json", "application/json", content.getBytes());

        when(
                fileService.updateVersion(any(), anyInt())).thenReturn(fileResponseDTO);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.multipart(
                "/files/update").file(jsonFile).param("version", "2").accept(
                MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        FileResponseDTO resultObject = jsonObjectMapper.readValue(result.getResponse().getContentAsString(), FileResponseDTO.class);

        assertNotNull(resultObject.getDownloadURI());
        assertTrue(resultObject.getDownloadURI().contains(fileResponseDTO.getVersion() + ""));

        assertEquals(fileResponseDTO.getName(), resultObject.getName());
        assertEquals(200, result.getResponse().getStatus());
    }

    @Test
    public void testUpdateBinaryFileVersion() throws Exception {

        String content = "{\"key1\": \"value1\"}";

        fileResponseDTO.setSize((long) content.getBytes().length);
        fileResponseDTO.setVersion(2);
        fileResponseDTO.setName("test.json");

        when(
                fileService.updateVersion(anyString(), any(), anyInt())).thenReturn(fileResponseDTO);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.post(
                "/files/update/test.json").content(content.getBytes()).param("version", "2").accept(
                MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        FileResponseDTO resultObject = jsonObjectMapper.readValue(result.getResponse().getContentAsString(), FileResponseDTO.class);

        assertNotNull(resultObject.getDownloadURI());
        assertTrue(resultObject.getDownloadURI().contains(fileResponseDTO.getVersion() + ""));

        assertEquals(fileResponseDTO.getName(), resultObject.getName());
        assertEquals(200, result.getResponse().getStatus());

    }

    @Test
    public void testUpdateFileVersionException() throws Exception {

        String content = "{\"key1\": \"value2\"}";

        MockMultipartFile jsonFile = new MockMultipartFile("file", "test.json", "application/json", content.getBytes());

        when(
                fileService.updateVersion(any(), anyInt())).thenThrow(new PersistingFileException(new Exception()));

        RequestBuilder requestBuilder = MockMvcRequestBuilders.multipart(
                "/files/update").file(jsonFile).param("version", "2").accept(
                MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals(PersistingFileException.class, Objects.requireNonNull(result.getResolvedException()).getClass());

    }

    @Test
    public void testDeleteFile() throws Exception {

        doNothing().when(fileService).delete(anyString());

        RequestBuilder requestBuilder = MockMvcRequestBuilders.delete(
                "/files/").param("fileName", "test.json").accept(
                MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());
    }

    @Test
    public void testDeleteFileException() throws Exception {

        doThrow(new DeletingFileException(new Exception())).when(fileService).delete(anyString());

        RequestBuilder requestBuilder = MockMvcRequestBuilders.delete(
                "/files/")
                .param("fileName", "test.json").accept(
                        MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals(DeletingFileException.class, Objects.requireNonNull(result.getResolvedException()).getClass());
    }

    @Test
    public void testDeleteFileVersion() throws Exception {

        doNothing().when(fileService).delete(anyString(), anyInt());

        RequestBuilder requestBuilder = MockMvcRequestBuilders.delete(
                "/files/")
                .param("fileName", "test.json")
                .param("version", "2").accept(
                        MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());
    }

    @Test
    public void testDeleteFileVersionException() throws Exception {

        doThrow(new DeletingFileException(new Exception())).when(fileService).delete(anyString(), anyInt());

        RequestBuilder requestBuilder = MockMvcRequestBuilders.delete(
                "/files/")
                .param("fileName", "test.json")
                .param("version", "2").accept(
                        MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals(DeletingFileException.class, Objects.requireNonNull(result.getResolvedException()).getClass());
    }


}
