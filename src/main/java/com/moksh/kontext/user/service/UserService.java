package com.moksh.kontext.user.service;

import com.moksh.kontext.common.exception.BusinessException;
import com.moksh.kontext.common.exception.ResourceNotFoundException;
import com.moksh.kontext.user.dto.CreateUserDto;
import com.moksh.kontext.user.dto.UpdateUserDto;
import com.moksh.kontext.user.dto.UserDto;
import com.moksh.kontext.user.entity.User;
import com.moksh.kontext.user.mapper.UserMapper;
import com.moksh.kontext.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable) {
        log.debug("Fetching all active users with pagination: {}", pageable);
        return userRepository.findAllActiveUsers(pageable)
                .map(userMapper::toDto);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(UUID id) {
        log.debug("Fetching user by id: {}", id);
        User user = userRepository.findById(id)
                .filter(User::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toDto(user);
    }


    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        User user = userRepository.findByEmail(email)
                .filter(User::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return userMapper.toDto(user);
    }

    public UserDto createUser(CreateUserDto createUserDto) {
        log.debug("Creating new user with email: {}", createUserDto.getEmail());
        
        validateUserCreation(createUserDto);
        
        User user = userMapper.toEntity(createUserDto);
        
        User savedUser = userRepository.save(user);
        log.info("User created successfully with id: {}", savedUser.getId());
        
        return userMapper.toDto(savedUser);
    }

    public UserDto updateUser(UUID id, UpdateUserDto updateUserDto) {
        log.debug("Updating user with id: {}", id);
        
        User existingUser = userRepository.findById(id)
                .filter(User::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        validateUserUpdate(updateUserDto, existingUser);
        
        userMapper.updateEntityFromDto(updateUserDto, existingUser);
        User updatedUser = userRepository.save(existingUser);
        
        log.info("User updated successfully with id: {}", updatedUser.getId());
        return userMapper.toDto(updatedUser);
    }

    public void deleteUser(UUID id) {
        log.debug("Soft deleting user with id: {}", id);
        
        User user = userRepository.findById(id)
                .filter(User::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        user.setIsActive(false);
        userRepository.save(user);
        
        log.info("User soft deleted successfully with id: {}", id);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> searchUsers(String searchTerm, Pageable pageable) {
        log.debug("Searching users with term: {}", searchTerm);
        return userRepository.findBySearchTerm(searchTerm, pageable)
                .map(userMapper::toDto);
    }




    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean existsByGoogleId(String googleId) {
        return userRepository.existsByGoogleId(googleId);
    }

    @Transactional(readOnly = true)
    public UserDto getUserByGoogleId(String googleId) {
        log.debug("Fetching user by Google ID: {}", googleId);
        User user = userRepository.findByGoogleId(googleId)
                .filter(User::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with Google ID: " + googleId));
        return userMapper.toDto(user);
    }

    public UserDto verifyUserEmail(UUID userId) {
        log.debug("Verifying email for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .filter(User::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        user.setIsEmailVerified(true);
        User updatedUser = userRepository.save(user);
        
        log.info("Email verified successfully for user ID: {}", userId);
        return userMapper.toDto(updatedUser);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getUsersByAuthProvider(User.AuthProvider authProvider, Pageable pageable) {
        log.debug("Fetching users by auth provider: {}", authProvider);
        return userRepository.findByAuthProvider(authProvider, pageable)
                .map(userMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getUsersByRole(User.UserRole role, Pageable pageable) {
        log.debug("Fetching users by role: {}", role);
        return userRepository.findByRole(role, pageable)
                .map(userMapper::toDto);
    }

    public UserDto updateUserRole(UUID id, User.UserRole role) {
        log.debug("Updating user role for id: {} to role: {}", id, role);
        
        User user = userRepository.findById(id)
                .filter(User::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        user.setRole(role);
        User updatedUser = userRepository.save(user);
        
        log.info("User role updated successfully for id: {}", id);
        return userMapper.toDto(updatedUser);
    }

    private void validateUserCreation(CreateUserDto createUserDto) {
        if (userRepository.existsByEmail(createUserDto.getEmail())) {
            throw new BusinessException("Email already exists: " + createUserDto.getEmail());
        }
        
        // Validate Google ID if provided for Google auth
        if (User.AuthProvider.GOOGLE.equals(createUserDto.getAuthProvider()) && createUserDto.getGoogleId() != null) {
            if (userRepository.existsByGoogleId(createUserDto.getGoogleId())) {
                throw new BusinessException("Google account already exists: " + createUserDto.getGoogleId());
            }
        }
        
        // Validate that Google ID is provided for Google auth
        if (User.AuthProvider.GOOGLE.equals(createUserDto.getAuthProvider()) && createUserDto.getGoogleId() == null) {
            throw new BusinessException("Google ID is required for Google authentication");
        }
    }

    private void validateUserUpdate(UpdateUserDto updateUserDto, User existingUser) {
        if (updateUserDto.getEmail() != null && 
            !updateUserDto.getEmail().equals(existingUser.getEmail()) &&
            userRepository.existsByEmail(updateUserDto.getEmail())) {
            throw new BusinessException("Email already exists: " + updateUserDto.getEmail());
        }
    }
}