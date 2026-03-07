package com.example.file_storage.controller.common;

import com.example.file_storage.config.TestStorageConfig;
import com.example.file_storage.dto.UserCreateUpdateDTO;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import com.fasterxml.jackson.databind.ObjectMapper;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStorageConfig.class)
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void loadProperties(@NotNull DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

    }

    private UserCreateUpdateDTO newUser(String baseName) {
        String suffix = java.lang.Long.toString(System.nanoTime(), 36);
        String username = baseName + "_" + suffix;
        if (username.length() > 20) {
            username = username.substring(0, 20);
        }
        return new UserCreateUpdateDTO(username, "myPassword");
    }


    @Test
    void checkRegistration() throws Exception {
        UserCreateUpdateDTO dto = newUser("Alex");
        mvc.perform(post("/api/auth/sign-up")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

    }

    @Test
    void checkDuplicateRegistration() throws Exception {
        UserCreateUpdateDTO dto1 = newUser("Samira");
        mvc.perform(post("/api/auth/sign-up")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/sign-up")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isConflict());

    }

    @Test
    void checkRegistrationThenLogin() throws Exception {
        UserCreateUpdateDTO dto2 = newUser("John");
        mvc.perform(post("/api/auth/sign-up")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/sign-in")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isOk())
                .andReturn();

    }

    @Test
    void checkBadCredentials() throws Exception {
        UserCreateUpdateDTO dto3 = newUser("Ryan");
        UserCreateUpdateDTO dto3FakePassword = new UserCreateUpdateDTO(dto3.userName(), "Password");
        mvc.perform(post("/api/auth/sign-up")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto3)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/sign-in")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto3FakePassword)))
                .andExpect(status().isUnauthorized())
                .andReturn();

    }

    @Test
    void checkRegistrationThenLoginThenLogout() throws Exception {
        UserCreateUpdateDTO dto4 = newUser("Oleg");
        mvc.perform(post("/api/auth/sign-up")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto4)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mvc.perform(post("/api/auth/sign-in")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto4)))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mvc.perform(post("/api/auth/sign-out").session(session))
                .andExpect(status().is3xxRedirection());
    }
}

