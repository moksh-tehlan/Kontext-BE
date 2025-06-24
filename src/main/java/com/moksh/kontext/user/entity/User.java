package com.moksh.kontext.user.entity;

import com.moksh.kontext.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Size(max = 100, message = "Nickname must not exceed 100 characters")
    @Column(name = "nickname")
    private String nickname;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @NotBlank(message = "Authentication provider is required")
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(name = "is_email_verified")
    private Boolean isEmailVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role = UserRole.USER;

    public enum AuthProvider {
        EMAIL_OTP("EMAIL_OTP"),
        GOOGLE("GOOGLE");

        private final String value;

        AuthProvider(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum UserRole {
        ADMIN, USER
    }
}