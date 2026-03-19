# Admin Capabilities Documentation

## Overview
This document details the capabilities, workflows, and responsibilities of admin roles in the AgroConnect platform. It is designed for operational clarity, accountability, and scalability.

---

## 1. Admin Roles

| Role             | Scope                | Key Responsibilities                  |
|------------------|---------------------|----------------------------------------|
| Super Admin      | Global              | All admin management, system config, escalations |
| Operations Admin | Platform-wide/Local | Order approvals, supply-demand, logistics |
| Finance Admin    | Platform-wide/Local | Pricing, revenue, payouts, financial reports |
| Regional Admin   | Region-specific     | Local user/order management, compliance |

---

## 2. Permission Matrix

| Action/Feature           | Super Admin | Operations Admin | Finance Admin | Regional Admin |
|--------------------------|:-----------:|:---------------:|:-------------:|:--------------:|
| View all data            |     ✅      |       ✅        |      ✅       |      ✅*       |
| Approve users            |     ✅      |       ✅        |      ❌       |      ✅        |
| Approve orders           |     ✅      |       ✅        |      ❌       |      ✅        |
| Assign mediators         |     ✅      |       ✅        |      ❌       |      ✅        |
| Adjust prices            |     ✅      |       ❌        |      ✅       |      ❌        |
| View/approve payouts     |     ✅      |       ❌        |      ✅       |      ❌        |
| Block users              |     ✅      |       ✅        |      ❌       |      ✅        |
| View performance history |     ✅      |       ✅        |      ✅       |      ✅        |
| System config            |     ✅      |       ❌        |      ❌       |      ❌        |
| Regional data only       |     ❌      |       ❌        |      ❌       |      ✅        |

*Regional Admins see only their region’s data.

---

## 3. Key Workflows

### 3.1 Supply → Demand Matching
1. Retailer submits demand
2. System suggests matches
3. Admin reviews/approves
4. Admin assigns mediator
5. Order created

### 3.2 Price Normalization
1. Farmers submit prices
2. System averages price
3. Finance/Admin reviews/adjusts
4. Final retail price set

### 3.3 Exception Handling
1. Exception detected (cancellation, delay, dispute)
2. Admin notified
3. Admin investigates
4. Admin resolves or escalates
5. System logs outcome

---

## 4. Decision Models

| Decision Point         | Inputs                        | Decision Owner      | Criteria/Logic                        |
|-----------------------|-------------------------------|---------------------|---------------------------------------|
| Order Approval        | Supply, demand, match quality | Operations/Regional | Product match, timing, quantity       |
| Price Adjustment      | Farmer prices, market data    | Finance/Admin       | Fairness, market alignment            |
| User Approval/Block   | Registration, history, alerts | Super/Regional      | Verification, fraud signals, feedback |
| Exception Resolution  | Incident details, logs        | Operations/Admin    | Policy, impact, evidence              |

---

## 5. Risk Controls

| Risk Area            | Control Mechanism                        | Owner           |
|----------------------|------------------------------------------|-----------------|
| Fraudulent Users     | Manual approval, block/suspend, audit    | Super/Regional  |
| Data Integrity       | Role-based access, audit logs            | Super Admin     |
| Financial Errors     | Dual approval, reporting, audit trails   | Finance Admin   |
| Delivery Delays      | Mediator monitoring, alerts, reassignment| Operations Admin|
| Quality Disputes     | Evidence review, admin mediation         | Operations Admin|

---

## 6. Admin Responsibilities

- **Supply-demand balancing:** Match supply and demand to maximize efficiency and reduce waste.
- **Pricing governance:** Review and adjust prices for fairness and market alignment.
- **Order approvals:** Approve or reject system-suggested orders to maintain quality.
- **Logistics oversight:** Assign mediators, monitor deliveries, and intervene in delays.
- **Freshness monitoring:** Track aging inventory and delayed pickups to maintain quality.
- **Financial visibility:** Monitor revenue, payments, and financial flows for transparency.
- **User governance:** Approve/block users, monitor performance, and resolve disputes.
- **Exception handling:** Resolve cancellations, disputes, and delivery issues to minimize disruption.

---

## 7. User Governance Capabilities

- Approve/reject new farmers and retailers
- Monitor mediator reliability and performance
- Block or suspend fraudulent users
- View detailed user performance history

---

This documentation ensures clarity, accountability, and operational excellence for all admin roles in AgroConnect.
