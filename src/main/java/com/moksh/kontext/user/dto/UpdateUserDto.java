package com.moksh.kontext.user.dto;

import com.moksh.kontext.user.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserDto {

    @Size(max = 100, message = "Nickname must not exceed 100 characters")
    private String nickname;

    @Email(message = "Email should be valid")
    private String email;

    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    private String profilePictureUrl;

    private User.UserRole role;
}