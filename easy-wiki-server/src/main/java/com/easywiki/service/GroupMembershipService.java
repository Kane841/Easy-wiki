package com.easywiki.service;

import com.easywiki.exception.BusinessException;
import com.easywiki.repository.GroupMemberRepository;
import org.springframework.stereotype.Service;

@Service
public class GroupMembershipService {

    private final GroupMemberRepository memberRepository;

    public GroupMembershipService(GroupMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public void requireMember(Long groupId, Long userId) {
        if (!memberRepository.findByGroupIdAndUserId(groupId, userId).isPresent()) {
            throw new BusinessException(403, "不是小组成员");
        }
    }
}
