package com.example.file_storage.controller;

import com.example.file_storage.dto.ResourceDTO;
import com.example.file_storage.entity.ResourceType;
import com.example.file_storage.exception.ApiExceptionHandler;
import com.example.file_storage.service.ResourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import org.springframework.security.test.context.support.WithMockUser;

@WebMvcTest(ResourceController.class)
@Import(ApiExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "alice")
class ResourceControllerWebMvcTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ResourceService resourceService;

    private static ResourceDTO fileDto(String path, String name, long size) {
        return new ResourceDTO(path, name, size, ResourceType.FILE);
    }

    private static ResourceDTO dirDto(String path, String name) {
        return new ResourceDTO(path, name, null, ResourceType.DIRECTORY);
    }

    @Test
    void getResource_valid() throws Exception {
        ResourceDTO dto = fileDto("docs/", "file.txt", 12L);
        when(resourceService.getResourceInfo("docs/file.txt", "alice")).thenReturn(dto);

        mvc.perform(get("/api/resource")
                        .param("path", "docs/file.txt")
                        .with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("docs/"))
                .andExpect(jsonPath("$.name").value("file.txt"))
                .andExpect(jsonPath("$.size").value(12))
                .andExpect(jsonPath("$.type").value("FILE"));
    }

    @Test
    void getResource_invalidPath() throws Exception {
        mvc.perform(get("/api/resource")
                        .param("path", "/abs/file.txt")
                        .with(user("alice")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(resourceService);
    }

    @Test
    void getResource_backslashPath() throws Exception {
        mvc.perform(get("/api/resource")
                        .param("path", "docs\\file.txt")
                        .with(user("alice")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(resourceService);
    }

    @Test
    void deleteResource_valid() throws Exception {
        doNothing().when(resourceService).deleteResource("docs/file.txt", "alice");

        mvc.perform(delete("/api/resource")
                        .param("path", "docs/file.txt")
                        .with(user("alice")))
                .andExpect(status().isNoContent());
    }

    @Test
    void downloadResource_valid() throws Exception {
        StreamingResponseBody body = out -> { };
        when(resourceService.download("docs/file.txt", "alice"))
                .thenReturn(new ResourceService.DownloadPayload(body, "file.txt"));

        mvc.perform(get("/api/resource/download")
                        .param("path", "docs/file.txt")
                        .with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("file.txt")));
    }

    @Test
    void moveResource_valid() throws Exception {
        ResourceDTO dto = fileDto("docs/", "new.txt", 5L);
        when(resourceService.moveResource("docs/old.txt", "docs/new.txt", "alice")).thenReturn(dto);

        mvc.perform(get("/api/resource/move")
                        .param("from", "docs/old.txt")
                        .param("to", "docs/new.txt")
                        .with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("new.txt"));
    }

    @Test
    void moveResource_blankOnlyTargetName_returnsBadRequest() throws Exception {
        doThrow(new IllegalArgumentException("Path must not contain blank names"))
                .when(resourceService).moveResource("docs/file.txt", "docs/ ", "alice");

        mvc.perform(get("/api/resource/move")
                        .param("from", "docs/file.txt")
                        .param("to", "docs/ ")
                        .with(user("alice")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Path must not contain blank names"));
    }

    @Test
    void search_valid() throws Exception {
        ResourceDTO dto = fileDto("docs/", "report.txt", 7L);
        when(resourceService.find(eq("report"), eq("alice"), any()))
                .thenReturn(List.of(dto));

        mvc.perform(get("/api/resource/search")
                        .param("query", "report")
                        .with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("report.txt"));
    }

    @Test
    void search_tooLongQuery() throws Exception {
        StringBuilder sb = new StringBuilder();
        while (sb.length() < 257) {
            sb.append('a');
        }

        mvc.perform(get("/api/resource/search")
                        .param("query", sb.toString())
                        .with(user("alice")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(resourceService);
    }

    @Test
    void upload_valid() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "object", "file.txt", "text/plain", "data".getBytes());
        List<ResourceDTO> result = List.of(fileDto("docs/", "file.txt", 4L));
        when(resourceService.upload(eq("docs/"), eq("alice"), anyList())).thenReturn(result);

        mvc.perform(multipart("/api/resource")
                        .file(file)
                        .param("path", "docs/")
                        .with(user("alice")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].name").value("file.txt"));
    }

    @Test
    void upload_invalidPath() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "object", "file.txt", "text/plain", "data".getBytes());

        mvc.perform(multipart("/api/resource")
                        .file(file)
                        .param("path", "docs")
                        .with(user("alice")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(resourceService);
    }

    @Test
    void upload_tooLarge_returnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "object", "archive.zip", "application/zip", "data".getBytes());
        doThrow(new IllegalArgumentException("File is too large: archive.zip. Maximum size is 2 GB."))
                .when(resourceService).upload(eq("docs/"), eq("alice"), anyList());

        mvc.perform(multipart("/api/resource")
                        .file(file)
                        .param("path", "docs/")
                        .with(user("alice")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("File is too large: archive.zip. Maximum size is 2 GB."));
    }

    @Test
    void getDirectoryContents_valid() throws Exception {
        List<ResourceDTO> result = List.of(dirDto("docs/", "sub/"));
        when(resourceService.getDirectoryContents("docs/", "alice")).thenReturn(result);

        mvc.perform(get("/api/directory")
                        .param("path", "docs/")
                        .with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("sub/"));
    }

    @Test
    void getDirectoryContents_rootPath_valid() throws Exception {
        List<ResourceDTO> result = List.of(dirDto("", "docs/"));
        when(resourceService.getDirectoryContents("", "alice")).thenReturn(result);

        mvc.perform(get("/api/directory")
                        .param("path", "")
                        .with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("docs/"));
    }

    @Test
    void createDirectory_valid() throws Exception {
        ResourceDTO dto = dirDto("docs/", "sub/");
        when(resourceService.createDirectory("docs/sub/", "alice")).thenReturn(dto);

        mvc.perform(post("/api/directory")
                        .param("path", "docs/sub/")
                        .with(user("alice")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("sub/"));
    }
}
