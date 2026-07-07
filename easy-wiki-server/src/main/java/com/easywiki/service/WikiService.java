package com.easywiki.service;

import com.easywiki.dto.event.NotificationEvent;
import com.easywiki.dto.response.WikiTreeNodeResponse;
import com.easywiki.entity.GroupMember;
import com.easywiki.entity.WikiPage;
import com.easywiki.enums.NotificationEventType;
import com.easywiki.exception.BusinessException;
import com.easywiki.exception.ConflictException;
import com.easywiki.repository.GroupMemberRepository;
import com.easywiki.repository.UserRepository;
import com.easywiki.repository.WikiPageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WikiService {

    private final WikiPageRepository wikiPageRepository;
    private final GroupMembershipService membershipService;
    private final GroupMemberRepository memberRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public WikiService(WikiPageRepository wikiPageRepository,
                       GroupMembershipService membershipService,
                       GroupMemberRepository memberRepository,
                       NotificationService notificationService,
                       UserRepository userRepository) {
        this.wikiPageRepository = wikiPageRepository;
        this.membershipService = membershipService;
        this.memberRepository = memberRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    public List<WikiTreeNodeResponse> getTree(Long groupId, Long userId) {
        membershipService.requireMember(groupId, userId);
        List<WikiPage> pages = wikiPageRepository.findByGroupIdOrderBySortOrderAsc(groupId);
        return buildTree(pages);
    }

    @Transactional
    public WikiPage createPage(Long groupId, Long userId, Long parentId, String title, String content) {
        membershipService.requireMember(groupId, userId);
        if (parentId != null) {
            wikiPageRepository.findByIdAndGroupId(parentId, groupId)
                    .orElseThrow(() -> new BusinessException(404, "父页面不存在"));
        }

        int sortOrder = nextSortOrder(groupId, parentId);

        WikiPage page = new WikiPage();
        page.setGroupId(groupId);
        page.setParentId(parentId);
        page.setTitle(title);
        page.setContent(content != null ? content : "");
        page.setSortOrder(sortOrder);
        page.setCreatedBy(userId);
        return wikiPageRepository.save(page);
    }

    public WikiPage getPage(Long groupId, Long userId, Long pageId) {
        membershipService.requireMember(groupId, userId);
        return findPageInGroup(groupId, pageId);
    }

    @Transactional
    public WikiPage updatePage(Long groupId, Long userId, Long pageId, String title, String content, Integer expectedVersion) {
        membershipService.requireMember(groupId, userId);
        WikiPage page = findPageInGroup(groupId, pageId);
        if (!page.getVersion().equals(expectedVersion)) {
            throw new ConflictException("页面版本冲突，请刷新后重试");
        }
        page.setTitle(title);
        page.setContent(content != null ? content : "");
        page = wikiPageRepository.save(page);
        notifyWikiUpdated(groupId, userId, page);
        return page;
    }

    @Transactional
    public void deletePage(Long groupId, Long userId, Long pageId) {
        membershipService.requireMember(groupId, userId);
        findPageInGroup(groupId, pageId);
        if (wikiPageRepository.countByParentId(pageId) > 0) {
            throw new BusinessException(400, "存在子页面，无法删除");
        }
        wikiPageRepository.deleteById(pageId);
    }

    public List<WikiPage> search(Long groupId, Long userId, String keyword) {
        membershipService.requireMember(groupId, userId);
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return wikiPageRepository.searchByGroupIdAndKeyword(groupId, keyword.trim());
    }

    private WikiPage findPageInGroup(Long groupId, Long pageId) {
        return wikiPageRepository.findByIdAndGroupId(pageId, groupId)
                .orElseThrow(() -> new BusinessException(404, "页面不存在"));
    }

    private int nextSortOrder(Long groupId, Long parentId) {
        return wikiPageRepository.findByGroupIdOrderBySortOrderAsc(groupId).stream()
                .filter(p -> {
                    if (parentId == null) {
                        return p.getParentId() == null;
                    }
                    return parentId.equals(p.getParentId());
                })
                .mapToInt(WikiPage::getSortOrder)
                .max()
                .orElse(-1) + 1;
    }

    private List<WikiTreeNodeResponse> buildTree(List<WikiPage> pages) {
        Map<Long, WikiTreeNodeResponse> nodeMap = new HashMap<>();
        List<WikiTreeNodeResponse> roots = new ArrayList<>();

        for (WikiPage page : pages) {
            nodeMap.put(page.getId(), WikiTreeNodeResponse.from(page));
        }

        for (WikiPage page : pages) {
            WikiTreeNodeResponse node = nodeMap.get(page.getId());
            if (page.getParentId() == null) {
                roots.add(node);
            } else {
                WikiTreeNodeResponse parent = nodeMap.get(page.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }

        sortTree(roots);
        return roots;
    }

    private void sortTree(List<WikiTreeNodeResponse> nodes) {
        nodes.sort(Comparator.comparingInt(WikiTreeNodeResponse::getSortOrder));
        for (WikiTreeNodeResponse node : nodes) {
            sortTree(node.getChildren());
        }
    }

    private void notifyWikiUpdated(Long groupId, Long editorId, WikiPage page) {
        String editorName = userRepository.findById(editorId)
                .map(u -> u.getUsername())
                .orElse("成员");
        for (GroupMember member : memberRepository.findByGroupId(groupId)) {
            if (member.getUserId().equals(editorId)) {
                continue;
            }
            notificationService.publish(new NotificationEvent(
                    member.getUserId(),
                    groupId,
                    NotificationEventType.WIKI_UPDATED,
                    "Wiki 更新",
                    editorName + " 更新了文档「" + page.getTitle() + "」",
                    null,
                    "/groups/" + groupId + "/wiki/pages/" + page.getId()
            ));
        }
    }
}
