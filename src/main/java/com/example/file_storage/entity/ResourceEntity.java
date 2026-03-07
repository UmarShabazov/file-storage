package com.example.file_storage.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "resources")
public class ResourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY )
    @Column (name = "id")
    private Long id;

    @NotNull
    @Column (name = "name")
    private String name;

    @Column (name = "size")
    private Long size;

    @NotNull
    @Column (name = "type")
    @Enumerated (EnumType.STRING)
    private ResourceType type;

    @NotNull
    @ManyToOne
    @JoinColumn (name = "owner_id", nullable = false)
    private UserEntity owner;

    @NotNull
    @Column(name = "full_path", nullable = false, length = 1024)
    private String fullPath;

    @NotNull
    @Column(name = "parent_path", nullable = false, length = 1024)
    private String parentPath;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public UserEntity getOwner() {
        return owner;
    }

    public void setOwner(UserEntity owner) {
        this.owner = owner;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    public ResourceEntity(){}

    public ResourceEntity(Long id, String name, Long size, ResourceType type, UserEntity owner, String fullPath, String parentPath) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.type = type;
        this.owner = owner;
        this.fullPath = fullPath;
        this.parentPath = parentPath;
    }
}
