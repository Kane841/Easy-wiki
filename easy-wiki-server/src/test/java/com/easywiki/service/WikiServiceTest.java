package com.easywiki.service;

import com.easywiki.dto.request.RegisterRequest;
import com.easywiki.entity.WikiPage;
import com.easywiki.exception.BusinessException;
import com.easywiki.exception.ConflictException;
import com.easywiki.repository.UserRepository;
import com.easywiki.repository.WikiPageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WikiServiceTest {

    @Autowired WikiService wikiService;
    @Autowired GroupService groupService;
    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;
    @Autowired WikiPageRepository wikiPageRepository;

    Long groupId;
    Long userId;

    @BeforeEach
    void setup() {
        authService.register(new RegisterRequest("wikiuser", "wiki@test.com", "pass12345"));
        userId = userRepository.findByUsername("wikiuser").orElseThrow().getId();
        groupId = groupService.createGroup(userId, "Wiki组", "desc", true).getId();
    }

    @Test
    void updateWithStaleVersionThrowsConflict() {
        WikiPage page = wikiService.createPage(groupId, userId, null, "标题", "# content");
        Integer originalVersion = page.getVersion();
        wikiService.updatePage(groupId, userId, page.getId(), "新标题", "# new", originalVersion);
        assertThatThrownBy(() -> wikiService.updatePage(groupId, userId, page.getId(), "x", "y", originalVersion))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void getTreeReturnsNestedStructure() {
        WikiPage root = wikiService.createPage(groupId, userId, null, "根", "# root");
        wikiService.createPage(groupId, userId, root.getId(), "子页", "# child");

        var tree = wikiService.getTree(groupId, userId);
        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getTitle()).isEqualTo("根");
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getTitle()).isEqualTo("子页");
    }

    @Test
    void deletePageWithChildrenThrows() {
        WikiPage parent = wikiService.createPage(groupId, userId, null, "父", "# p");
        wikiService.createPage(groupId, userId, parent.getId(), "子", "# c");

        assertThatThrownBy(() -> wikiService.deletePage(groupId, userId, parent.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("子页面");
    }

    @Test
    void searchFindsByTitleAndContent() {
        wikiService.createPage(groupId, userId, null, "Spring Boot", "# 入门指南");
        wikiService.createPage(groupId, userId, null, "其他", "# unrelated");

        var results = wikiService.search(groupId, userId, "Spring");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Spring Boot");
    }
}
