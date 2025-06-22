package com.moksh.kontext.sample.mapper;

import com.moksh.kontext.sample.dto.UserDto;
import com.moksh.kontext.sample.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    
    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }
        
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setCreatedBy(user.getCreatedBy());
        dto.setUpdatedBy(user.getUpdatedBy());
        dto.setVersion(user.getVersion());
        dto.setIsActive(user.getIsActive());
        
        return dto;
    }
    
    public User toEntity(UserDto dto) {
        if (dto == null) {
            return null;
        }
        
        User user = new User();
        user.setId(dto.getId());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFullName());
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : User.UserStatus.ACTIVE);
        user.setVersion(dto.getVersion());
        user.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        
        return user;
    }
    
    public void updateEntityFromDto(UserDto dto, User entity) {
        if (dto == null || entity == null) {
            return;
        }
        
        entity.setUsername(dto.getUsername());
        entity.setEmail(dto.getEmail());
        entity.setFullName(dto.getFullName());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }
}