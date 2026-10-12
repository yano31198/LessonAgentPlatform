package com.zone.lesoongen.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "app.security.enabled=true")
@ActiveProfiles("test")
class PlatformAuthApiTest {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper mapper;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void registerLoginLogoutAndLessonIsolation() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String alice = "alice_" + suffix;
        String bob = "bob_" + suffix;
        register(alice, "Alice");
        register(bob, "Bob");

        MockHttpSession aliceSession = login(alice);
        String lessonBody = mapper.writeValueAsString(Map.of(
                "title", "Alice 教案", "subject", "数学", "grade", "七年级",
                "topic", "一次函数", "durationMinutes", 45, "content", "教学内容"));
        MvcResult created = mvc.perform(post("/api/platform/lessons")
                        .session(aliceSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(lessonBody))
                .andExpect(status().isCreated())
                .andReturn();
        @SuppressWarnings("unchecked")
        Map<String, Object> createdBody = mapper.readValue(
                created.getResponse().getContentAsString(StandardCharsets.UTF_8), Map.class);
        @SuppressWarnings("unchecked")
        String lessonId = (String) ((Map<String, Object>) createdBody.get("lesson")).get("id");

        mvc.perform(get("/api/platform/lessons").session(aliceSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(lessonId));

        MockHttpSession bobSession = login(bob);
        mvc.perform(get("/api/platform/lessons").session(bobSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + lessonId + "')]").isEmpty());
        mvc.perform(get("/api/platform/lessons/" + lessonId).session(bobSession))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/auth/logout").session(aliceSession))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(aliceSession))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousPlatformRequestReturns401() throws Exception {
        mvc.perform(get("/api/platform/lessons"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    private void register(String username, String displayName) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "username", username, "password", "SecurePass123!",
                                "displayName", displayName))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username));
    }

    private MockHttpSession login(String username) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "username", username, "password", "SecurePass123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
