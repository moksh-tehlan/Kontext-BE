package com.moksh.kontext.sample.service;

import com.moksh.kontext.common.exception.BusinessException;
import com.moksh.kontext.common.exception.ResourceNotFoundException;
import com.moksh.kontext.sample.dto.UserDto;
import com.moksh.kontext.sample.entity.User;
import com.moksh.kontext.sample.mapper.UserMapper;
import com.moksh.kontext.sample.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserDto createUser(UserDto userDto) {
        log.debug("Creating user with username: {}", userDto.getUsername());
        
        validateUserForCreation(userDto);
        
        User user = userMapper.toEntity(userDto);
        user.setStatus(User.UserStatus.ACTIVE);
        
        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());
        
        return userMapper.toDto(savedUser);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        log.debug("Fetching user by ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id.toString()));
        
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public UserDto getUserByUsername(String username) {
        log.debug("Fetching user by username: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable, String search) {
        log.debug("Fetching users with search: {}, page: {}", search, pageable.getPageNumber());
        
        Page<User> users;
        if (StringUtils.hasText(search)) {
            users = userRepository.findActiveUsersBySearch(search, pageable);
        } else {
            users = userRepository.findAllActiveUsers(pageable);
        }
        
        return users.map(userMapper::toDto);
    }

    @Transactional
    public UserDto updateUser(Long id, UserDto userDto) {
        log.debug("Updating user with ID: {}", id);
        
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id.toString()));
        
        validateUserForUpdate(userDto, existingUser);
        
        userMapper.updateEntityFromDto(userDto, existingUser);
        User updatedUser = userRepository.save(existingUser);
        
        log.info("User updated successfully with ID: {}", updatedUser.getId());
        return userMapper.toDto(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        log.debug("Deleting user with ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id.toString()));
        
        user.setIsActive(false);
        userRepository.save(user);
        
        log.info("User soft deleted successfully with ID: {}", id);
    }

    private void validateUserForCreation(UserDto userDto) {
        if (userRepository.existsByUsername(userDto.getUsername())) {
            throw new BusinessException("Username already exists", HttpStatus.CONFLICT);
        }
        
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new BusinessException("Email already exists", HttpStatus.CONFLICT);
        }
    }

    private void validateUserForUpdate(UserDto userDto, User existingUser) {
        if (!existingUser.getUsername().equals(userDto.getUsername()) &&
            userRepository.existsByUsername(userDto.getUsername())) {
            throw new BusinessException("Username already exists", HttpStatus.CONFLICT);
        }
        
        if (!existingUser.getEmail().equals(userDto.getEmail()) &&
            userRepository.existsByEmail(userDto.getEmail())) {
            throw new BusinessException("Email already exists", HttpStatus.CONFLICT);
        }
    }
}