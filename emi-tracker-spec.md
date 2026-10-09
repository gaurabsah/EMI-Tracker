# EMI Tracker: SDD Specification (v1)

**Flow followed:** Epic → Artefacts (ADR, Product spec, Testing strategy) → Review gate → Implementation spec → Review gate → Triage → Stories → Tasks → Implement

**Status:** Both review gates approved. Ready for implementation.

---

## 1. Epic

Companies sell items on instalments, and customers can't easily see what they owe, when, or how much of each payment is interest. v1 delivers a web app where a Company defines plans, sells on them, and records payments, and a Customer sees an accurate schedule and balance.

---

## 2. Product Spec

### Actors

| Actor | Description |
|---|---|
| **Admin** | Platform owner, seeded at startup. Creates and deactivates Companies, and views all data. |
| **Company** | The seller. Creates plans, creates Customers, registers purchases, and records payments. |
| **Customer** | The buyer. Read-only view of their own purchases, schedules, and balances. |

Every actor logs in (email + password).

### Core flow

1. A Company creates a **plan**: interest method (flat or reducing balance), annual rate, and tenure.
2. A Company registers a **purchase**: customer, item price, down payment, and plan.
3. The system generates an **amortization schedule**. Down payment 0 means full instalments, and down payment above 0 means partial payment + instalments. This is one flow for both options.
4. A Company records a **payment** against an instalment (manual recording, no payment gateway).
5. A Customer sees upcoming, paid, and overdue instalments, the principal/interest split per instalment, and the outstanding balance.

### Assumptions (approved)

- A Customer belongs to one Company in v1.
- A Company creates Customer accounts, with no self-registration.
- An instalment is paid in full or not at all (no partial payments of one instalment).
- Currency is INR only.

### Out of scope for v1

Payment gateway, late fees, prepayment or foreclosure, notifications, multi-currency, schedule preview before purchase, password reset, pagination, editing or deleting plans, payment history beyond `paid_on`.

---

## 3. Architecture Decision Records

### ADR-1: Money handling
Store amounts as integer paise (`long`), never floats. Rounding happens per instalment, and the final instalment absorbs the remainder. Rates are stored in basis points.

### ADR-2: Interest strategy
One calculator interface with two implementations, `FlatRate` and `ReducingBalance`, selected by the plan. The schedule is generated once at purchase time and stored. A purchase copies method, rate, and tenure from the plan, so later plan edits never change existing purchases.

### ADR-3: Tenancy and access
Every company-owned row carries `company_id`. Role-based access (Admin, Company, Customer) is enforced in the backend. Company scoping comes from the JWT, never from request parameters. Cross-tenant access returns **404** (not 403) so ids can't be probed.

### ADR-4: Stack (revised)
Java 21 + Spring Boot 3, Spring Data JPA, H2 in-memory, React (Vite). One repo with `/backend` and `/frontend`.

- **Tradeoff:** all data is lost on restart, so a startup seeder creates the Admin (and optionally demo data).
- **Migration path:** because we go through JPA, moving to Postgres or the Python/FastAPI rewrite is a config change plus a new backend, not a redesign.

### ADR-5: Authentication
Email + password, BCrypt hashes, stateless JWT carrying `userId` and `role`. Spring Security enforces role per route.

---

## 4. Testing Strategy

- **Unit tests on the calculators** against hand-checked values. Both cases use ₹50,000 financed (₹70,000 price minus ₹20,000 down), 12 months, 12% p.a.:
  - Reducing balance: EMI = **₹4,442.44**
  - Flat rate: **₹4,666.67** per month (total interest ₹6,000)
- **Invariants:** principal parts sum to the financed amount, the final balance is exactly 0, and the instalment count equals the tenure.
- **API tests for role boundaries:** a Customer can't see another customer's data, and a Company can't see another company's. Unauthenticated requests get 401, and wrong-role requests get 403.
- **One end-to-end test:** plan → purchase → schedule → payment → customer's balance.

---

## 5. Implementation Spec

### 5.1 Data model

All money is in integer paise and rates are in basis points.

| Table | Columns |
|---|---|
| `users` | id, email (unique), password_hash, role (`ADMIN`/`COMPANY`/`CUSTOMER`), company_id (null for Admin) |
| `companies` | id, name, active |
| `customers` | id, user_id, company_id, name, phone |
| `plans` | id, company_id, name, method (`FLAT`/`REDUCING`), annual_rate_bps, tenure_months |
| `purchases` | id, company_id, customer_id, item_name, price, down_payment, financed, method, annual_rate_bps, tenure_months, start_date |
| `instalments` | id, purchase_id, seq, due_date, principal, interest, amount, status (`PENDING`/`PAID`), paid_on |

Notes:
- **Overdue is derived** (`PENDING` and `due_date` before today), not stored.
- The down payment is recorded on the purchase and is not an instalment.
- **Outstanding balance** = sum of principal on `PENDING` instalments.

### 5.2 Calculation rules

- `financed = price - down_payment`. Validate `0 ≤ down_payment < price`.
- Monthly rate `r = annual_rate / 12`. Due dates are `start_date` plus `seq` months.
- **Reducing balance:**
  - `EMI = P·r·(1+r)^n / ((1+r)^n − 1)`, rounded half-up to paise.
  - Each month: interest = `round(balance·r)`, principal = `EMI − interest`.
  - The last instalment clears the exact remaining balance.
- **Flat rate:**
  - Total interest = `P · annual_rate · n/12`.
  - Principal and interest are each split evenly across `n`, with the rounding remainder in the last instalment.

### 5.3 API

