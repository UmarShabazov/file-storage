package com.example.file_storage.service;

import com.example.file_storage.dto.ResourceDTO;
import com.example.file_storage.entity.ResourceEntity;
import com.example.file_storage.entity.ResourceType;
import com.example.file_storage.entity.UserEntity;
import com.example.file_storage.exception.ResourceAlreadyExistsException;
import com.example.file_storage.exception.ResourceNotFoundException;
import com.example.file_storage.exception.StorageException;
import com.example.file_storage.repository.ResourceRepository;
import com.example.file_storage.repository.UserRepository;
import com.example.file_storage.repository.storage.StorageRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.data.domain.Pageable;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.*;

@Service
@Transactional
public class ResourceService {

    private static final long MAX_FILE_SIZE_BYTES = 2L * 1024 * 1024 * 1024;
    private static final long MAX_TOTAL_UPLOAD_SIZE_BYTES = 2L * 1024 * 1024 * 1024;
    private static final int MAX_FILES_PER_REQUEST = 200;

    public record DownloadPayload(StreamingResponseBody body, String filename) {

    }

    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final StorageRepository storageRepository;
    private final PathService pathService;

    public ResourceService(ResourceRepository resourceRepository,
                           UserRepository userRepository,
                           StorageRepository storageRepository,
                           PathService pathService) {
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.storageRepository = storageRepository;
        this.pathService = pathService;
    }

    public ResourceDTO getResourceInfo(String path, String username) {
        path = normalizeLookupPath(path);

        UserEntity owner = requireUser(username);

        ResourceEntity entity = getResourceEntity(path, owner);

        return entityToDTOConverter(entity);

    }

    public void deleteResource(String path, String username) {
        path = normalizeLookupPath(path);

        UserEntity owner = requireUser(username);

        ResourceEntity entity = getResourceEntity(path, owner);

        if (entity.getType() == ResourceType.DIRECTORY) {

            List<ResourceEntity> descendants = resourceRepository.findAllByOwnerAndFullPathStartingWith(owner, path);

            storageRepository.deleteFiles(owner.getId(),
                    descendants.stream().map(ResourceEntity::getFullPath).toList());
            resourceRepository.deleteAll(descendants);

        } else {
            storageRepository.deleteFile(owner.getId(), path);
            resourceRepository.deleteByOwnerAndFullPath(owner, path);
        }

    }

    public DownloadPayload download(String path, String username) {
        path = normalizeLookupPath(path);
        final String normalizedPath = path;

        UserEntity owner = requireUser(username);

        ResourceEntity entity = getResourceEntity(normalizedPath, owner);

        if (entity.getType() == ResourceType.FILE) {
            StreamingResponseBody body = out -> {
                try (InputStream in = storageRepository.downloadFile(owner.getId(), normalizedPath)) {
                    in.transferTo(out);
                }
            };

            return new DownloadPayload(body, entity.getName());
        } else {
            StreamingResponseBody body = out -> zipFolder(owner, normalizedPath, out);
            return new DownloadPayload(body, entity.getName() + ".zip");
        }

    }

    public ResourceDTO moveResource(String from, String to, String username) {
        from = normalizeLookupPath(from);

        UserEntity owner = requireUser(username);

        ResourceEntity entity = getResourceEntity(from, owner);
        validateMoveTargetType(to, entity.getType());
        to = normalizeMoveTargetPath(to, entity.getType());

        if (from.equals(to)) {
            return entityToDTOConverter(entity);
        }

        checkDirectorySelfInjection(from, to, entity);

        ensureNoConflictOnMove(owner, to);

        if (entity.getType() == ResourceType.FILE) {
            moveFile(from, to, owner, entity);
        } else {
            moveDirectory(from, to, owner);
        }
        return entityToDTOConverter(entity);
    }

