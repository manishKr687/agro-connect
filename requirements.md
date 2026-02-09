# MVP Requirements: B2B Demand-Driven Fresh Vegetable Supply Chain Platform

## Core Business Rules
- Only fresh vegetables are traded.
- Farmers update upcoming harvest data (vegetable type, quantity, harvest date, expected price).
- Retailers post demand in advance (vegetable, quantity, required date).
- Admin controls the system, matches supply with demand, normalizes prices, and approves purchases.
- Vegetables are collected by mediators and delivered to retailers within 24–48 hours of harvest.
- The system must avoid buying without confirmed demand to prevent wastage.

## User Roles & Permissions
- **Admin:** Full access, system control, price normalization, order approval.
- **Farmer:** Add/edit harvests, view matched orders, confirm harvest readiness.
- **Mediator:** View assigned collections, update delivery status.
- **Retailer:** Post demand, view matched supply, confirm receipt.

## Core Features (MVP)
- Farmer: Register/login, add harvest, view matches.
- Retailer: Register/login, post demand, view matches.
- Mediator: View collections, update delivery.
- Admin: Approve/normalize prices, match supply/demand, dashboard.

## Order Lifecycle
1. Retailer posts demand
2. Farmer posts harvest
3. Admin matches, normalizes price, creates order
4. Mediator collects and delivers
5. Retailer confirms receipt
6. Order closed, freshness tracked

## Pricing Normalization
- Admin reviews and approves/adjusts system-suggested price.

## Freshness Tracking
- Track harvest, collection, and delivery times. Flag orders >48h.

## Database Schema (Key Tables)
- User, Harvest, Demand, Order, FreshnessLog

## Admin Dashboard KPIs
- Total orders, avg. delivery time, % delivered within 24/48h, match rate, wastage, price trends, active users.

## Constraints
- Simple, mobile-first UI for low digital literacy
- Local language support
- Prioritize speed, reliability, freshness

## Goal
- Lean, scalable MVP for one region/vegetable, expandable to full platform.
