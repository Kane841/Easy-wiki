package com.easywiki.repository;

import com.easywiki.entity.GroupJoinRequest;
import com.easywiki.enums.JoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, Long> {

    List<GroupJoinRequest> findByGroupIdAndStatus(Long groupId, JoinRequestStatus status);

    Optional<GroupJoinRequest> findByGroupIdAndUserIdAndStatus(Long groupId, Long userId, JoinRequestStatus status);

    void deleteByGroupId(Long groupId);
}
