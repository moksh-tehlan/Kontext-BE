package com.moksh.kontext.project.repository;

import com.moksh.kontext.project.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByUserIdAndIsActiveTrue(UUID userId);
    
    Page<Project> findByUserIdAndIsActiveTrue(UUID userId, Pageable pageable);
    
    Optional<Project> findByIdAndUserIdAndIsActiveTrue(UUID projectId, UUID userId);
    
    @Query("SELECT p FROM Project p WHERE p.user.id = :userId AND p.isActive = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Project> findByUserIdAndSearchTerm(@Param("userId") UUID userId, 
                                          @Param("searchTerm") String searchTerm, 
                                          Pageable pageable);
}