| Role | Endpoint | Purpose |
|---|---|---|
| Public | `POST /api/auth/login` | Returns JWT + role |
| Admin | `POST /api/admin/companies` | Create company + its login |
| Admin | `GET /api/admin/companies` | List companies |
| Company | `POST /api/plans`, `GET /api/plans` | Manage plans |
| Company | `POST /api/customers`, `GET /api/customers` | Create customer + login |
| Company | `POST /api/purchases`, `GET /api/purchases`, `GET /api/purchases/{id}` | Sell and view; creating generates the schedule |
| Company | `POST /api/instalments/{id}/pay` | Mark an instalment paid |
| Customer | `GET /api/me/purchases`, `GET /api/me/purchases/{id}` | Own purchases, schedule, and outstanding balance |

### 5.4 Screens

- **Login** (shared), then redirects by role.
- **Admin:** companies list + create form.
- **Company:** plans, customers, and purchases (list + create form), plus a purchase detail page with the schedule table and a "Mark paid" button.
- **Customer:** purchases list, plus a detail page with the schedule, status badges, and outstanding balance.

---

## 6. Triage

- **Size:** 6 stories, about 30 tasks. All are v1-critical.
- **Risk 1, money rounding:** wrong EMI numbers would make the product useless, so the calculators come first and are tested against the hand-checked values.
- **Risk 2, tenant leaks:** every story that exposes data includes a cross-tenant 404 test.
- **Risk 3, H2 data loss on restart:** the startup seeder keeps the app usable after every restart.
- **Critical path:** S1 → S3 → S4 → S5. S2 has no dependencies and runs in parallel with S1, and S6 runs in parallel with S5.

---

## 7. Stories

| # | Story | Depends on | Done when (verify) |
|---|---|---|---|
| S1 | Skeleton + auth | none | Seeded Admin logs in and gets a JWT, no token gives 401, wrong role gives 403 |
| S2 | Interest calculators | none | Check values match (reducing ₹4,442.44, flat ₹4,666.67), invariants hold |
| S3 | Admin and Company management | S1 | Admin creates Company + login, Company creates plans and Customers + logins |
| S4 | Purchases + schedule generation | S2, S3 | Purchase creates a stored schedule, down-payment validation works, cross-tenant access gives 404 |
| S5 | Record payments | S4 | Instalment goes to PAID with `paid_on`, a double-pay is rejected, overdue is derived correctly |
| S6 | Customer views | S4 | Customer sees only their own purchases, schedule, and outstanding balance |

**Implementation order:** S1 ∥ S2 → S3 → S4 → S5 ∥ S6

---

## 8. Tasks

### S1: Skeleton + auth
- [ ] **T1.1** Maven/Spring Boot project with H2, JPA, and Security. *verify: app boots*
- [ ] **T1.2** `User` entity, BCrypt, and startup Admin seeder (depends on T1.1)
- [ ] **T1.3** `POST /api/auth/login` + JWT filter carrying `userId` and `role` (depends on T1.2)
- [ ] **T1.4** Role-based route rules (depends on T1.3). *verify: 401 and 403 tests*
- [ ] **T1.5** Vite + React app with login page and role-based redirect (depends on T1.3)

### S2: Interest calculators
- [ ] **T2.1** `Money` convention (paise as `long`) and calculator interface returning a list of (principal, interest, amount)
- [ ] **T2.2** `ReducingBalance` implementation (depends on T2.1)
- [ ] **T2.3** `FlatRate` implementation (depends on T2.1)
- [ ] **T2.4** Unit tests: check values, principal sums to the financed amount, final balance is 0, instalment count equals tenure (depends on T2.2, T2.3)

### S3: Admin and Company management
- [ ] **T3.1** `Company`, `Customer`, and `Plan` entities and repositories (depends on T1.2)
- [ ] **T3.2** Admin endpoints: create and list companies, with the company login created in one transaction (depends on T3.1, T1.4)
- [ ] **T3.3** Company endpoints: plans and customers, scoped by `company_id` from the token (depends on T3.1, T1.4)
- [ ] **T3.4** API tests, including cross-tenant 404 (depends on T3.2, T3.3)
- [ ] **T3.5** React screens: Admin companies, Company plans, and Company customers (depends on T3.2, T3.3, T1.5)

### S4: Purchases + schedule
- [ ] **T4.1** `Purchase` and `Instalment` entities (depends on T3.1)
- [ ] **T4.2** Purchase service: validate `0 ≤ down_payment < price`, copy plan terms, call the calculator, persist the schedule (depends on T2.4, T4.1)
- [ ] **T4.3** `POST/GET /api/purchases` and `GET /api/purchases/{id}` (depends on T4.2)
- [ ] **T4.4** API tests: both plan methods, validation errors, and cross-tenant 404 (depends on T4.3)
- [ ] **T4.5** React: purchases list, create form, and detail page with the schedule table (depends on T4.3)

### S5: Payments
- [ ] **T5.1** `POST /api/instalments/{id}/pay`: set PAID and `paid_on`, reject if already paid (depends on T4.3)
- [ ] **T5.2** Derived overdue flag in the purchase response (depends on T4.3)
- [ ] **T5.3** API tests: pay, double-pay, and cross-tenant (depends on T5.1)
- [ ] **T5.4** React "Mark paid" button and status badges (depends on T5.1, T4.5)

### S6: Customer views
- [ ] **T6.1** `GET /api/me/purchases` and `/{id}` with outstanding balance (depends on T4.3)
- [ ] **T6.2** API tests: a Customer can't read another Customer's purchase (depends on T6.1)
- [ ] **T6.3** React Customer pages: list and detail (depends on T6.1, T1.5)

---

## 9. Implement

**Start with S2** (calculators): pure logic, no dependencies, highest risk. Implement it test-first with the minimum code, in the order T2.1 → T2.4.
