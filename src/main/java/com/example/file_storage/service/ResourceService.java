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
import org.springframework.data.domain.Page;
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

        UserEntity owner = requireUser(username);

        ResourceEntity entity = getResourceEntity(path, owner);

        return entityToDTOConverter(entity);

    }

    public void deleteResource(String path, String username) {

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

        UserEntity owner = requireUser(username);

        ResourceEntity entity = getResourceEntity(path, owner);

        if (entity.getType() == ResourceType.FILE) {
            StreamingResponseBody body = out -> {
                try (InputStream in = storageRepository.downloadFile(owner.getId(), path)) {
                    in.transferTo(out);
                }
            };

            return new DownloadPayload(body, entity.getName());
        } else {
            StreamingResponseBody body = out -> zipFolder(owner, path, out);
            return new DownloadPayload(body, entity.getName() + ".zip");
        }

    }

    public ResourceDTO moveResource(String from, String to, String username) {

        UserEntity owner = requireUser(username);

        ResourceEntity entity = getResourceEntity(from, owner);

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

    private void ensureNoConflictOnMove(UserEntity owner, String to) {
        boolean conflictOnMove = resourceRepository.findByOwnerAndFullPath(owner, to).isPresent();
        if (conflictOnMove) {
            throw new ResourceAlreadyExistsException(
                    "Resource with the same name already exists at path" + to + ".");
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

    public Page<ResourceDTO> find(String query, String username, Pageable pageable) {

        UserEntity owner = requireUser(username);

        Page<ResourceEntity> resourceEntities = resourceRepository
                .findAllByOwnerAndNameContainingIgnoreCase(owner, query, pageable);

        return resourceEntities.map(this::entityToDTOConverter);
    }

    public List<ResourceDTO> upload(String path, String username, List<MultipartFile> files) {

        UserEntity owner = requireUser(username);

        ResourceEntity target = getResourceEntity(path, owner);

        if (target.getType() != ResourceType.DIRECTORY) {
            throw new IllegalArgumentException("Upload path must point to a directory");
        }

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("There should be files for upload");
        }

        List<ResourceDTO> uploaded = new ArrayList<>();

        for (MultipartFile file : files) {


            String relative = file.getOriginalFilename();

            if (relative == null || relative.isBlank()) {
                throw new IllegalArgumentException("File name must be present");
            }

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

        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path must not be blank");
        }

        if (path.equals("/")) {
            throw new IllegalArgumentException("Root directory already exists");
        }

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
        ResourceEntity e = new ResourceEntity();
        e.setOwner(owner);
        e.setFullPath(fullPath);
        e.setParentPath(pathService.extractParentPath(fullPath));
        e.setName(pathService.extractName(fullPath));
        e.setType(type);
        e.setSize(size);
        return e;
    }

}
