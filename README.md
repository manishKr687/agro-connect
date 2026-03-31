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

Production safeguards in this repo:
- `JWT_SECRET` must be at least 32 characters and cannot use the dev secret.
- `CORS_ALLOWED_ORIGINS` must use explicit HTTPS origins in the prod profile.
- `BOOTSTRAP_ADMIN_USERNAME` and `BOOTSTRAP_ADMIN_PASSWORD` must both be set or both be blank.
- `BOOTSTRAP_ADMIN_PASSWORD` must be at least 16 characters in the prod profile.
- The actuator server binds to `127.0.0.1:8082` in production.

### Frontend

```bash
cd frontend
npm install
npm start
```

Runs on `http://localhost:3000`. Update `src/api/axiosConfig.js` if the backend URL differs.

---

## Docker Deployment

The entire stack (frontend, backend, database) runs with a single command using Docker Compose.

### Architecture

```
Browser → :3000 (Nginx)
              ├── /          → serves React static files
              └── /api/*     → proxied to backend:8081
                                      ↓
                              Spring Boot :8081
                                      ↓
                              PostgreSQL :5432
```

Nginx acts as a reverse proxy — the browser never talks to the backend directly, which eliminates CORS issues in production.

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running

### Files

| File | Purpose |
|------|---------|
| `docker-compose.yml` | Orchestrates all three services |
| `backend/Dockerfile` | Multi-stage build: Maven → slim JRE image |
| `frontend/Dockerfile` | Multi-stage build: Node → Nginx image |
| `frontend/nginx.conf` | Serves static files, proxies `/api/` to backend |
| `.env.example` | Template for all required environment variables |

### Quick Start (Local)

**1. Clone and enter the project**
```bash
cd agro-connect
```

**2. Create your environment file**

Windows:
```cmd
copy .env.example .env
```

Linux/Mac:
```bash
cp .env.example .env
```

**3. Edit `.env` with your values**
```env
DB_USERNAME=agroconnect
DB_PASSWORD=your_strong_password

# Generate with: openssl rand -hex 32
JWT_SECRET=your_32_char_minimum_secret_key_here

CORS_ALLOWED_ORIGINS=http://localhost:3000

BOOTSTRAP_ADMIN_USERNAME=admin
BOOTSTRAP_ADMIN_PASSWORD=your_admin_password
```

For production, set `CORS_ALLOWED_ORIGINS=https://yourdomain.com` and use a long random admin password.

**4. Build and start**
```bash
docker-compose up -d --build
```

The first build takes a few minutes. Subsequent starts are fast.

**5. Open the app**
```
http://localhost:3000
```

Login with the admin credentials you set in `.env`.

### Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `DB_USERNAME` | Yes | PostgreSQL username |
| `DB_PASSWORD` | Yes | PostgreSQL password |
| `JWT_SECRET` | Yes | HMAC signing key (min 32 characters) |
| `CORS_ALLOWED_ORIGINS` | Yes | Browser-visible origin (e.g. `http://localhost:3000`) |
| `BOOTSTRAP_ADMIN_USERNAME` | Yes | First admin username (only used on first boot) |
| `BOOTSTRAP_ADMIN_PASSWORD` | Yes | First admin password (only used on first boot) |
| `JWT_EXPIRATION_MS` | No | Token lifetime in ms (default: 36000000 = 10 hours) |

### Common Commands

```bash
# Start in background
docker-compose up -d

# Stop (data is preserved)
docker-compose down

# Stop and delete all data
docker-compose down -v

# Rebuild after code changes
docker-compose up -d --build

# View logs
docker-compose logs -f
docker-compose logs -f backend
docker-compose logs -f frontend

# Check container status
docker-compose ps
```

### Deploying to a Server (Ubuntu VPS)

**1. Install Docker on the server**
```bash
curl -fsSL https://get.docker.com | sh
```

**2. Copy project files to the server**

From your local machine:
```bash
scp -r ./agro-connect user@your-server-ip:/app/agro-connect
```

**3. SSH into the server and start**
```bash
ssh user@your-server-ip
cd /app/agro-connect
cp .env.example .env
nano .env   # fill in production values
docker-compose up -d --build
```

**4. Set CORS to your real domain**

