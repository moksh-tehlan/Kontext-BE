package com.moksh.kontext.chat.repository;

import com.moksh.kontext.chat.entity.Chat;
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
public interface ChatRepository extends JpaRepository<Chat, UUID> {

    List<Chat> findByProjectIdAndIsActiveTrue(UUID projectId);
    
    Page<Chat> findByProjectIdAndIsActiveTrue(UUID projectId, Pageable pageable);
    
    Optional<Chat> findByIdAndProjectIdAndIsActiveTrue(UUID chatId, UUID projectId);
    
    @Query("SELECT c FROM Chat c WHERE c.project.id = :projectId AND c.isActive = true AND " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Chat> findByProjectIdAndSearchTerm(@Param("projectId") UUID projectId, 
                                          @Param("searchTerm") String searchTerm, 
                                          Pageable pageable);
}