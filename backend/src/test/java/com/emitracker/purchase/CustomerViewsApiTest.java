package com.emitracker.purchase;

import static org.assertj.core.api.Assertions.assertThat;
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
class CustomerViewsApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private long sell(TestApi api, Tenant company, long planId, Person customer, String item) throws Exception {
        return api.body(api.post("/api/purchases", company.token(), Map.of(
                "customerId", customer.customerId(), "planId", planId, "itemName", item,
                "price", 7_000_000L, "downPayment", 2_000_000L)).andExpect(status().isCreated()))
                .get("id").asLong();
    }

    @Test
    void customerSeesOnlyTheirOwnPurchasesWithScheduleAndBalance() throws Exception {
        TestApi api = new TestApi(mvc, json);
        Tenant company = api.createCompany("Shared Co");
        long plan = api.createPlan(company.token(), "REDUCING", 1200, 12);
        Person asha = api.createCustomer(company.token(), "Asha");
        Person ravi = api.createCustomer(company.token(), "Ravi");
        long ashaPurchase = sell(api, company, plan, asha, "Phone");
        long raviPurchase = sell(api, company, plan, ravi, "TV");

        JsonNode mine = api.body(api.get("/api/me/purchases", asha.token()));
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).get("id").asLong()).isEqualTo(ashaPurchase);
        assertThat(mine.get(0).get("outstandingBalance").asLong()).isEqualTo(5_000_000L);

        JsonNode detail = api.body(api.get("/api/me/purchases/" + ashaPurchase, asha.token()));
        assertThat(detail.get("instalments")).hasSize(12);
        assertThat(detail.get("instalments").get(0).get("amount").asLong()).isEqualTo(444_244L);

        api.get("/api/me/purchases/" + raviPurchase, asha.token()).andExpect(status().isNotFound());
        api.get("/api/me/purchases/" + ashaPurchase, ravi.token()).andExpect(status().isNotFound());
    }

    @Test
    void customerCannotReadAnotherCompanysPurchase() throws Exception {
        TestApi api = new TestApi(mvc, json);
        Tenant a = api.createCompany("Company A");
        Tenant b = api.createCompany("Company B");
        long planA = api.createPlan(a.token(), "FLAT", 1200, 6);
        Person customerA = api.createCustomer(a.token(), "A's customer");
        Person customerB = api.createCustomer(b.token(), "B's customer");
        long purchaseA = sell(api, a, planA, customerA, "Fridge");

        api.get("/api/me/purchases/" + purchaseA, customerB.token()).andExpect(status().isNotFound());
        assertThat(api.body(api.get("/api/me/purchases", customerB.token()))).isEmpty();
    }

    @Test
    void customerBalanceFollowsPaymentsAndMeEndpointsAreCustomerOnly() throws Exception {
        TestApi api = new TestApi(mvc, json);
        Tenant company = api.createCompany("Balance Co");
        long plan = api.createPlan(company.token(), "REDUCING", 1200, 12);
        Person customer = api.createCustomer(company.token(), "Cust");
        long purchase = sell(api, company, plan, customer, "Bike");
        JsonNode first = api.body(api.get("/api/purchases/" + purchase, company.token())).get("instalments").get(0);

        api.post("/api/instalments/" + first.get("id").asLong() + "/pay", company.token(), Map.of())
                .andExpect(status().isOk());

        JsonNode view = api.body(api.get("/api/me/purchases/" + purchase, customer.token()));
        assertThat(view.get("outstandingBalance").asLong()).isEqualTo(5_000_000L - first.get("principal").asLong());
        assertThat(view.get("instalments").get(0).get("status").asText()).isEqualTo("PAID");

        api.get("/api/me/purchases", company.token()).andExpect(status().isForbidden());
        api.get("/api/me/purchases", api.adminToken()).andExpect(status().isForbidden());
        api.get("/api/purchases", customer.token()).andExpect(status().isForbidden());
    }
}
