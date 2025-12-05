# Database Schema Documentation

## Overview
The application uses an H2 in-memory database for development. All tables use UUID as primary keys and include timestamp fields for auditing.

## Database Connection Information

### H2 Console Access
- **URL**: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:mem:schedulerdb`
- **Username**: `sa`
- **Password**: (empty string)
- **Driver Class**: `org.h2.Driver`

### Configuration Location
Database settings are configured in `/backend/src/main/resources/application.properties`

## Entity Relationship Diagram

```
┌─────────────┐       ┌──────────────┐       ┌──────────────┐
│   Tenants   │◄──────┤BusinessUsers │       │    Admins    │
└──────┬──────┘       └──────────────┘       └──────────────┘
       │
       ├────────────┐
       │            │
       ▼            ▼
┌─────────────┐ ┌──────────────┐            ┌──────────────┐
│SessionTypes │ │ BlockedSlots │            │  Customers   │
└──────┬──────┘ └──────────────┘            └──────┬───────┘
       │                                            │
       │           ┌──────────────┐                │
       └──────────►│   Bookings   │◄───────────────┘
                   └──────┬───────┘
                          │
                          ▼
                   ┌──────────────┐
                   │   Payments   │
                   └──────────────┘
```

## Database Tables

### 1. tenants
Stores information about businesses using the platform.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY | Unique identifier |
| name | VARCHAR | NOT NULL | Business name |
| slug | VARCHAR | UNIQUE, NOT NULL | URL-friendly identifier |
| email | VARCHAR | NOT NULL | Business contact email |
| phone | VARCHAR | NULL | Business phone number |
| logo_url | VARCHAR | NULL | URL to business logo |
| description | TEXT | NULL | Business description |
| brand_colors | JSON | NULL | Custom branding colors |
| custom_domain | VARCHAR | NULL | Custom domain for booking page |
| status | VARCHAR | NOT NULL | Status (ACTIVE, INACTIVE) |
| subscription_tier | VARCHAR | NOT NULL | Subscription level (BASIC, PRO, ENTERPRISE) |
| subscription_expires_at | TIMESTAMP | NULL | Subscription expiration |
| created_at | TIMESTAMP | NOT NULL | Record creation time |
| updated_at | TIMESTAMP | NOT NULL | Last update time |

**Relationships:**
- One-to-many with `business_users`
- One-to-many with `session_types`
- One-to-many with `bookings`
- One-to-many with `blocked_slots`
- One-to-many with `payments`

### 2. admins
Platform administrators who can manage tenants.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY | Unique identifier |
| email | VARCHAR | UNIQUE, NOT NULL | Admin email |
| clerk_user_id | VARCHAR | UNIQUE | Clerk authentication ID |
| first_name | VARCHAR | NULL | Admin first name |
| last_name | VARCHAR | NULL | Admin last name |
| role | VARCHAR | NOT NULL | Role (default: ADMIN) |
| is_active | BOOLEAN | NOT NULL | Account status |
| last_login_at | TIMESTAMP | NULL | Last login timestamp |
| created_at | TIMESTAMP | NOT NULL | Record creation time |
| updated_at | TIMESTAMP | NOT NULL | Last update time |

**Authentication:**
- `clerk_user_id` links to Clerk authentication
- Must have `{"role": "ADMIN"}` in Clerk public_metadata

### 3. business_users
Users who manage a specific tenant/business.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY | Unique identifier |
| tenant_id | UUID | NOT NULL, FK | Reference to tenant |
| email | VARCHAR | NOT NULL | Business user email |
| clerk_user_id | VARCHAR | UNIQUE | Clerk authentication ID |
| first_name | VARCHAR | NULL | User first name |
| last_name | VARCHAR | NULL | User last name |
| role | VARCHAR | NOT NULL | Role within business (OWNER, MANAGER) |
| is_active | BOOLEAN | NOT NULL | Account status |
| last_login_at | TIMESTAMP | NULL | Last login timestamp |
| created_at | TIMESTAMP | NOT NULL | Record creation time |
| updated_at | TIMESTAMP | NOT NULL | Last update time |

**Constraints:**
- UNIQUE constraint on (tenant_id, email)

**Relationships:**
- Many-to-one with `tenants`

**Authentication:**
- `clerk_user_id` links to Clerk authentication
- Must have `{"role": "BUSINESS", "tenant_id": "..."}` in Clerk public_metadata
- JIT (Just-In-Time) linking: If clerk_user_id not found, system attempts to link by email

### 4. customers
End users who book sessions.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY | Unique identifier |
| email | VARCHAR | UNIQUE, NOT NULL | Customer email |
| clerk_user_id | VARCHAR | UNIQUE | Clerk authentication ID |
| first_name | VARCHAR | NOT NULL | Customer first name |
| last_name | VARCHAR | NOT NULL | Customer last name |
| phone | VARCHAR | NULL | Customer phone number |
| timezone | VARCHAR | NULL | Customer timezone (default: UTC) |
| created_at | TIMESTAMP | NOT NULL | Record creation time |
| updated_at | TIMESTAMP | NOT NULL | Last update time |

**Relationships:**
- One-to-many with `bookings`
- One-to-many with `payments`

**Authentication:**
- Can book with or without authentication
- If no `clerk_user_id`, customer record created with email only

### 5. session_types
Types of sessions/services offered by a business.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY | Unique identifier |
| tenant_id | UUID | NOT NULL, FK | Reference to tenant |
| name | VARCHAR | NOT NULL | Session type name |
| description | TEXT | NULL | Detailed description |
| duration_minutes | INTEGER | NOT NULL | Session duration in minutes |
| price | DECIMAL(10,2) | NOT NULL | Session price |
| currency | VARCHAR | NOT NULL | Currency code (default: USD) |
| capacity | INTEGER | NULL | Max participants (default: 1) |
| category | VARCHAR | NULL | Session category |
| color | VARCHAR | NULL | Display color for calendar |
| is_active | BOOLEAN | NOT NULL | Availability status |
| cancellation_policy | TEXT | NULL | Cancellation policy text |
| created_at | TIMESTAMP | NOT NULL | Record creation time |
| updated_at | TIMESTAMP | NOT NULL | Last update time |

**Relationships:**
- Many-to-one with `tenants`
- One-to-many with `bookings`

### 6. bookings
Customer reservations for sessions.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY | Unique identifier |
| tenant_id | UUID | NOT NULL, FK | Reference to tenant |
| customer_id | UUID | NOT NULL, FK | Reference to customer |
| session_type_id | UUID | NOT NULL, FK | Reference to session type |
| start_time | TIMESTAMP | NOT NULL | Booking start time |
| end_time | TIMESTAMP | NOT NULL | Booking end time |
| status | VARCHAR | NOT NULL | Status (PENDING, CONFIRMED, CANCELLED) |
| participants | INTEGER | NULL | Number of participants (default: 1) |
| notes | TEXT | NULL | Additional notes |
| customer_timezone | VARCHAR | NULL | Customer's timezone |
| cancellation_reason | VARCHAR | NULL | Reason for cancellation |
| cancelled_at | TIMESTAMP | NULL | Cancellation timestamp |
| cancelled_by | VARCHAR | NULL | Who cancelled the booking |
| created_at | TIMESTAMP | NOT NULL | Record creation time |
| updated_at | TIMESTAMP | NOT NULL | Last update time |

**Relationships:**
- Many-to-one with `tenants`
- Many-to-one with `customers`
- Many-to-one with `session_types`
- One-to-many with `payments`

**Business Logic:**
- `end_time` is calculated as `start_time + session_type.duration_minutes`
- System checks for conflicts with `blocked_slots` before creating booking
- Confirmation number generated as `BK-{first-8-chars-of-UUID}`

### 7. blocked_slots
Time slots blocked by business users (unavailable for booking).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY | Unique identifier |
| tenant_id | UUID | NOT NULL, FK | Reference to tenant |
| start_time | TIMESTAMP | NOT NULL | Block start time |
| end_time | TIMESTAMP | NOT NULL | Block end time |
| reason | VARCHAR | NULL | Reason for blocking |
| created_by | VARCHAR | NULL | Clerk User ID of creator |
| created_at | TIMESTAMP | NOT NULL | Record creation time |
| updated_at | TIMESTAMP | NOT NULL | Last update time |

**Relationships:**
- Many-to-one with `tenants`

**Business Logic:**
- System prevents bookings that conflict with blocked slots
- Conflict detection checks if booking time overlaps with any blocked slot
- Query uses: `(slot_start <= booking_start AND slot_end > booking_start) OR (slot_start < booking_end AND slot_end >= booking_end) OR (slot_start >= booking_start AND slot_end <= booking_end)`

### 8. payments
Payment records for bookings.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY | Unique identifier |
| booking_id | UUID | NOT NULL, FK | Reference to booking |
| tenant_id | UUID | NOT NULL, FK | Reference to tenant |
| customer_id | UUID | NOT NULL, FK | Reference to customer |
| amount | DECIMAL(10,2) | NOT NULL | Payment amount |
| currency | VARCHAR | NOT NULL | Currency code (default: USD) |
| status | VARCHAR | NOT NULL | Status (PENDING, COMPLETED, FAILED, REFUNDED) |
| payment_method | VARCHAR | NULL | Payment method used |
| stripe_payment_intent_id | VARCHAR | NULL | Stripe payment intent ID |
| stripe_charge_id | VARCHAR | NULL | Stripe charge ID |
| refund_amount | DECIMAL(10,2) | NULL | Amount refunded (default: 0) |
| refunded_at | TIMESTAMP | NULL | Refund timestamp |
| failure_reason | TEXT | NULL | Reason for payment failure |
| metadata | JSON | NULL | Additional payment metadata |
| created_at | TIMESTAMP | NOT NULL | Record creation time |
| updated_at | TIMESTAMP | NOT NULL | Last update time |

**Relationships:**
- Many-to-one with `bookings`
- Many-to-one with `tenants`
- Many-to-one with `customers`

## Indexes

The following indexes are automatically created:
- Primary key indexes on all `id` columns
- Unique indexes on:
  - `tenants.slug`
  - `admins.email`
  - `admins.clerk_user_id`
  - `business_users.clerk_user_id`
  - `business_users.(tenant_id, email)` (composite)
  - `customers.email`
  - `customers.clerk_user_id`

## Important Queries

### Find conflicting blocked slots
```sql
SELECT * FROM blocked_slots
WHERE tenant_id = ?
  AND ((start_time <= ? AND end_time > ?)
    OR (start_time < ? AND end_time >= ?)
    OR (start_time >= ? AND end_time <= ?))
