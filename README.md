# AgroConnect — B2B Demand-Driven Fresh Vegetable Supply Chain Platform

A full-stack platform connecting farmers, retailers, and delivery agents through an admin-managed supply-demand matching engine.

---

## Business Rules

- Only fresh vegetables are traded.
- Farmers post upcoming harvest data (crop, quantity, harvest date, expected price).
- Retailers post demand in advance (crop, quantity, required date, target price).
- Admin reviews AI-scored matches, creates delivery tasks, and assigns agents.
- Vegetables are collected and delivered to retailers within 24–48 hours of harvest.
- The system avoids unconfirmed purchases to prevent wastage.

---

## User Roles

| Role     | Permissions |
|----------|-------------|
| Admin    | Full access: match supply/demand, manage delivery tasks, manage users, view exceptions |
| Farmer   | Add/edit harvests, view matched orders, request withdrawal |
| Retailer | Post demand, view matched supply, request changes |
| Agent    | View assigned tasks, update delivery status |

---

## Delivery Task Lifecycle

```
Retailer posts demand
        |
        v
Farmer posts harvest
        |
        v
Admin reviews match suggestions (scored by MatchingService)
        |
        v
Admin creates DeliveryTask → status: ASSIGNED
        |
        v
Agent accepts → ACCEPTED
        |
        v
Agent picks up from farmer → PICKED_UP
        |
        v
Agent delivers to retailer → IN_TRANSIT → DELIVERED
        |
        v
Harvest: SOLD | Demand: FULFILLED
```

Exceptions (REJECTED tasks, withdrawal requests, stuck tasks >24h) are surfaced in the Admin dashboard.

---

## Matching Score Formula

| Factor         | Weight | Description |
|----------------|--------|-------------|
| Crop name match | 45%   | Case-insensitive exact match (required) |
| Quantity ratio  | 25%   | demand/harvest quantity, capped at 1.0 |
| Freshness       | 15%   | Days since harvest, decays over 7 days |
| Price alignment | 10%   | How close harvest price is to target price |
| Urgency         | 5%    | How soon the demand is required |

---

## Tech Stack

### Backend
- Java 17, Spring Boot 3.2
- Spring Security with stateless JWT (jjwt 0.12.6)
- Spring Data JPA + PostgreSQL
- Flyway database migrations
- Bucket4j rate limiting on `/api/auth/**`
- Spring Boot Actuator (health, metrics)

### Frontend
- React + Material UI (MUI)
- JWT authentication with session storage
- Axios for API calls
- Role-based routing and dashboards

---

## Getting Started

### Prerequisites
- Java 17+
- PostgreSQL running on `localhost:5432` with database `agroconnect`
- Node.js 18+

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Dev defaults (see `application-dev.properties`):
- DB: `localhost:5432/agroconnect`, user `postgres`, password `postgres`
- Bootstrap admin: username `admin`, password `admin123`
- API runs on port `8081`

For production, set environment variables:
```
DB_URL, DB_USERNAME, DB_PASSWORD
JWT_SECRET
CORS_ALLOWED_ORIGINS
SPRING_PROFILES_ACTIVE=prod
```

### Frontend

```bash
cd frontend
npm install
npm start
```

Runs on `http://localhost:3000`. Update `src/api/axiosConfig.js` if the backend URL differs.

---

## Directory Structure

```
agro-connect/
├── backend/
│   ├── src/main/java/com/agroconnect/
│   │   ├── config/          # Security, Flyway bootstrap, CORS
│   │   ├── controller/      # REST controllers per role
│   │   ├── dto/             # Request/response DTOs with validation
│   │   ├── model/           # JPA entities (User, Harvest, Demand, DeliveryTask)
│   │   ├── repository/      # Spring Data JPA repositories
│   │   ├── security/        # JWT filter, rate limiting, SecurityConfig
│   │   └── service/         # Business logic (Matching, DeliveryTask, etc.)
│   ├── src/main/resources/
│   │   ├── application.properties         # Base config
│   │   ├── application-dev.properties     # Dev overrides
│   │   ├── application-prod.properties    # Prod overrides (gitignored)
│   │   └── db/migration/                  # Flyway SQL migrations
│   └── src/test/java/com/agroconnect/
│       └── service/         # Unit tests (Mockito)
└── frontend/
    ├── src/
    │   ├── api/             # Axios config and API helpers
    │   ├── components/      # Reusable UI components
    │   └── pages/           # Role-specific dashboards (Admin, Farmer, Retailer, Agent)
    └── public/
```

---

## Database Schema

**users** — id, username, password_hash, role (ADMIN/FARMER/RETAILER/AGENT)

**harvests** — id, farmer_id (FK), crop_name, quantity, harvest_date, expected_price, status (AVAILABLE/RESERVED/SOLD/WITHDRAWAL_REQUESTED)

**demands** — id, retailer_id (FK), crop_name, quantity, required_date, target_price, status (OPEN/RESERVED/FULFILLED/CANCELLED), requested_* fields for change requests

**delivery_tasks** — id, harvest_id (FK), demand_id (FK), assigned_agent_id (FK), status (ASSIGNED/ACCEPTED/PICKED_UP/IN_TRANSIT/DELIVERED/REJECTED/CANCELLED), assigned_at, rejection_reason

---

## Admin Dashboard KPIs

- Total delivery tasks and their statuses
- Exception queue (rejected tasks, withdrawal requests, change requests, stuck tasks)
- Match suggestions with scored candidates
- User management (create ADMIN/AGENT/FARMER/RETAILER accounts)

---

## Scalability & Future Enhancements

- Multi-region, multi-vegetable support
- AI-driven demand forecasting and dynamic pricing
- Payment integration
- Cold chain tracking via IoT sensors
- Push notifications per role
- Logistics partner API integrations
- Advanced analytics and reporting
