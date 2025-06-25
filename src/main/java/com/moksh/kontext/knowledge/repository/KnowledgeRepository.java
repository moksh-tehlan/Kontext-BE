package com.moksh.kontext.knowledge.repository;

import com.moksh.kontext.knowledge.entity.Knowledge;
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
public interface KnowledgeRepository extends JpaRepository<Knowledge, UUID> {

    List<Knowledge> findByProjectIdAndIsActiveTrue(UUID projectId);
    
    Page<Knowledge> findByProjectIdAndIsActiveTrue(UUID projectId, Pageable pageable);
    
    List<Knowledge> findByProjectIdAndTypeAndIsActiveTrue(UUID projectId, Knowledge.KnowledgeType type);
    
    Page<Knowledge> findByProjectIdAndTypeAndIsActiveTrue(UUID projectId, Knowledge.KnowledgeType type, Pageable pageable);
    
    Optional<Knowledge> findByIdAndProjectIdAndIsActiveTrue(UUID knowledgeId, UUID projectId);
    
    @Query("SELECT k FROM Knowledge k WHERE k.project.id = :projectId AND k.isActive = true AND " +
           "(LOWER(k.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(k.source) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Knowledge> findByProjectIdAndSearchTerm(@Param("projectId") UUID projectId, 
                                                @Param("searchTerm") String searchTerm, 
                                                Pageable pageable);
    
    @Query("SELECT k FROM Knowledge k WHERE k.project.user.id = :userId AND k.project.id = :projectId AND k.isActive = true")
    List<Knowledge> findByUserIdAndProjectId(@Param("userId") UUID userId, @Param("projectId") UUID projectId);
}