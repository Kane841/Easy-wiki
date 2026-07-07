package com.easywiki.service;

import com.easywiki.dto.request.RegisterRequest;
import com.easywiki.enums.JoinRequestStatus;
import com.easywiki.enums.MemberRole;
import com.easywiki.repository.GroupJoinRequestRepository;
import com.easywiki.repository.GroupMemberRepository;
import com.easywiki.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GroupServiceTest {

    @Autowired GroupService groupService;
    @Autowired GroupMemberRepository memberRepository;
    @Autowired GroupJoinRequestRepository joinRequestRepository;
    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;

    @BeforeEach
    void setup() {
        authService.register(new RegisterRequest("admin1", "a@test.com", "pass12345"));
    }

    @Test
    void createGroupAddsAdminMember() {
        Long userId = userRepository.findByUsername("admin1").orElseThrow().getId();
        var group = groupService.createGroup(userId, "研发团队", "desc", true);
        var member = memberRepository.findByGroupIdAndUserId(group.getId(), userId).orElseThrow();
        assertThat(member.getRole()).isEqualTo(MemberRole.ADMIN);
    }

    @Test
    void joinByInviteAddsMember() {
        Long adminId = userRepository.findByUsername("admin1").orElseThrow().getId();
        authService.register(new RegisterRequest("member1", "m@test.com", "pass12345"));
        Long memberId = userRepository.findByUsername("member1").orElseThrow().getId();

        var group = groupService.createGroup(adminId, "研发团队", "desc", true);
        var invite = groupService.createInvite(group.getId(), adminId);

        groupService.joinByInvite(invite.getToken(), memberId);

        assertThat(groupService.isMember(group.getId(), memberId)).isTrue();
    }

    @Test
    void approveJoinRequestAddsMember() {
        Long adminId = userRepository.findByUsername("admin1").orElseThrow().getId();
        authService.register(new RegisterRequest("applicant", "app@test.com", "pass12345"));
        Long applicantId = userRepository.findByUsername("applicant").orElseThrow().getId();

        var group = groupService.createGroup(adminId, "研发团队", "desc", true);
        var request = groupService.applyToJoin(group.getId(), applicantId, "想加入");

        groupService.approveJoinRequest(request.getId(), adminId);

        assertThat(groupService.isMember(group.getId(), applicantId)).isTrue();
        var updated = joinRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JoinRequestStatus.APPROVED);
    }
}
