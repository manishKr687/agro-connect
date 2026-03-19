package com.agroconnect.controller;

// ...existing code...
// ...existing code...
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    // ...existing code...

    @Test
    void testRegisterUser() throws Exception {
        String uniquePhone = "99999" + System.currentTimeMillis();
        String userJson = "{" +
            "\"name\":\"Test User\"," +
            "\"phone\":\"" + uniquePhone + "\"," +
            "\"passwordHash\":\"password\"," +
            "\"languagePreference\":\"en\"}";
        mockMvc.perform(post("/api/register/farmer")
            .contentType(MediaType.APPLICATION_JSON)
            .content(userJson))
            .andExpect(status().isOk());
    }
}
