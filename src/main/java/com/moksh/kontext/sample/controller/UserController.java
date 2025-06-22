package com.moksh.kontext.sample.controller;

import com.moksh.kontext.common.response.ApiResponse;
import com.moksh.kontext.common.response.PageResponse;
import com.moksh.kontext.sample.dto.UserDto;
import com.moksh.kontext.sample.service.UserService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @Timed(value = "user.create", description = "Time taken to create user")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody UserDto userDto) {
        log.info("Creating user with username: {}", userDto.getUsername());
        
        UserDto createdUser = userService.createUser(userDto);
        ApiResponse<UserDto> response = ApiResponse.success(createdUser, "User created successfully");
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Timed(value = "user.get", description = "Time taken to get user by ID")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        log.info("Fetching user by ID: {}", id);
        
        UserDto user = userService.getUserById(id);
        ApiResponse<UserDto> response = ApiResponse.success(user);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/username/{username}")
    @Timed(value = "user.get.username", description = "Time taken to get user by username")
    public ResponseEntity<ApiResponse<UserDto>> getUserByUsername(@PathVariable String username) {
        log.info("Fetching user by username: {}", username);
        
        UserDto user = userService.getUserByUsername(username);
        ApiResponse<UserDto> response = ApiResponse.success(user);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Timed(value = "user.list", description = "Time taken to list users")
    public ResponseEntity<ApiResponse<PageResponse<UserDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search) {
        
        log.info("Fetching users - page: {}, size: {}, sortBy: {}, sortDir: {}, search: {}", 
                page, size, sortBy, sortDir, search);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<UserDto> users = userService.getAllUsers(pageable, search);
        PageResponse<UserDto> pageResponse = PageResponse.of(users);
        ApiResponse<PageResponse<UserDto>> response = ApiResponse.success(pageResponse);
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Timed(value = "user.update", description = "Time taken to update user")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable Long id, 
            @Valid @RequestBody UserDto userDto) {
        
        log.info("Updating user with ID: {}", id);
        
        UserDto updatedUser = userService.updateUser(id, userDto);
        ApiResponse<UserDto> response = ApiResponse.success(updatedUser, "User updated successfully");
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Timed(value = "user.delete", description = "Time taken to delete user")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        log.info("Deleting user with ID: {}", id);
        
        userService.deleteUser(id);
        ApiResponse<Void> response = ApiResponse.success(null, "User deleted successfully");
        
        return ResponseEntity.ok(response);
    }
}