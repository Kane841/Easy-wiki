package com.easywiki.controller;

import com.easywiki.dto.request.LoginRequest;
import com.easywiki.dto.request.RegisterRequest;
import com.easywiki.dto.response.AuthResponse;
import com.easywiki.service.AuthService;
import com.easywiki.service.GroupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GroupControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AuthService authService;
    @Autowired GroupService groupService;

    private String adminToken;
    private String outsiderToken;
    private Long groupId;

    @BeforeEach
    void setup() {
        authService.register(new RegisterRequest("groupadmin", "ga@test.com", "pass12345"));
        authService.register(new RegisterRequest("outsider", "out@test.com", "pass12345"));

        LoginRequest adminLogin = new LoginRequest();
        adminLogin.setUsername("groupadmin");
        adminLogin.setPassword("pass12345");
        LoginRequest outsiderLogin = new LoginRequest();
        outsiderLogin.setUsername("outsider");
        outsiderLogin.setPassword("pass12345");

        AuthResponse adminAuth = authService.login(adminLogin);
        AuthResponse outsiderAuth = authService.login(outsiderLogin);
        adminToken = adminAuth.getToken();
        outsiderToken = outsiderAuth.getToken();

        var group = groupService.createGroup(adminAuth.getUserId(), "测试小组", "desc", true);
        groupId = group.getId();
    }

    @Test
    void nonMemberAccessingGroupDetailReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/groups/" + groupId)
                .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}
