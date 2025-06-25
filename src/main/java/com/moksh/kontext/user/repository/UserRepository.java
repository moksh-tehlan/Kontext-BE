package com.moksh.kontext.user.repository;

import com.moksh.kontext.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {


    Optional<User> findByEmail(String email);



    boolean existsByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    boolean existsByGoogleId(String googleId);

    @Query("SELECT u FROM User u WHERE u.authProvider = :authProvider AND u.isActive = true")
    Page<User> findByAuthProvider(@Param("authProvider") User.AuthProvider authProvider, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true")
    Page<User> findByRole(@Param("role") User.UserRole role, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.isActive = true")
    Page<User> findAllActiveUsers(Pageable pageable);


    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.nickname) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "u.isActive = true")
    Page<User> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);
}