```

### Get upcoming bookings for a tenant
```sql
SELECT * FROM bookings
WHERE tenant_id = ?
  AND start_time > NOW()
ORDER BY start_time ASC
```

### Find business user by Clerk ID
```sql
SELECT * FROM business_users
WHERE clerk_user_id = ?
```

### Find or create customer by email
```sql
SELECT * FROM customers WHERE email = ?
-- If not found, INSERT new customer
```

## Data Initialization

The system auto-creates demo data on startup via `DataInitializer.java`:

1. **Demo Tenant**: "demo-yoga" with slug "demo-yoga"
2. **Demo Business User**: Linked to Clerk user `user_36O5AICSwOtpNTGrRyG8XgYV8tz`
3. **Demo Session Types**: Various yoga classes

## Migration Notes

### From Development to Production

1. **Change Database**: Update `application.properties` to use PostgreSQL/MySQL instead of H2
2. **Update ddl-auto**: Change from `create-drop` to `validate` or `update`
3. **Add Connection Pool**: Configure HikariCP settings
4. **Enable SSL**: Add SSL configuration for production database

Example production configuration:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/scheduler
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
```

## Backup and Restore

For H2 in-memory database:
- Data is lost on application restart
- Not suitable for production use

For production database:
- Use database-specific backup tools (pg_dump for PostgreSQL, mysqldump for MySQL)
- Implement regular automated backups
- Test restore procedures regularly

## Security Considerations

1. **Database Credentials**: Never commit real credentials to version control
2. **API Keys**: Clerk secret keys must be kept secure
3. **SQL Injection**: All queries use parameterized statements via JPA
4. **Access Control**: Application enforces tenant isolation at the service layer
5. **Encryption**: Consider encrypting sensitive fields (customer emails, payment info) in production
