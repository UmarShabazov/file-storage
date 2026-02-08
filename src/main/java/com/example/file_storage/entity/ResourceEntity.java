package com.example.file_storage.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import javax.annotation.Nullable;

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
    @JoinColumn (name = "user_id")
    private UserEntity owner;

    @ManyToOne
    @JoinColumn (name = "parent_id")
    private ResourceEntity parent;


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

    public ResourceEntity getParent() {
        return parent;
    }

    public void setParent(ResourceEntity parent) {
        this.parent = parent;
    }

    public ResourceEntity(){}

    public ResourceEntity( String name, Long size, @NotNull ResourceType type, UserEntity user, ResourceEntity parentId) {
        this.name = name;
        this.size = size;
        this.type = type;
        this.owner = user;
        this.parent = parentId;
    }
}
