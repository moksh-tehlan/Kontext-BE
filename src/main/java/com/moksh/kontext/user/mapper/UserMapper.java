package com.moksh.kontext.user.mapper;

import com.moksh.kontext.user.dto.CreateUserDto;
import com.moksh.kontext.user.dto.UpdateUserDto;
import com.moksh.kontext.user.dto.UserDto;
import com.moksh.kontext.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final ModelMapper modelMapper;

    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }
        return modelMapper.map(user, UserDto.class);
    }

    public User toEntity(CreateUserDto createDto) {
        if (createDto == null) {
            return null;
        }
        return modelMapper.map(createDto, User.class);
    }

    public void updateEntityFromDto(UpdateUserDto updateDto, User user) {
        if (updateDto == null || user == null) {
            return;
        }
        
        // Use ModelMapper's map method with skip null strategy
        modelMapper.getConfiguration().setSkipNullEnabled(true);
        modelMapper.map(updateDto, user);
        modelMapper.getConfiguration().setSkipNullEnabled(false);
    }
}