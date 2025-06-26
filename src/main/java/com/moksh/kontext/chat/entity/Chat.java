package com.moksh.kontext.chat.entity;

import com.moksh.kontext.common.entity.BaseEntity;
import com.moksh.kontext.project.entity.Project;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Chat extends BaseEntity {

    @NotBlank(message = "Chat name is required")
    @Size(max = 100, message = "Chat name must not exceed 100 characters")
    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
}