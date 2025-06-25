package com.moksh.kontext.common.util;

import com.moksh.kontext.common.exception.BusinessException;
import com.moksh.kontext.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Utility class to simplify access to Spring Security Context
 * 
 * Usage Examples:
 * 
 * // Get current user safely
 * User user = SecurityContextUtil.getCurrentUser();
 * if (user != null) { ... }
 * 
 * // Get current user with Optional
 * SecurityContextUtil.getCurrentUserOptional()
 *     .ifPresent(user -> log.info("User: {}", user.getEmail()));
 * 
 * // Get current user or throw exception
 * User user = SecurityContextUtil.getCurrentUserOrThrow();
 * 
 * // Get just the user ID
 * UUID userId = SecurityContextUtil.getCurrentUserId();
 * 
 * // Check permissions
 * if (SecurityContextUtil.isCurrentUserAdmin()) { ... }
 * if (SecurityContextUtil.canAccessResource(resourceOwnerId)) { ... }
 * 
 * // In controllers - replace @AuthenticationPrincipal
 * // OLD: public ApiResponse<Void> logout(@AuthenticationPrincipal User user)
 * // NEW: public ApiResponse<Void> logout()
 * authService.logout(SecurityContextUtil.getCurrentUserIdOrThrow());
 */
@Component
@Slf4j
public class SecurityContextUtil {

    /**
     * Get the currently authenticated user
     * @return User object if authenticated, null if not authenticated
     */
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        
        return null;
    }

    /**
     * Get the currently authenticated user
     * @return Optional containing User if authenticated, empty Optional if not
     */
    public static Optional<User> getCurrentUserOptional() {
        return Optional.ofNullable(getCurrentUser());
    }

    /**
     * Get the currently authenticated user or throw exception
     * @return User object
     * @throws BusinessException if no user is authenticated
     */
    public static User getCurrentUserOrThrow() {
        User user = getCurrentUser();
        if (user == null) {
            throw new BusinessException("No authenticated user found");
        }
        return user;
    }

    /**
     * Get the ID of the currently authenticated user
     * @return UUID of current user, null if not authenticated
     */
    public static UUID getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    /**
     * Get the ID of the currently authenticated user as Optional
     * @return Optional containing UUID if authenticated, empty Optional if not
     */
    public static Optional<UUID> getCurrentUserIdOptional() {
        return Optional.ofNullable(getCurrentUserId());
    }

    /**
     * Get the ID of the currently authenticated user or throw exception
     * @return UUID of current user
     * @throws BusinessException if no user is authenticated
     */
    public static UUID getCurrentUserIdOrThrow() {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("No authenticated user found");
        }
        return userId;
    }

    /**
     * Get the email of the currently authenticated user
     * @return Email string, null if not authenticated
     */
    public static String getCurrentUserEmail() {
        User user = getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    /**
     * Get the email of the currently authenticated user as Optional
     * @return Optional containing email if authenticated, empty Optional if not
     */
    public static Optional<String> getCurrentUserEmailOptional() {
        return Optional.ofNullable(getCurrentUserEmail());
    }

    /**
     * Get the role of the currently authenticated user
     * @return UserRole enum, null if not authenticated
     */
    public static User.UserRole getCurrentUserRole() {
        User user = getCurrentUser();
        return user != null ? user.getRole() : null;
    }

    /**
     * Check if current user has the specified role
     * @param role The role to check
     * @return true if user has the role, false otherwise
     */
    public static boolean hasRole(User.UserRole role) {
        User.UserRole currentRole = getCurrentUserRole();
        return currentRole != null && currentRole.equals(role);
    }

    /**
     * Check if current user is an admin
     * @return true if user is admin, false otherwise
     */
    public static boolean isCurrentUserAdmin() {
        return hasRole(User.UserRole.ADMIN);
    }

    /**
     * Check if current user is a regular user
     * @return true if user is regular user, false otherwise
     */
    public static boolean isCurrentUserRegularUser() {
        return hasRole(User.UserRole.USER);
    }

    /**
     * Check if there is an authenticated user
     * @return true if user is authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        return getCurrentUser() != null;
    }

    /**
     * Check if the current user ID matches the provided user ID
     * @param userId The user ID to check against
     * @return true if current user ID matches, false otherwise
     */
    public static boolean isCurrentUser(UUID userId) {
        UUID currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(userId);
    }

    /**
     * Check if current user can access the resource (admin or owner)
     * @param resourceOwnerId The ID of the resource owner
     * @return true if current user is admin or owns the resource
     */
    public static boolean canAccessResource(UUID resourceOwnerId) {
        return isCurrentUserAdmin() || isCurrentUser(resourceOwnerId);
    }

    /**
     * Get the full name of the currently authenticated user
     * @return Full name string, null if not authenticated
     */
    public static String getCurrentUserFullName() {
        User user = getCurrentUser();
        if (user == null) {
            return null;
        }
        return (user.getFirstName() != null ? user.getFirstName() : "") + 
               " " + 
               (user.getLastName() != null ? user.getLastName() : "");
    }

    /**
     * Clear the security context (useful for testing or manual logout)
     */
    public static void clearContext() {
        SecurityContextHolder.clearContext();
        log.debug("Security context cleared");
    }
}