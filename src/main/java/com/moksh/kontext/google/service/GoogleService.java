package com.moksh.kontext.google.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.moksh.kontext.common.exception.BusinessException;
import com.moksh.kontext.google.config.GoogleConfig;
import com.moksh.kontext.google.dto.GoogleUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleService {

    private final GoogleConfig googleConfig;

    public GoogleUserInfo verifyGoogleToken(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), 
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleConfig.getClientId()))
                    .build();

            GoogleIdToken token = verifier.verify(idToken);
            
            if (token != null) {
                GoogleIdToken.Payload payload = token.getPayload();
                
                String email = payload.getEmail();
                boolean emailVerified = payload.getEmailVerified();
                String firstName = (String) payload.get("given_name");
                String lastName = (String) payload.get("family_name");
                String fullName = (String) payload.get("name");
                String pictureUrl = (String) payload.get("picture");
                String googleId = payload.getSubject();

                log.info("Successfully verified Google token for user: {}", email);

                return GoogleUserInfo.builder()
                        .email(email)
                        .firstName(firstName != null ? firstName : "")
                        .lastName(lastName != null ? lastName : "")
                        .fullName(fullName != null ? fullName : "")
                        .pictureUrl(pictureUrl)
                        .emailVerified(emailVerified)
                        .googleId(googleId)
                        .build();
            } else {
                log.error("Invalid Google ID token");
                throw new BusinessException("Invalid Google ID token");
            }
        } catch (GeneralSecurityException | IOException e) {
            log.error("Error verifying Google ID token", e);
            throw new BusinessException("Failed to verify Google ID token: " + e.getMessage());
        }
    }
}