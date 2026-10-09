package com.emitracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class EndToEndTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void planToPurchaseToScheduleToPaymentToCustomerBalance() throws Exception {
        TestApi api = new TestApi(mvc, json);

        TestApi.Tenant company = api.createCompany("End To End Finance");
        long plan = api.createPlan(company.token(), "REDUCING", 1200, 12);
        TestApi.Person customer = api.createCustomer(company.token(), "Asha");

        JsonNode purchase = api.body(api.post("/api/purchases", company.token(), Map.of(
                "customerId", customer.customerId(), "planId", plan, "itemName", "Laptop",
                "price", 7_000_000L, "downPayment", 2_000_000L)).andExpect(status().isCreated()));
        long purchaseId = purchase.get("id").asLong();
        assertThat(purchase.get("instalments")).hasSize(12);
        assertThat(purchase.get("instalments").get(0).get("amount").asLong()).isEqualTo(444_244L);

        JsonNode before = api.body(api.get("/api/me/purchases/" + purchaseId, customer.token()));
        assertThat(before.get("outstandingBalance").asLong()).isEqualTo(5_000_000L);

        JsonNode first = before.get("instalments").get(0);
        api.post("/api/instalments/" + first.get("id").asLong() + "/pay", company.token(), Map.of())
                .andExpect(status().isOk());

        JsonNode after = api.body(api.get("/api/me/purchases/" + purchaseId, customer.token()));
        assertThat(after.get("outstandingBalance").asLong())
                .isEqualTo(5_000_000L - first.get("principal").asLong());
        assertThat(after.get("instalments").get(0).get("status").asText()).isEqualTo("PAID");
    }
}
