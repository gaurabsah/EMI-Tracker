package com.emitracker.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.emitracker.user.Role;
import com.emitracker.user.User;
import com.emitracker.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;

    @Test
    void seededAdminCanLoginAndGetsJwtAndRole() throws Exception {
        mvc.perform(login("admin@emitracker.local", "admin123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void wrongPasswordOrUnknownEmailIs401() throws Exception {
        mvc.perform(login("admin@emitracker.local", "nope")).andExpect(status().isUnauthorized());
        mvc.perform(login("ghost@emitracker.local", "admin123")).andExpect(status().isUnauthorized());
    }

    @Test
    void blankLoginFieldsAre400() throws Exception {
        mvc.perform(login("", "")).andExpect(status().isBadRequest());
    }

    @Test
    void noTokenIs401() throws Exception {
        mvc.perform(get("/api/admin/ping")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/plans/ping")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me/ping")).andExpect(status().isUnauthorized());
    }

    @Test
    void garbageTokenIs401() throws Exception {
        mvc.perform(get("/api/admin/ping").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithAnotherKeyIs401() throws Exception {
        String forged = io.jsonwebtoken.Jwts.builder().subject("1").claim("role", "ADMIN")
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        "some-other-secret-some-other-secret-some-other".getBytes()))
                .compact();
        mvc.perform(get("/api/admin/ping").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongRoleIs403AndRightRoleIs200() throws Exception {
        String admin = token(Role.ADMIN, null);
        String company = token(Role.COMPANY, 1L);
        String customer = token(Role.CUSTOMER, 1L);

        mvc.perform(get("/api/admin/ping").header("Authorization", admin)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/ping").header("Authorization", company)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/ping").header("Authorization", customer)).andExpect(status().isForbidden());

        mvc.perform(get("/api/plans/ping").header("Authorization", company)).andExpect(status().isOk());
        mvc.perform(get("/api/plans/ping").header("Authorization", admin)).andExpect(status().isForbidden());
        mvc.perform(get("/api/plans/ping").header("Authorization", customer)).andExpect(status().isForbidden());

        mvc.perform(get("/api/me/ping").header("Authorization", customer)).andExpect(status().isOk());
        mvc.perform(get("/api/me/ping").header("Authorization", company)).andExpect(status().isForbidden());
        mvc.perform(get("/api/me/ping").header("Authorization", admin)).andExpect(status().isForbidden());
    }

    @Test
    void jwtCarriesUserIdRoleAndCompany() {
        User user = users.save(new User("co@test.local", encoder.encode("pw"), Role.COMPANY, 7L));
        String token = jwt.issue(new AuthPrincipal(user.getId(), Role.COMPANY, 7L, null));
        AuthPrincipal parsed = jwt.parse(token).orElseThrow();

        org.assertj.core.api.Assertions.assertThat(parsed.userId()).isEqualTo(user.getId());
        org.assertj.core.api.Assertions.assertThat(parsed.role()).isEqualTo(Role.COMPANY);
        org.assertj.core.api.Assertions.assertThat(parsed.companyId()).isEqualTo(7L);
    }

    private String token(Role role, Long companyId) {
        return "Bearer " + jwt.issue(new AuthPrincipal(99L, role, companyId, null));
    }

    private org.springframework.test.web.servlet.RequestBuilder login(String email, String password) throws Exception {
        return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(java.util.Map.of("email", email, "password", password)));
    }
}