    private void validateMoveTargetType(String path, ResourceType type) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null");
        }
        boolean endsWithSlash = path.endsWith("/");
        if (type == ResourceType.DIRECTORY && !endsWithSlash) {
            throw new IllegalArgumentException("Directory target path must end with '/'");
        }
        if (type == ResourceType.FILE && endsWithSlash) {
            throw new IllegalArgumentException("File target path must not end with '/'");
        }
    }

    private String normalizeMoveTargetPath(String path, ResourceType type) {
        return pathService.normalizePath(path, type);
    }

    private void ensureNoConflictOnMove(UserEntity owner, String to) {
        boolean conflictOnMove = resourceRepository.findByOwnerAndFullPath(owner, to).isPresent();
        if (conflictOnMove) {
            throw new ResourceAlreadyExistsException(
                    "Resource already exists: " + to + ".");
        }
    }


    private static void checkDirectorySelfInjection(String from, String to, ResourceEntity entity) {
        if (entity.getType() == ResourceType.DIRECTORY) {
            String fromNorm = from.endsWith("/") ? from : from + "/";
            String toNorm = to.endsWith("/") ? to : to + "/";
            if (toNorm.startsWith(fromNorm)) {
                throw new IllegalArgumentException("Cannot move directory into itself: " + from + " -> " + to);
            }
        }
    }

    private void moveDirectory(String from, String to, UserEntity owner) {
        List<ResourceEntity> descendants = resourceRepository.findAllByOwnerAndFullPathStartingWith(owner, from);

        for (ResourceEntity descendant : descendants) {
            String oldPath = descendant.getFullPath();
            String newPath = to + oldPath.substring(from.length());

            if (descendant.getType() == ResourceType.FILE) {
                storageRepository.copyFile(owner.getId(), oldPath, newPath);
                storageRepository.deleteFile(owner.getId(), oldPath);
            }

            applyNewPath(descendant, newPath);
        }

        resourceRepository.saveAll(descendants);
    }

    private void applyNewPath(ResourceEntity entity, String newPath) {
        newPath = entity.getType() == ResourceType.DIRECTORY
                ? pathService.normalizePath(newPath, ResourceType.DIRECTORY)
                : pathService.normalizePath(newPath, ResourceType.FILE);
        entity.setFullPath(newPath);
        entity.setName(pathService.extractName(newPath));
        entity.setParentPath(pathService.extractParentPath(newPath));
    }

    private void moveFile(String from, String to, UserEntity owner, ResourceEntity entity) {
        storageRepository.copyFile(owner.getId(), from, to);
        storageRepository.deleteFile(owner.getId(), from);

        applyNewPath(entity, to);

        resourceRepository.save(entity);
    }

    public List<ResourceDTO> find(String query, String username, Pageable pageable) {

        UserEntity owner = requireUser(username);

        return resourceRepository
                .findAllByOwnerAndNameContainingIgnoreCase(owner, query, pageable)
                .stream()
                .map(this::entityToDTOConverter)
                .toList();
    }

    public List<ResourceDTO> upload(String path, String username, List<MultipartFile> files) {
        path = pathService.normalizePath(path, ResourceType.DIRECTORY);

        UserEntity owner = requireUser(username);

        ResourceEntity target = getResourceEntity(path, owner);

        if (target.getType() != ResourceType.DIRECTORY) {
            throw new IllegalArgumentException("Upload path must point to a directory");
        }

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("There should be files for upload");
        }

        validateUploadLimits(files);

        List<ResourceDTO> uploaded = new ArrayList<>();

        for (MultipartFile file : files) {


            String relative = file.getOriginalFilename();

            if (relative == null || relative.isBlank()) {
                throw new IllegalArgumentException("File name must be present");
            }
            relative = pathService.normalizeUploadRelativePath(relative);

            ensureDirectoryExists(path, relative, owner);

            String finalPath = path + relative;

            if (resourceRepository.existsByOwnerAndFullPath(owner, finalPath)) {
                throw new ResourceAlreadyExistsException("Resource already exists: " + finalPath);
            }

            try {
                storageRepository.putFile(owner.getId(), finalPath, file.getInputStream(), file.getSize());
            } catch (Exception e) {
                throw new StorageException(e.getMessage());
            }
            ResourceEntity entity = buildEntity(
                    owner,
                    finalPath,
                    ResourceType.FILE,
                    file.getSize()
            );

            resourceRepository.save(entity);
            uploaded.add(entityToDTOConverter(entity));
        }


        return uploaded.stream()
                .sorted(Comparator
                        .comparing(ResourceDTO::type)
                        .thenComparing(ResourceDTO::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void validateUploadLimits(List<MultipartFile> files) {
        if (files.size() > MAX_FILES_PER_REQUEST) {
            throw new IllegalArgumentException(
                    "Too many files in one upload request. Maximum is " + MAX_FILES_PER_REQUEST + ".");
        }

        long totalSize = 0;
        for (MultipartFile file : files) {
            long fileSize = file.getSize();
            if (fileSize > MAX_FILE_SIZE_BYTES) {
                throw new IllegalArgumentException(
                        "File is too large: " + safeFileName(file) + ". Maximum size is 2 GB.");
            }

            totalSize += fileSize;
            if (totalSize > MAX_TOTAL_UPLOAD_SIZE_BYTES) {
                throw new IllegalArgumentException("Upload size exceeds 2 GB.");
            }
        }
    }

    private String safeFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        return originalFilename == null || originalFilename.isBlank() ? "<unknown>" : originalFilename;
    }

    private void ensureDirectoryExists(String basePath, String relative, UserEntity owner) {

        String relativeParentPath = pathService.extractParentPath(relative);

        if (relativeParentPath.isEmpty()) {
            return;
        } else {

            String[] segments = relativeParentPath.split("/");
            String currentPath = basePath;

            for (String segment : segments) {

                if (segment.isBlank()) {
                    continue;
                }

                currentPath += segment + "/";

                if (!resourceRepository.existsByOwnerAndFullPath(owner, currentPath)) {

                    createDirectory(currentPath, owner.getUserName());
                }

            }
        }
    }

    public List<ResourceDTO> getDirectoryContents(String path, String username) {
        path = path.isEmpty() ? "" : pathService.normalizePath(path, ResourceType.DIRECTORY);

        UserEntity owner = requireUser(username);

        List<ResourceEntity> directoryContent = resourceRepository.findAllByOwnerAndParentPath(owner, path);

        if (path.isEmpty()) {
            directoryContent = directoryContent.stream()
                    .filter(e -> !e.getFullPath().isEmpty())
                    .toList();
        }

        return directoryContent.stream()
                .map(this::entityToDTOConverter)
                .sorted(Comparator
                        .comparing(ResourceDTO::type)
                        .thenComparing(ResourceDTO::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

    }

    public ResourceDTO createDirectory(String path, String username) {
        path = pathService.normalizePath(path, ResourceType.DIRECTORY);

        UserEntity owner = requireUser(username);
        String parentPath = pathService.extractParentPath(path);

        ResourceEntity parent = getResourceEntity(parentPath, owner);

        if (parent.getType() != ResourceType.DIRECTORY) {
            throw new IllegalArgumentException("Parent is not a directory: " + parentPath);
        }

        ensureNoConflictOnMove(owner, path);

        ResourceEntity entity = buildEntity(owner, path, ResourceType.DIRECTORY, null);
        resourceRepository.save(entity);

        return entityToDTOConverter(entity);


    }

    private ResourceDTO entityToDTOConverter(ResourceEntity entity) {

        String name = entity.getName();
        if (entity.getType() == ResourceType.DIRECTORY && !name.endsWith("/")) {
            name = name + "/";
        }

        return new ResourceDTO(entity.getParentPath(), name, entity.getSize(), entity.getType());
    }

    private ResourceEntity getResourceEntity(String path, UserEntity owner) {
        if (path.isEmpty()) {
            return getOrCreateRoot(owner);
        }

        return resourceRepository.findByOwnerAndFullPath(owner, path)
                .orElseThrow(() -> new ResourceNotFoundException("There is no resource with this path: " + path + "."));
    }

    private ResourceEntity getOrCreateRoot(UserEntity owner) {
        return resourceRepository.findByOwnerAndFullPath(owner, "")
                .orElseGet(() -> {
                    ResourceEntity root = new ResourceEntity();
                    root.setOwner(owner);
                    root.setFullPath("");
                    root.setParentPath("");
                    root.setName("");
                    root.setType(ResourceType.DIRECTORY);
                    root.setSize(null);
                    return resourceRepository.save(root);
                });
    }

    private UserEntity requireUser(String username) {
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private void zipFolder(UserEntity owner, String path, OutputStream out) throws IOException {

        List<ResourceEntity> items = resourceRepository.findAllByOwnerAndFullPathStartingWith(owner, path);

        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (ResourceEntity item : items) {
                String relative = item.getFullPath().substring(path.length());
                if (relative.startsWith("/")) relative = relative.substring(1);

                if (item.getType() == ResourceType.DIRECTORY) {
                    if (!relative.endsWith("/")) relative += "/";
                    zip.putNextEntry(new ZipEntry(relative));
                    zip.closeEntry();
                } else {
                    try (InputStream in = storageRepository.downloadFile(owner.getId(), item.getFullPath())) {
                        zip.putNextEntry(new ZipEntry(relative));
                        in.transferTo(zip);
                        zip.closeEntry();
                    }
                }
            }
        }


    }


    private ResourceEntity buildEntity(UserEntity owner, String fullPath,
                                       ResourceType type, Long size) {
        fullPath = type == ResourceType.DIRECTORY
                ? pathService.normalizePath(fullPath, ResourceType.DIRECTORY)
                : pathService.normalizePath(fullPath, ResourceType.FILE);
        ResourceEntity e = new ResourceEntity();
        e.setOwner(owner);
        e.setFullPath(fullPath);
        e.setParentPath(pathService.extractParentPath(fullPath));
        e.setName(pathService.extractName(fullPath));
        e.setType(type);
        e.setSize(size);
        return e;
    }

    private String normalizeLookupPath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null");
        }
        if (path.isEmpty()) {
            return "";
        }
        return path.endsWith("/")
                ? pathService.normalizePath(path, ResourceType.DIRECTORY)
                : pathService.normalizePath(path, ResourceType.FILE);
    }

}
