package com.easywiki.service;

import com.easywiki.entity.Group;
import com.easywiki.entity.GroupInvite;
import com.easywiki.entity.GroupJoinRequest;
import com.easywiki.entity.GroupMember;
import com.easywiki.enums.JoinRequestStatus;
import com.easywiki.enums.MemberRole;
import com.easywiki.exception.BusinessException;
import com.easywiki.exception.ConflictException;
import com.easywiki.repository.GroupInviteRepository;
import com.easywiki.repository.GroupJoinRequestRepository;
import com.easywiki.repository.GroupMemberRepository;
import com.easywiki.repository.GroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GroupService {

    private static final int INVITE_VALID_DAYS = 7;

    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final GroupJoinRequestRepository joinRequestRepository;
    private final GroupInviteRepository inviteRepository;

    public GroupService(GroupRepository groupRepository,
                        GroupMemberRepository memberRepository,
                        GroupJoinRequestRepository joinRequestRepository,
                        GroupInviteRepository inviteRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.inviteRepository = inviteRepository;
    }

    @Transactional
    public Group createGroup(Long userId, String name, String description, boolean discoverable) {
        Group group = new Group();
        group.setName(name);
        group.setDescription(description);
        group.setDiscoverable(discoverable);
        group.setCreatedBy(userId);
        group = groupRepository.save(group);

        GroupMember admin = new GroupMember();
        admin.setGroupId(group.getId());
        admin.setUserId(userId);
        admin.setRole(MemberRole.ADMIN);
        memberRepository.save(admin);

        return group;
    }

    public List<Group> listMyGroups(Long userId) {
        List<Long> groupIds = memberRepository.findByUserId(userId).stream()
                .map(GroupMember::getGroupId)
                .toList();
        if (groupIds.isEmpty()) {
            return List.of();
        }
        return groupRepository.findAllById(groupIds);
    }

    public Group getGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(404, "小组不存在"));
    }

    @Transactional
    public GroupInvite createInvite(Long groupId, Long adminUserId) {
        requireAdmin(groupId, adminUserId);

        GroupInvite invite = new GroupInvite();
        invite.setGroupId(groupId);
        invite.setCreatedBy(adminUserId);
        invite.setExpiresAt(LocalDateTime.now().plusDays(INVITE_VALID_DAYS));
        return inviteRepository.save(invite);
    }

    @Transactional
    public GroupMember joinByInvite(String token, Long userId) {
        GroupInvite invite = inviteRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(404, "邀请链接无效"));
        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(400, "邀请链接已过期");
        }
        if (isMember(invite.getGroupId(), userId)) {
            throw new ConflictException("已是小组成员");
        }

        GroupMember member = new GroupMember();
        member.setGroupId(invite.getGroupId());
        member.setUserId(userId);
        member.setRole(MemberRole.MEMBER);
        return memberRepository.save(member);
    }

    @Transactional
    public GroupJoinRequest applyToJoin(Long groupId, Long userId, String reason) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(404, "小组不存在"));
        if (isMember(groupId, userId)) {
            throw new ConflictException("已是小组成员");
        }
        if (joinRequestRepository.findByGroupIdAndUserIdAndStatus(groupId, userId, JoinRequestStatus.PENDING).isPresent()) {
            throw new ConflictException("已有待审批的入组申请");
        }

        GroupJoinRequest request = new GroupJoinRequest();
        request.setGroupId(groupId);
        request.setUserId(userId);
        request.setReason(reason);
        request.setStatus(JoinRequestStatus.PENDING);
        return joinRequestRepository.save(request);
    }

    @Transactional
    public GroupJoinRequest approveJoinRequest(Long requestId, Long adminUserId) {
        GroupJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(404, "入组申请不存在"));
        requireAdmin(request.getGroupId(), adminUserId);
        if (request.getStatus() != JoinRequestStatus.PENDING) {
            throw new BusinessException(400, "申请已处理");
        }
        if (isMember(request.getGroupId(), request.getUserId())) {
            request.setStatus(JoinRequestStatus.APPROVED);
            joinRequestRepository.save(request);
            throw new ConflictException("用户已是小组成员");
        }

        GroupMember member = new GroupMember();
        member.setGroupId(request.getGroupId());
        member.setUserId(request.getUserId());
        member.setRole(MemberRole.MEMBER);
        memberRepository.save(member);

        request.setStatus(JoinRequestStatus.APPROVED);
        return joinRequestRepository.save(request);
    }

    @Transactional
    public GroupJoinRequest rejectJoinRequest(Long requestId, Long adminUserId) {
        GroupJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(404, "入组申请不存在"));
        requireAdmin(request.getGroupId(), adminUserId);
        if (request.getStatus() != JoinRequestStatus.PENDING) {
            throw new BusinessException(400, "申请已处理");
        }
        request.setStatus(JoinRequestStatus.REJECTED);
        return joinRequestRepository.save(request);
    }

    public List<GroupJoinRequest> listPendingJoinRequests(Long groupId, Long adminUserId) {
        requireAdmin(groupId, adminUserId);
        return joinRequestRepository.findByGroupIdAndStatus(groupId, JoinRequestStatus.PENDING);
    }

    public boolean isMember(Long groupId, Long userId) {
        return memberRepository.findByGroupIdAndUserId(groupId, userId).isPresent();
    }

    @Transactional
    public void removeMember(Long groupId, Long targetUserId, Long adminUserId) {
        requireAdmin(groupId, adminUserId);
        if (adminUserId.equals(targetUserId)) {
            throw new BusinessException(400, "不能移除自己，请使用退出小组");
        }
        GroupMember member = memberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new BusinessException(404, "成员不存在"));
        memberRepository.delete(member);
    }

    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        GroupMember member = memberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new BusinessException(403, "不是小组成员"));
        memberRepository.delete(member);
    }

    @Transactional
    public void dissolveGroup(Long groupId, Long adminUserId) {
        requireAdmin(groupId, adminUserId);
        memberRepository.deleteByGroupId(groupId);
        joinRequestRepository.deleteByGroupId(groupId);
        inviteRepository.deleteByGroupId(groupId);
        groupRepository.deleteById(groupId);
    }

    private void requireAdmin(Long groupId, Long userId) {
        GroupMember member = memberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new BusinessException(403, "不是小组成员"));
        if (member.getRole() != MemberRole.ADMIN) {
            throw new BusinessException(403, "需要管理员权限");
        }
    }
}
