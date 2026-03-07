package com.example.file_storage.controller;

import com.example.file_storage.dto.ResourceDTO;
import com.example.file_storage.service.ResourceService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    private static final String PATH_ANY =
            "^(?!/)(?!.*//)(?!.*(^|/)\\.\\.?(?:/|$))(?:[^/]+/)*[^/]+/?$";

    private static final String PATH_DIR =
            "^$|(?!/)(?!.*//)(?!.*(^|/)\\.\\.?(?:/|$))(?:[^/]+/)*[^/]+/$";
    private static final String PATH_DIR_NON_EMPTY =
            "^(?!/)(?!.*//)(?!.*(^|/)\\.\\.?(?:/|$))(?:[^/]+/)*[^/]+/$";


    @GetMapping("/resource")
    public ResourceDTO getResource(@RequestParam("path")
                                   @NotBlank(message = "Path to file should be presented")
                                   @Pattern(regexp = PATH_ANY, message = "Path must be relative and valid")
                                   String path,
                                   @AuthenticationPrincipal UserDetails user) {

        return resourceService.getResourceInfo(path, user.getUsername());
    }

    @DeleteMapping("/resource")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@RequestParam("path")
                               @NotBlank(message = "Path to file should be presented")
                               @Pattern(regexp = PATH_ANY, message = "Path must be relative and valid")
                               String path,
                               @AuthenticationPrincipal UserDetails user) {

        resourceService.deleteResource(path, user.getUsername());

    }

    @GetMapping(value = "/resource/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> downloadResource(@RequestParam("path")
                                                                  @NotBlank(message = "Path to file should be presented")
                                                                  @Pattern(regexp = PATH_ANY, message = "Path must be relative and valid")
                                                                  String path,
                                                                  @AuthenticationPrincipal UserDetails user) {


        ResourceService.DownloadPayload payload = resourceService.download(path, user.getUsername());

        String cd = ContentDisposition.attachment()
                .filename(payload.filename(), StandardCharsets.UTF_8)
                .build().toString();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, cd)
                .body(payload.body());
    }

    @GetMapping("/resource/move")
    public ResourceDTO moveResource(@RequestParam("from")
                                    @NotBlank(message = "Path to file should be presented")
                                    @Pattern(regexp = PATH_ANY, message = "Path must be relative and valid")
                                    String from,
                                    @RequestParam("to")
                                    @NotBlank(message = "Path to file should be presented")
                                    @Pattern(regexp = PATH_ANY, message = "Path must be relative and valid")
                                    String to,
                                    @AuthenticationPrincipal UserDetails user) {


        return resourceService.moveResource(from, to, user.getUsername());
    }

    @GetMapping("/resource/search")
    public Page<ResourceDTO> find(@RequestParam("query")
                                  @NotBlank(message = "Query should be presented")
                                  @Size(max = 256, message = "Query is too long")
                                  String query,
                                  @AuthenticationPrincipal UserDetails user,
                                  @PageableDefault(size = 50) Pageable pageable) {

        return resourceService.find(query, user.getUsername(), pageable);

    }

    @PostMapping(value = "/resource", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<ResourceDTO> upload(@RequestParam("path")
                                    @Pattern(regexp = PATH_DIR, message = "Upload path must be a directory")
                                    String path,
                                    @RequestParam("object") List<MultipartFile> objects,
                                    @AuthenticationPrincipal UserDetails user) {

        return resourceService.upload(path, user.getUsername(), objects);

    }


    @GetMapping("/directory")
    public List<ResourceDTO> getDirectoryContents(@RequestParam("path")
                                                  @Pattern(regexp = PATH_DIR, message = "Directory path must end with '/'")
                                                  String path,
                                                  @AuthenticationPrincipal UserDetails user) {


        return resourceService.getDirectoryContents(path, user.getUsername());
    }

    @PostMapping("/directory")
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceDTO createDirectory(@RequestParam("path")
                                       @NotBlank(message = "Directory path must not be blank")
                                       @Pattern(regexp = PATH_DIR_NON_EMPTY, message = "Directory path must end with '/'")
                                       String path,
                                       @AuthenticationPrincipal UserDetails user) {

        return resourceService.createDirectory(path, user.getUsername());
    }


}