In `.env`:
```env
CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

**5. Put HTTPS in front of the app**

Terminate TLS at a reverse proxy or load balancer and forward traffic to the frontend container. Do not serve production traffic over plain HTTP.

**6. Back up PostgreSQL**

At minimum, schedule regular `pg_dump` backups from the database container or your managed PostgreSQL service.

From this repo on Windows PowerShell you can run:
```powershell
.\scripts\backup-db.ps1
```

### Notes

- Database data is stored in the `postgres_data` Docker volume — it persists across restarts and `docker-compose down`.
- The bootstrap admin is only created once (on first startup when no admin exists). After setup, clear `BOOTSTRAP_ADMIN_USERNAME` and `BOOTSTRAP_ADMIN_PASSWORD`.
- The backend actuator (health/metrics) runs on internal port `8082` and is not exposed outside the container.
- All containers have `restart: unless-stopped` — they come back automatically after a server reboot.

---

### Deploying to AWS ECS (Fargate)

#### Architecture

```
Internet → ALB :80 → ECS Fargate Task
                          ├── frontend container (Nginx :80)
                          │       └── /api/* proxied to localhost:8081
                          └── backend container (Spring Boot :8081)
                                      ↓
                              RDS PostgreSQL
```

Both frontend and backend run as **sidecars in the same Fargate task**. Nginx proxies `/api/` to `localhost:8081` since they share the same network namespace.

#### Prerequisites

- AWS CLI installed and configured (`aws configure`)
- Docker Desktop running
- An AWS account

#### Step 1 — Create ECR Repositories

```bash
aws ecr create-repository --repository-name agroconnect-backend --region YOUR_REGION
aws ecr create-repository --repository-name agroconnect-frontend --region YOUR_REGION
```

#### Step 2 — Build and Push Images

```bash
# Authenticate Docker with ECR
aws ecr get-login-password --region YOUR_REGION | docker login --username AWS --password-stdin YOUR_ACCOUNT_ID.dkr.ecr.YOUR_REGION.amazonaws.com

# Build and push backend
docker build -t agroconnect-backend ./backend
docker tag agroconnect-backend:latest YOUR_ACCOUNT_ID.dkr.ecr.YOUR_REGION.amazonaws.com/agroconnect-backend:latest
docker push YOUR_ACCOUNT_ID.dkr.ecr.YOUR_REGION.amazonaws.com/agroconnect-backend:latest

# Build and push frontend
docker build -t agroconnect-frontend ./frontend
docker tag agroconnect-frontend:latest YOUR_ACCOUNT_ID.dkr.ecr.YOUR_REGION.amazonaws.com/agroconnect-frontend:latest
docker push YOUR_ACCOUNT_ID.dkr.ecr.YOUR_REGION.amazonaws.com/agroconnect-frontend:latest
```

#### Step 3 — Create RDS PostgreSQL

In AWS Console → RDS → Create database:
- Engine: PostgreSQL 16
- Template: Free tier (for testing) or Production
- DB name: `agroconnect`
- Username/password: save these for Step 4
- VPC: same VPC as your ECS cluster
- Public access: No (keep it private)

Note the **endpoint URL** — you'll need it in Step 4.

#### Step 4 — Store Secrets in AWS Parameter Store

```bash
aws ssm put-parameter --name /agroconnect/DB_USERNAME              --value "your_db_user"      --type SecureString
aws ssm put-parameter --name /agroconnect/DB_PASSWORD              --value "your_db_password"  --type SecureString
aws ssm put-parameter --name /agroconnect/JWT_SECRET               --value "your_jwt_secret"   --type SecureString
aws ssm put-parameter --name /agroconnect/CORS_ALLOWED_ORIGINS     --value "http://your-alb-dns.amazonaws.com" --type SecureString
aws ssm put-parameter --name /agroconnect/BOOTSTRAP_ADMIN_USERNAME --value "admin"             --type SecureString
aws ssm put-parameter --name /agroconnect/BOOTSTRAP_ADMIN_PASSWORD --value "your_password"     --type SecureString
```

#### Step 5 — Create CloudWatch Log Groups

```bash
aws logs create-log-group --log-group-name /ecs/agroconnect-backend
aws logs create-log-group --log-group-name /ecs/agroconnect-frontend
```

#### Step 6 — Register the Task Definition

Edit `ecs-task-definition.json` and replace all `YOUR_ACCOUNT_ID`, `YOUR_REGION`, and `YOUR_RDS_ENDPOINT` placeholders with real values. Then:

```bash
aws ecs register-task-definition --cli-input-json file://ecs-task-definition.json
```

#### Step 7 — Create ECS Cluster

```bash
aws ecs create-cluster --cluster-name agroconnect-cluster
```

#### Step 8 — Create Application Load Balancer

In AWS Console → EC2 → Load Balancers → Create ALB:
- Scheme: Internet-facing
- Listener: HTTP port 80
- Target group: IP type, port 80
- Register no targets yet (ECS will do this)

Note the **ALB DNS name** and update `CORS_ALLOWED_ORIGINS` in Parameter Store to match.

#### Step 9 — Create ECS Service

In AWS Console → ECS → Clusters → agroconnect-cluster → Create Service:
- Launch type: Fargate
- Task definition: agroconnect (the one registered in Step 6)
- Desired tasks: 1
- VPC/subnets: same as RDS
- Security group: allow port 80 inbound
- Load balancer: select the ALB from Step 8, container: `frontend:80`

#### Step 10 — Access the App

```
http://YOUR_ALB_DNS_NAME
```

#### Updating After Code Changes

```bash
# Rebuild and push new images
docker build -t agroconnect-backend ./backend
docker tag agroconnect-backend:latest YOUR_ACCOUNT_ID.dkr.ecr.YOUR_REGION.amazonaws.com/agroconnect-backend:latest
docker push YOUR_ACCOUNT_ID.dkr.ecr.YOUR_REGION.amazonaws.com/agroconnect-backend:latest

# Force ECS to pull the new image
aws ecs update-service --cluster agroconnect-cluster --service agroconnect-service --force-new-deployment
```

---

### Common Mistakes to Avoid

#### Secrets & Configuration

| Mistake | What happens | Fix |
|---------|-------------|-----|
| Committing `.env` to Git | Credentials exposed publicly forever | `.env` is in `.gitignore` — never remove it |
| Using a short `JWT_SECRET` | App fails to start or tokens are easily cracked | Use at least 32 characters; generate with `openssl rand -hex 32` |
| Keeping example passwords in production | System is trivially compromised | Change every value in `.env` before going live |
| Setting `CORS_ALLOWED_ORIGINS=*` | Any website can make authenticated API calls on behalf of your users | Always set the exact origin, e.g. `https://yourdomain.com` |
| Wrong `CORS_ALLOWED_ORIGINS` for the environment | All API calls return 403 | Must match the browser-visible URL exactly, including port (e.g. `http://localhost:3000`) |

#### Docker & Build

| Mistake | What happens | Fix |
|---------|-------------|-----|
| Running `docker-compose up --build` without updating `package-lock.json` | Frontend build fails with `npm ci` sync error | Run `npm install` in `/frontend` first whenever you add/update packages |
| Port conflict (XAMPP, IIS, another app on port 80/3000/5432) | Container fails to start or wrong app opens | Change the port mapping in `docker-compose.yml`, e.g. `"3000:80"` |
| Running `docker-compose down -v` in production | **Deletes all database data permanently** | Use `docker-compose down` (without `-v`) to just stop containers |
| Not waiting for health checks on first boot | Frontend starts before backend is ready | Wait ~60 seconds after `docker-compose up` before opening the browser |
| Editing code without rebuilding | Changes not reflected in running containers | Always run `docker-compose up -d --build` after code changes |

#### Database

| Mistake | What happens | Fix |
|---------|-------------|-----|
| Changing `DB_USERNAME` or `DB_PASSWORD` after first run | PostgreSQL container fails to start (credentials mismatch with existing volume) | Run `docker-compose down -v` first to reset, or keep the original credentials |
| Modifying entity fields without a Flyway migration | App fails to start in prod (`ddl-auto=validate` rejects schema mismatch) | Always create a new `V2__...sql` migration file for any schema change |

#### Production Server

| Mistake | What happens | Fix |
|---------|-------------|-----|
| Exposing port 5432 publicly | Database accessible from the internet | Remove the `ports` entry for `db` in `docker-compose.yml` (already done) |
| Not setting up a firewall | All ports open to the internet | On Ubuntu: `ufw allow 22 && ufw allow 3000 && ufw enable` |
| Using HTTP instead of HTTPS in production | Passwords and tokens sent in plaintext | Put Nginx or Caddy in front with an SSL certificate (Let's Encrypt is free) |

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
