# EMI Tracker

Companies sell items on instalments; customers see their schedule and balance. Spec: `emi-tracker-spec.md`.

## Run

Backend (Java 21+, Maven), http://localhost:8080:

```
cd backend && mvn spring-boot:run
```

Frontend (Node), http://localhost:5173 (proxies `/api` to the backend):

```
cd frontend && npm install && npm run dev
```

Data is in-memory H2, so it resets on every restart. The Admin is re-seeded at startup:
`admin@emitracker.local` / `admin123` (override with `app.admin.email` / `app.admin.password`).

Flow: log in as Admin, create a Company; log in as that Company, create a plan and a customer, register a purchase and mark instalments paid; log in as the Customer to see the schedule and balance.

## Test

```
cd backend && mvn test
```

## Notes

- Money is integer paise and rates are basis points in the API; the UI converts to rupees and percent.
- `frontend/.npmrc` pins the public npm registry for this project.
- Set `app.jwt.secret` to a real secret outside local development.
