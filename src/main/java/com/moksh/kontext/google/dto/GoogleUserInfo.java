package com.moksh.kontext.google.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleUserInfo {
    
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String pictureUrl;
    private boolean emailVerified;
    private String googleId;
}