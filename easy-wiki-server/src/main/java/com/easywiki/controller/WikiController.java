package com.easywiki.controller;

import com.easywiki.dto.common.ApiResponse;
import com.easywiki.dto.request.CreateWikiPageRequest;
import com.easywiki.dto.request.UpdateWikiPageRequest;
import com.easywiki.dto.response.UploadResponse;
import com.easywiki.dto.response.WikiPageResponse;
import com.easywiki.dto.response.WikiTreeNodeResponse;
import com.easywiki.security.UserPrincipal;
import com.easywiki.service.FileService;
import com.easywiki.service.FileService;
import com.easywiki.service.GroupMembershipService;
import com.easywiki.service.WikiService;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/wiki")
public class WikiController {

    private final WikiService wikiService;
    private final FileService fileService;
    private final GroupMembershipService membershipService;

    public WikiController(WikiService wikiService, FileService fileService,
                          GroupMembershipService membershipService) {
        this.wikiService = wikiService;
        this.fileService = fileService;
        this.membershipService = membershipService;
    }

    @GetMapping("/tree")
    public ApiResponse<List<WikiTreeNodeResponse>> getTree(@PathVariable Long groupId) {
        Long userId = currentUserId();
        return ApiResponse.ok(wikiService.getTree(groupId, userId));
    }

    @PostMapping("/pages")
    public ApiResponse<WikiPageResponse> createPage(@PathVariable Long groupId,
                                                    @Valid @RequestBody CreateWikiPageRequest req) {
        Long userId = currentUserId();
        var page = wikiService.createPage(groupId, userId, req.getParentId(), req.getTitle(), req.getContent());
        return ApiResponse.ok(WikiPageResponse.from(page));
    }

    @GetMapping("/pages/{pageId}")
    public ApiResponse<WikiPageResponse> getPage(@PathVariable Long groupId, @PathVariable Long pageId) {
        Long userId = currentUserId();
        return ApiResponse.ok(WikiPageResponse.from(wikiService.getPage(groupId, userId, pageId)));
    }

    @PutMapping("/pages/{pageId}")
    public ApiResponse<WikiPageResponse> updatePage(@PathVariable Long groupId,
                                                    @PathVariable Long pageId,
                                                    @Valid @RequestBody UpdateWikiPageRequest req) {
        Long userId = currentUserId();
        var page = wikiService.updatePage(groupId, userId, pageId, req.getTitle(), req.getContent(), req.getVersion());
        return ApiResponse.ok(WikiPageResponse.from(page));
    }

    @DeleteMapping("/pages/{pageId}")
    public ApiResponse<Void> deletePage(@PathVariable Long groupId, @PathVariable Long pageId) {
        Long userId = currentUserId();
        wikiService.deletePage(groupId, userId, pageId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/search")
    public ApiResponse<List<WikiPageResponse>> search(@PathVariable Long groupId,
                                                    @RequestParam String keyword) {
        Long userId = currentUserId();
        List<WikiPageResponse> results = wikiService.search(groupId, userId, keyword).stream()
                .map(WikiPageResponse::from)
                .toList();
        return ApiResponse.ok(results);
    }

    @PostMapping("/upload")
    public ApiResponse<UploadResponse> upload(@PathVariable Long groupId,
                                              @RequestParam("file") MultipartFile file) {
        Long userId = currentUserId();
        membershipService.requireMember(groupId, userId);
        String url = fileService.saveImage(groupId, file);
        return ApiResponse.ok(new UploadResponse(url));
    }

    private Long currentUserId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return principal.userId();
    }
}
