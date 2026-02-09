# System Design & Architecture: B2B Demand-Driven Fresh Vegetable Supply Chain Platform

## 1. High-Level System Architecture

```
+-------------------+         +-------------------+         +-------------------+
|    Farmer App     | <-----> |                   | <-----> |   Retailer App    |
| (Mobile/Web, UX)  |         |                   |         | (Mobile/Web, UX)  |
+-------------------+         |                   |         +-------------------+
                              |                   |
+-------------------+         |                   |         +-------------------+
| Mediator App      | <-----> |   API Gateway     | <-----> |   Admin Dashboard |
| (Mobile/Web, UX)  |         | (Spring Boot)     |         | (Web, UX)         |
+-------------------+         |                   |         +-------------------+
                              |                   |
                              +-------------------+
                                       |
                                       v
                              +-------------------+
                              |  Core Services    |
                              | (Spring Boot)     |
                              | - User Mgmt       |
                              | - Supply/Demand   |
                              | - Order Mgmt      |
                              | - Pricing         |
                              | - Freshness       |
                              +-------------------+
                                       |
                                       v
                              +-------------------+
                              |   Database (SQL)  |
                              +-------------------+
```

## 2. Component-wise Architecture
- Frontend: Mobile-first, localized UI (Farmer, Mediator, Retailer: PWA/Android; Admin: Web)
- API Gateway: Auth, routing, rate limiting
- Core Services: User, Supply, Demand, Order, Pricing, Freshness (Spring Boot)
- Database: Relational (PostgreSQL/MySQL)

## 3. User Roles & Permissions
| Role      | Permissions                                                                 |
|-----------|-----------------------------------------------------------------------------|
| Admin     | Full access: approve/reject orders, match supply/demand, set prices, view KPIs|
| Farmer    | Add/edit harvests, view matched orders, confirm harvest readiness            |
| Mediator  | View assigned collections, update delivery status                            |
| Retailer  | Post demand, view matched supply, confirm receipt                            |

## 4. Core Features (MVP)
- Farmer: Register/login, add harvest, view matches
- Retailer: Register/login, post demand, view matches
- Mediator: View collections, update delivery
- Admin: Approve/normalize prices, match supply/demand, dashboard

## 5. Order Lifecycle & Workflows
```
Retailer posts demand
        |
        v
Farmer posts harvest
        |
        v
Admin matches supply & demand, normalizes price, creates order
        |
        v
Order assigned to Mediator for collection
        |
        v
Mediator collects from Farmer, delivers to Retailer (within 24–48h)
        |
        v
Retailer confirms receipt
        |
        v
Order closed, freshness tracked
```

## 6. Pricing Normalization Logic
- Admin reviews all matched supply-demand pairs.
- System suggests a normalized price (e.g., average of expected price and market rate).
- Admin can approve, adjust, or override.
- No order proceeds without Admin approval.

## 7. Freshness Tracking Mechanism
- Each order records:
  - Harvest date/time (Farmer input)
  - Collection date/time (Mediator input)
  - Delivery date/time (Retailer input)
- System calculates time from harvest to delivery.
- Orders exceeding 48h are flagged for Admin review.

## 8. Database Schema (Key Tables & Fields)
**User**
- id (PK)
- name
- role (ENUM: ADMIN, FARMER, MEDIATOR, RETAILER)
- phone
- password_hash
- language_preference

**Harvest**
- id (PK)
- farmer_id (FK)
- vegetable_type
- quantity
- harvest_date
- expected_price
- status (PENDING, MATCHED, COMPLETED)

**Demand**
- id (PK)
- retailer_id (FK)
- vegetable_type
- quantity
- required_date
- status (PENDING, MATCHED, COMPLETED)

**Order**
- id (PK)
- harvest_id (FK)
- demand_id (FK)
- admin_id (FK)
- mediator_id (FK)
- normalized_price
- status (CREATED, COLLECTED, DELIVERED, CLOSED, FLAGGED)
- created_at
- collected_at
- delivered_at

**FreshnessLog**
- id (PK)
- order_id (FK)
- harvest_date
- delivered_date
- freshness_hours (calculated)

## 9. Admin Dashboard KPIs
- Total orders (today, week, month)
- Average time from harvest to delivery
- % of orders delivered within 24/48h
- Supply-demand match rate
- Wastage (unmatched harvests)
- Price trends (expected vs normalized)
- Active users by role

## 10. Scalability & Future Enhancements
**For MVP:**
- Single region, one vegetable, minimal features
- Mobile-first, local language support
- Simple, reliable workflows

**For Future:**
- Multi-region, multi-vegetable support
- Advanced AI for demand forecasting, dynamic pricing
- Payment integration
- Cold chain tracking (IoT sensors)
- Advanced analytics & reporting
- API integrations with logistics partners
- Role-based notifications & chat

## 11. Practical Implementation Details
- Tech Stack: Java, Spring Boot, PostgreSQL/MySQL, React/Angular (Admin), PWA/Android (Farmer, Mediator, Retailer)
- Localization: Resource bundles for UI text, support right-to-left scripts if needed
- Mobile-first: Large buttons, minimal text, icon-driven navigation
- Reliability: Transactional DB, retry logic, audit logs
- Speed: Optimized queries, caching for static data

## 12. Example Component Diagram (Text)
```
[Farmer App]      [Retailer App]      [Mediator App]      [Admin Dashboard]
      |                 |                   |                     |
      +-----------------+-------------------+---------------------+
                                |
                        [API Gateway (Spring Boot)]
                                |
                        [Core Services (Spring Boot)]
                                |
                        [Relational Database]
```
