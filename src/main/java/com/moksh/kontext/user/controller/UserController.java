package com.moksh.kontext.user.controller;

import com.moksh.kontext.common.response.ApiResponse;
import com.moksh.kontext.common.response.PageResponse;
import com.moksh.kontext.user.dto.CreateUserDto;
import com.moksh.kontext.user.dto.UpdateUserDto;
import com.moksh.kontext.user.dto.UserDto;
import com.moksh.kontext.user.entity.User;
import com.moksh.kontext.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<UserDto>> getAllUsers(@PageableDefault(size = 20) Pageable pageable) {
        log.debug("GET /api/users - Fetching all users");
        
        Page<UserDto> users = userService.getAllUsers(pageable);
        PageResponse<UserDto> pageResponse = PageResponse.of(users);
        
        return ApiResponse.success(pageResponse, "Users retrieved successfully");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ApiResponse<UserDto> getUserById(@PathVariable Long id) {
        log.debug("GET /api/users/{} - Fetching user by id", id);
        
        UserDto user = userService.getUserById(id);
        return ApiResponse.success(user, "User retrieved successfully");
    }

    @PostMapping
    public ApiResponse<UserDto> createUser(@Valid @RequestBody CreateUserDto createUserDto) {
        log.debug("POST /api/users - Creating new user");
        
        UserDto createdUser = userService.createUser(createUserDto);
        return ApiResponse.success(createdUser, "User created successfully", 201);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ApiResponse<UserDto> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserDto updateUserDto) {
        log.debug("PUT /api/users/{} - Updating user", id);
        
        UserDto updatedUser = userService.updateUser(id, updateUserDto);
        return ApiResponse.success(updatedUser, "User updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        log.debug("DELETE /api/users/{} - Deleting user", id);
        
        userService.deleteUser(id);
        return ApiResponse.success(null, "User deleted successfully", 204);
    }

    @GetMapping("/email/{email}")
    public ApiResponse<UserDto> getUserByEmail(@PathVariable String email) {
        log.debug("GET /api/users/email/{} - Fetching user by email", email);
        
        UserDto user = userService.getUserByEmail(email);
        return ApiResponse.success(user, "User retrieved successfully");
    }

    @GetMapping("/google/{googleId}")
    public ApiResponse<UserDto> getUserByGoogleId(@PathVariable String googleId) {
        log.debug("GET /api/users/google/{} - Fetching user by Google ID", googleId);
        
        UserDto user = userService.getUserByGoogleId(googleId);
        return ApiResponse.success(user, "User retrieved successfully");
    }

    @PatchMapping("/{id}/verify-email")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ApiResponse<UserDto> verifyUserEmail(@PathVariable Long id) {
        log.debug("PATCH /api/users/{}/verify-email - Verifying user email", id);
        
        UserDto updatedUser = userService.verifyUserEmail(id);
        return ApiResponse.success(updatedUser, "Email verified successfully");
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserDto> updateUserRole(@PathVariable Long id, @RequestParam User.UserRole role) {
        log.debug("PATCH /api/users/{}/role - Updating user role to {}", id, role);
        
        UserDto updatedUser = userService.updateUserRole(id, role);
        return ApiResponse.success(updatedUser, "User role updated successfully");
    }
}