package com.easywiki.controller;

import com.easywiki.dto.common.ApiResponse;
import com.easywiki.dto.request.ApplyJoinRequest;
import com.easywiki.dto.request.CreateGroupRequest;
import com.easywiki.dto.response.GroupInviteResponse;
import com.easywiki.dto.response.GroupResponse;
import com.easywiki.dto.response.JoinRequestResponse;
import com.easywiki.security.UserPrincipal;
import com.easywiki.service.GroupMembershipService;
import com.easywiki.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;
    private final GroupMembershipService membershipService;

    public GroupController(GroupService groupService, GroupMembershipService membershipService) {
        this.groupService = groupService;
        this.membershipService = membershipService;
    }

    @PostMapping
    public ApiResponse<GroupResponse> createGroup(@Valid @RequestBody CreateGroupRequest req) {
        Long userId = currentUserId();
        var group = groupService.createGroup(userId, req.getName(), req.getDescription(), req.isDiscoverable());
        return ApiResponse.ok(GroupResponse.from(group));
    }

    @GetMapping
    public ApiResponse<List<GroupResponse>> listMyGroups() {
        Long userId = currentUserId();
        List<GroupResponse> groups = groupService.listMyGroups(userId).stream()
                .map(GroupResponse::from)
                .toList();
        return ApiResponse.ok(groups);
    }

    @GetMapping("/{id}")
    public ApiResponse<GroupResponse> getGroup(@PathVariable Long id) {
        Long userId = currentUserId();
        membershipService.requireMember(id, userId);
        return ApiResponse.ok(GroupResponse.from(groupService.getGroup(id)));
    }

    @PostMapping("/{id}/invites")
    public ApiResponse<GroupInviteResponse> createInvite(@PathVariable Long id) {
        Long userId = currentUserId();
        var invite = groupService.createInvite(id, userId);
        return ApiResponse.ok(GroupInviteResponse.from(invite));
    }

    @PostMapping("/join/{token}")
    public ApiResponse<GroupResponse> joinByInvite(@PathVariable String token) {
        Long userId = currentUserId();
        var member = groupService.joinByInvite(token, userId);
        return ApiResponse.ok(GroupResponse.from(groupService.getGroup(member.getGroupId())));
    }

    @PostMapping("/{id}/join-requests")
    public ApiResponse<JoinRequestResponse> applyToJoin(@PathVariable Long id,
                                                        @Valid @RequestBody ApplyJoinRequest req) {
        Long userId = currentUserId();
        var request = groupService.applyToJoin(id, userId, req.getReason());
        return ApiResponse.ok(JoinRequestResponse.from(request));
    }

    @GetMapping("/{id}/join-requests")
    public ApiResponse<List<JoinRequestResponse>> listPendingJoinRequests(@PathVariable Long id) {
        Long userId = currentUserId();
        List<JoinRequestResponse> requests = groupService.listPendingJoinRequests(id, userId).stream()
                .map(JoinRequestResponse::from)
                .toList();
        return ApiResponse.ok(requests);
    }

    @PutMapping("/join-requests/{requestId}/approve")
    public ApiResponse<JoinRequestResponse> approveJoinRequest(@PathVariable Long requestId) {
        Long userId = currentUserId();
        var request = groupService.approveJoinRequest(requestId, userId);
        return ApiResponse.ok(JoinRequestResponse.from(request));
    }

    @PutMapping("/join-requests/{requestId}/reject")
    public ApiResponse<JoinRequestResponse> rejectJoinRequest(@PathVariable Long requestId) {
        Long userId = currentUserId();
        var request = groupService.rejectJoinRequest(requestId, userId);
        return ApiResponse.ok(JoinRequestResponse.from(request));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ApiResponse<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        groupService.removeMember(id, userId, currentUserId());
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/leave")
    public ApiResponse<Void> leaveGroup(@PathVariable Long id) {
        groupService.leaveGroup(id, currentUserId());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> dissolveGroup(@PathVariable Long id) {
        groupService.dissolveGroup(id, currentUserId());
        return ApiResponse.ok(null);
    }

    private Long currentUserId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return principal.userId();
    }
}
