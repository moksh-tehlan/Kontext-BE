package com.moksh.kontext.user.dto;

import com.moksh.kontext.common.dto.BaseDto;
import com.moksh.kontext.user.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto extends BaseDto {

    @Size(max = 100, message = "Nickname must not exceed 100 characters")
    private String nickname;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    private String profilePictureUrl;

    private User.AuthProvider authProvider;

    private String googleId;

    private Boolean isEmailVerified;

    private User.UserRole role;
}