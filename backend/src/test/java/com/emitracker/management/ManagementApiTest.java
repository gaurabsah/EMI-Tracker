package com.emitracker.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.emitracker.TestApi;
import com.emitracker.TestApi.Person;
import com.emitracker.TestApi.Tenant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class ManagementApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private TestApi api() {
        return new TestApi(mvc, json);
    }

    @Test
    void adminCreatesCompanyWithLoginAndListsIt() throws Exception {
        TestApi api = api();
        String email = TestApi.uniqueEmail("co");

        api.post("/api/admin/companies", api.adminToken(),
                Map.of("name", "Acme Finance", "email", email, "password", TestApi.PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Acme Finance"))
                .andExpect(jsonPath("$.active").value(true));

        api.login(email, TestApi.PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("COMPANY"));

        JsonNode list = api.body(api.get("/api/admin/companies", api.adminToken()));
        assertThat(list.findValuesAsText("email")).contains(email);
    }

    @Test
    void duplicateEmailIs409AndShortPasswordIs400() throws Exception {
        TestApi api = api();
        Tenant existing = api.createCompany("First");

        api.post("/api/admin/companies", api.adminToken(),
                Map.of("name", "Second", "email", existing.email(), "password", TestApi.PASSWORD))
                .andExpect(status().isConflict());
        api.post("/api/admin/companies", api.adminToken(),
                Map.of("name", "Third", "email", TestApi.uniqueEmail("co"), "password", "short"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void companyCreatesPlansAndCustomersWithLogins() throws Exception {
        TestApi api = api();
        Tenant company = api.createCompany("Plans Co");

        api.post("/api/plans", company.token(),
                Map.of("name", "Reducing 12m", "method", "REDUCING", "annualRateBps", 1200, "tenureMonths", 12))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.method").value("REDUCING"))
                .andExpect(jsonPath("$.annualRateBps").value(1200));

        Person customer = api.createCustomer(company.token(), "Asha");
        api.login(customer.email(), TestApi.PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        assertThat(api.body(api.get("/api/plans", company.token()))).hasSize(1);
        assertThat(api.body(api.get("/api/customers", company.token())).get(0).get("email").asText())
                .isEqualTo(customer.email());
    }

    @Test
    void invalidPlansAreRejected() throws Exception {
        TestApi api = api();
        Tenant company = api.createCompany("Validation Co");

        api.post("/api/plans", company.token(),
                Map.of("name", "Bad", "method", "FLAT", "annualRateBps", 1200, "tenureMonths", 0))
                .andExpect(status().isBadRequest());
        api.post("/api/plans", company.token(),
                Map.of("name", "Bad", "method", "COMPOUND", "annualRateBps", 1200, "tenureMonths", 6))
                .andExpect(status().isBadRequest());
        api.post("/api/plans", company.token(),
                Map.of("name", "Bad", "method", "FLAT", "annualRateBps", -1, "tenureMonths", 6))
                .andExpect(status().isBadRequest());
    }

    @Test
    void companyCannotSeeAnotherCompanysPlansOrCustomers() throws Exception {
        TestApi api = api();
        Tenant a = api.createCompany("Company A");
        Tenant b = api.createCompany("Company B");
        api.createPlan(a.token(), "FLAT", 1000, 6);
        api.createCustomer(a.token(), "A's customer");

        assertThat(api.body(api.get("/api/plans", b.token()))).isEmpty();
        assertThat(api.body(api.get("/api/customers", b.token()))).isEmpty();
        assertThat(api.body(api.get("/api/plans", a.token()))).hasSize(1);
        assertThat(api.body(api.get("/api/customers", a.token()))).hasSize(1);
    }

    @Test
    void companyScopeComesFromTokenNotRequestBody() throws Exception {
        TestApi api = api();
        Tenant a = api.createCompany("Company A");
        Tenant b = api.createCompany("Company B");

        api.post("/api/plans", a.token(),
                Map.of("name", "Sneaky", "method", "FLAT", "annualRateBps", 1000, "tenureMonths", 6,
                        "companyId", b.companyId()))
                .andExpect(status().isCreated());

        assertThat(api.body(api.get("/api/plans", a.token()))).hasSize(1);
        assertThat(api.body(api.get("/api/plans", b.token()))).isEmpty();
    }

    @Test
    void onlyAdminCanManageCompanies() throws Exception {
        TestApi api = api();
        Tenant company = api.createCompany("Not Admin");
        Person customer = api.createCustomer(company.token(), "Cust");

        api.get("/api/admin/companies", company.token()).andExpect(status().isForbidden());
        api.get("/api/admin/companies", customer.token()).andExpect(status().isForbidden());
        api.get("/api/plans", customer.token()).andExpect(status().isForbidden());
        api.get("/api/customers", api.adminToken()).andExpect(status().isForbidden());
    }

    @Test
    void deactivatedCompanyAndItsCustomersCannotLogIn() throws Exception {
        TestApi api = api();
        Tenant company = api.createCompany("Soon Inactive");
        Person customer = api.createCustomer(company.token(), "Cust");

        api.patch("/api/admin/companies/" + company.companyId() + "/active", api.adminToken(),
                Map.of("active", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        api.login(company.email(), TestApi.PASSWORD).andExpect(status().isUnauthorized());
        api.login(customer.email(), TestApi.PASSWORD).andExpect(status().isUnauthorized());

        api.patch("/api/admin/companies/" + company.companyId() + "/active", api.adminToken(),
                Map.of("active", true)).andExpect(status().isOk());
        api.login(company.email(), TestApi.PASSWORD).andExpect(status().isOk());
    }

    @Test
    void customerTokenCarriesCustomerId() throws Exception {
        TestApi api = api();
        Tenant company = api.createCompany("Claims Co");
        Person customer = api.createCustomer(company.token(), "Cust");

        String payload = new String(java.util.Base64.getUrlDecoder()
                .decode(customer.token().split("\\.")[1]));
        JsonNode claims = json.readTree(payload);
        assertThat(claims.get("customerId").asLong()).isEqualTo(customer.customerId());
        assertThat(claims.get("companyId").asLong()).isEqualTo(company.companyId());
        assertThat(claims.get("role").asText()).isEqualTo("CUSTOMER");
    }
}
