package com.emitracker.purchase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
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
class PurchaseApiTest {

    private static final long PRICE = 7_000_000L;
    private static final long DOWN = 2_000_000L;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private TestApi api() {
        return new TestApi(mvc, json);
    }

    private static LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Kolkata"));
    }

    private Map<String, Object> purchaseBody(long customerId, long planId, long price, long down) {
        Map<String, Object> body = new HashMap<>();
        body.put("customerId", customerId);
        body.put("planId", planId);
        body.put("itemName", "Laptop");
        body.put("price", price);
        body.put("downPayment", down);
        return body;
    }

    private JsonNode createPurchase(TestApi api, String token, long customerId, long planId, long price, long down)
            throws Exception {
        return api.body(api.post("/api/purchases", token, purchaseBody(customerId, planId, price, down))
                .andExpect(status().isCreated()));
    }

    @Test
    void reducingBalancePurchaseStoresTheSchedule() throws Exception {
        TestApi api = api();
        Tenant company = api.createCompany("Reducing Co");
        long plan = api.createPlan(company.token(), "REDUCING", 1200, 12);
        Person customer = api.createCustomer(company.token(), "Asha");

        JsonNode created = createPurchase(api, company.token(), customer.customerId(), plan, PRICE, DOWN);

        assertThat(created.get("financed").asLong()).isEqualTo(5_000_000L);
        assertThat(created.get("method").asText()).isEqualTo("REDUCING");
        assertThat(created.get("annualRateBps").asInt()).isEqualTo(1200);
        assertThat(created.get("tenureMonths").asInt()).isEqualTo(12);
        assertThat(created.get("outstandingBalance").asLong()).isEqualTo(5_000_000L);

        JsonNode schedule = created.get("instalments");
        assertThat(schedule).hasSize(12);
        assertThat(schedule.get(0).get("amount").asLong()).isEqualTo(444_244L);
        assertThat(schedule.get(0).get("interest").asLong()).isEqualTo(50_000L);
        long principalSum = 0;
        LocalDate start = LocalDate.parse(created.get("startDate").asText());
        for (int i = 0; i < 12; i++) {
            principalSum += schedule.get(i).get("principal").asLong();
            assertThat(schedule.get(i).get("seq").asInt()).isEqualTo(i + 1);
            assertThat(LocalDate.parse(schedule.get(i).get("dueDate").asText())).isEqualTo(start.plusMonths(i + 1));
            assertThat(schedule.get(i).get("status").asText()).isEqualTo("PENDING");
        }
        assertThat(principalSum).isEqualTo(5_000_000L);

        JsonNode fetched = api.body(api.get("/api/purchases/" + created.get("id").asLong(), company.token()));
        assertThat(fetched.get("instalments")).isEqualTo(schedule);
    }

    @Test
    void flatRatePurchaseUsesFlatCalculator() throws Exception {
        TestApi api = api();
        Tenant company = api.createCompany("Flat Co");
        long plan = api.createPlan(company.token(), "FLAT", 1200, 12);
        Person customer = api.createCustomer(company.token(), "Ravi");

        JsonNode schedule = createPurchase(api, company.token(), customer.customerId(), plan, PRICE, DOWN)
                .get("instalments");

        assertThat(schedule.get(0).get("amount").asLong()).isEqualTo(466_667L);
        long interest = 0;
        for (JsonNode i : schedule) {
            interest += i.get("interest").asLong();
        }
        assertThat(interest).isEqualTo(600_000L);
    }

    @Test
    void zeroDownPaymentFinancesTheFullPrice() throws Exception {
        TestApi api = api();
        Tenant company = api.createCompany("Zero Down Co");
        long plan = api.createPlan(company.token(), "REDUCING", 1200, 6);
        Person customer = api.createCustomer(company.token(), "Meera");

        JsonNode created = createPurchase(api, company.token(), customer.customerId(), plan, PRICE, 0);

        assertThat(created.get("financed").asLong()).isEqualTo(PRICE);
        assertThat(created.get("instalments")).hasSize(6);
    }

    @Test
    void invalidPurchasesAreRejected() throws Exception {
        TestApi api = api();
        Tenant company = api.createCompany("Validation Co");
        long plan = api.createPlan(company.token(), "FLAT", 1200, 6);
        Person customer = api.createCustomer(company.token(), "Cust");
        long c = customer.customerId();

        api.post("/api/purchases", company.token(), purchaseBody(c, plan, PRICE, PRICE)).andExpect(status().isBadRequest());
        api.post("/api/purchases", company.token(), purchaseBody(c, plan, PRICE, PRICE + 1)).andExpect(status().isBadRequest());
        api.post("/api/purchases", company.token(), purchaseBody(c, plan, PRICE, -1)).andExpect(status().isBadRequest());
        api.post("/api/purchases", company.token(), purchaseBody(c, plan, 0, 0)).andExpect(status().isBadRequest());

        Map<String, Object> noItem = purchaseBody(c, plan, PRICE, DOWN);
        noItem.put("itemName", " ");
        api.post("/api/purchases", company.token(), noItem).andExpect(status().isBadRequest());

        assertThat(api.body(api.get("/api/purchases", company.token()))).isEmpty();
    }

    @Test
    void companyCannotUseOrSeeAnotherCompanysData() throws Exception {
        TestApi api = api();
        Tenant a = api.createCompany("Company A");
        Tenant b = api.createCompany("Company B");
        long planA = api.createPlan(a.token(), "FLAT", 1200, 6);
        Person customerA = api.createCustomer(a.token(), "A's customer");
        long planB = api.createPlan(b.token(), "FLAT", 1200, 6);
        Person customerB = api.createCustomer(b.token(), "B's customer");
        long purchaseA = createPurchase(api, a.token(), customerA.customerId(), planA, PRICE, DOWN).get("id").asLong();

        api.post("/api/purchases", b.token(), purchaseBody(customerA.customerId(), planB, PRICE, DOWN))
                .andExpect(status().isNotFound());
        api.post("/api/purchases", b.token(), purchaseBody(customerB.customerId(), planA, PRICE, DOWN))
                .andExpect(status().isNotFound());
        api.get("/api/purchases/" + purchaseA, b.token()).andExpect(status().isNotFound());
        assertThat(api.body(api.get("/api/purchases", b.token()))).isEmpty();
        assertThat(api.body(api.get("/api/purchases", a.token()))).hasSize(1);
    }

    @Test
    void payingMarksPaidWithDateAndReducesBalance() throws Exception {
        TestApi api = api();
        Tenant company = api.createCompany("Pay Co");
        long plan = api.createPlan(company.token(), "REDUCING", 1200, 12);
        Person customer = api.createCustomer(company.token(), "Payer");
        JsonNode purchase = createPurchase(api, company.token(), customer.customerId(), plan, PRICE, DOWN);
        JsonNode first = purchase.get("instalments").get(0);

        api.post("/api/instalments/" + first.get("id").asLong() + "/pay", company.token(), Map.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidOn").value(today().toString()));

        JsonNode after = api.body(api.get("/api/purchases/" + purchase.get("id").asLong(), company.token()));
        assertThat(after.get("outstandingBalance").asLong())
                .isEqualTo(5_000_000L - first.get("principal").asLong());
        assertThat(after.get("instalments").get(0).get("status").asText()).isEqualTo("PAID");
        assertThat(after.get("instalments").get(1).get("status").asText()).isEqualTo("PENDING");
    }

    @Test
    void doublePayIsRejected() throws Exception {
        TestApi api = api();
        Tenant company = api.createCompany("Double Pay Co");
        long plan = api.createPlan(company.token(), "FLAT", 1200, 6);
        Person customer = api.createCustomer(company.token(), "Payer");
        long instalment = createPurchase(api, company.token(), customer.customerId(), plan, PRICE, DOWN)
                .get("instalments").get(0).get("id").asLong();

        api.post("/api/instalments/" + instalment + "/pay", company.token(), Map.of()).andExpect(status().isOk());
        api.post("/api/instalments/" + instalment + "/pay", company.token(), Map.of()).andExpect(status().isConflict());
    }

    @Test
    void payingAnotherCompanysInstalmentIs404AndCustomersCannotPay() throws Exception {
        TestApi api = api();
        Tenant a = api.createCompany("Company A");
        Tenant b = api.createCompany("Company B");
        long plan = api.createPlan(a.token(), "FLAT", 1200, 6);
        Person customer = api.createCustomer(a.token(), "A's customer");
        long instalment = createPurchase(api, a.token(), customer.customerId(), plan, PRICE, DOWN)
                .get("instalments").get(0).get("id").asLong();

        api.post("/api/instalments/" + instalment + "/pay", b.token(), Map.of()).andExpect(status().isNotFound());
        api.post("/api/instalments/999999/pay", a.token(), Map.of()).andExpect(status().isNotFound());
        api.post("/api/instalments/" + instalment + "/pay", customer.token(), Map.of()).andExpect(status().isForbidden());

        JsonNode unchanged = api.body(api.get("/api/purchases", a.token()));
        assertThat(unchanged.get(0).get("outstandingBalance").asLong()).isEqualTo(5_000_000L);
    }

    @Test
    void overdueIsDerivedFromDueDateAndStatus() throws Exception {
        TestApi api = api();
        Tenant company = api.createCompany("Overdue Co");
        long plan = api.createPlan(company.token(), "FLAT", 1200, 6);
        Person customer = api.createCustomer(company.token(), "Late");

        Map<String, Object> body = purchaseBody(customer.customerId(), plan, PRICE, DOWN);
        body.put("startDate", today().minusMonths(3).toString());
        JsonNode purchase = api.body(api.post("/api/purchases", company.token(), body).andExpect(status().isCreated()));

        long overdue = 0;
        for (JsonNode i : purchase.get("instalments")) {
            boolean expected = LocalDate.parse(i.get("dueDate").asText()).isBefore(today());
            assertThat(i.get("overdue").asBoolean()).isEqualTo(expected);
            overdue += expected ? 1 : 0;
        }
        assertThat(overdue).isGreaterThanOrEqualTo(2);
        assertThat(purchase.get("overdueCount").asLong()).isEqualTo(overdue);

        long firstId = purchase.get("instalments").get(0).get("id").asLong();
        api.post("/api/instalments/" + firstId + "/pay", company.token(), Map.of())
                .andExpect(jsonPath("$.overdue").value(false));
        JsonNode after = api.body(api.get("/api/purchases/" + purchase.get("id").asLong(), company.token()));
        assertThat(after.get("overdueCount").asLong()).isEqualTo(overdue - 1);
    }

    @Test
    void futureInstalmentsAreNotOverdue() throws Exception {
        TestApi api = api();
        Tenant company = api.createCompany("Future Co");
        long plan = api.createPlan(company.token(), "FLAT", 1200, 6);
        Person customer = api.createCustomer(company.token(), "OnTime");

        JsonNode purchase = createPurchase(api, company.token(), customer.customerId(), plan, PRICE, DOWN);

        assertThat(purchase.get("overdueCount").asLong()).isZero();
    }
}
