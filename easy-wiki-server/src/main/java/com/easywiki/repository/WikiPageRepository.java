package com.easywiki.repository;

import com.easywiki.entity.WikiPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WikiPageRepository extends JpaRepository<WikiPage, Long> {

    List<WikiPage> findByGroupIdOrderBySortOrderAsc(Long groupId);

    long countByParentId(Long parentId);

    Optional<WikiPage> findByIdAndGroupId(Long id, Long groupId);

    @Query("SELECT p FROM WikiPage p WHERE p.groupId = :groupId AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<WikiPage> searchByGroupIdAndKeyword(@Param("groupId") Long groupId, @Param("keyword") String keyword);
}
