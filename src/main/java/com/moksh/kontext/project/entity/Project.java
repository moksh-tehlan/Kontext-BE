package com.moksh.kontext.project.entity;

import com.moksh.kontext.common.entity.BaseEntity;
import com.moksh.kontext.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Project extends BaseEntity {

    @NotBlank(message = "Project name is required")
    @Size(max = 100, message = "Project name must not exceed 100 characters")
    @Column(name = "name", nullable = false)
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}