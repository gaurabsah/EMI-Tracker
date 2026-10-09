package com.emitracker;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Thin helper so API tests read as flows. Tokens are returned as full "Bearer ..." header values. */
public class TestApi {

    public static final String ADMIN_EMAIL = "admin@emitracker.local";
    public static final String ADMIN_PASSWORD = "admin123";
    public static final String PASSWORD = "password123";

    private final MockMvc mvc;
    private final ObjectMapper json;

    public TestApi(MockMvc mvc, ObjectMapper json) {
        this.mvc = mvc;
        this.json = json;
    }

    public static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
    }

    public ResultActions login(String email, String password) throws Exception {
        return post("/api/auth/login", null, Map.of("email", email, "password", password));
    }

    public String token(String email, String password) throws Exception {
        String body = login(email, password).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("token").asText();
    }

    public String adminToken() throws Exception {
        return token(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    public ResultActions post(String url, String token, Object body) throws Exception {
        var req = MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        if (token != null) {
            req.header("Authorization", token);
        }
        return mvc.perform(req);
    }

    public ResultActions patch(String url, String token, Object body) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.patch(url).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    public ResultActions get(String url, String token) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(url).header("Authorization", token));
    }

    public JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    /** Creates a company via the Admin API and returns its login token plus ids. */
    public Tenant createCompany(String name) throws Exception {
        String email = uniqueEmail("co");
        JsonNode created = body(post("/api/admin/companies", adminToken(),
                Map.of("name", name, "email", email, "password", PASSWORD)));
        return new Tenant(created.get("id").asLong(), email, token(email, PASSWORD));
    }

    public long createPlan(String companyToken, String method, int rateBps, int tenure) throws Exception {
        return body(post("/api/plans", companyToken,
                Map.of("name", method + " " + tenure, "method", method, "annualRateBps", rateBps,
                        "tenureMonths", tenure))).get("id").asLong();
    }

    public Person createCustomer(String companyToken, String name) throws Exception {
        String email = uniqueEmail("cust");
        long id = body(post("/api/customers", companyToken,
                Map.of("name", name, "phone", "9999999999", "email", email, "password", PASSWORD)))
                .get("id").asLong();
        return new Person(id, email, token(email, PASSWORD));
    }

    public record Tenant(long companyId, String email, String token) {
    }

    public record Person(long customerId, String email, String token) {
    }
}
