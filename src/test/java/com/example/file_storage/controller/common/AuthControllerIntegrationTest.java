package com.example.file_storage.controller.common;

import com.example.file_storage.config.TestcontainersConfiguration;
import com.example.file_storage.config.TestStorageConfig;
import com.example.file_storage.dto.UserCreateUpdateDTO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.http.Cookie;

import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestStorageConfig.class, TestcontainersConfiguration.class})
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();
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
        Cookie sessionCookie = loginResult.getResponse().getCookie("SESSION");
        Assertions.assertNotNull(sessionCookie);

        mvc.perform(post("/api/auth/sign-out").cookie(sessionCookie))
                .andExpect(status().isNoContent());
    }
}
