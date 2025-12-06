# Liquibase Database Migration Guide

## Overview

This project uses Liquibase for database schema versioning and migration management. Liquibase provides a database-independent way to track, version, and deploy database changes.

## Directory Structure

```
src/main/resources/
└── db/
    └── changelog/
        ├── db.changelog-master.yaml          # Master changelog file
        └── changes/
            └── v1.0.0-initial-schema.yaml    # Initial schema definition
```

## Configuration

### Application Properties

Located in `src/main/resources/application.properties`:

```properties
# JPA/Hibernate - DDL is managed by Liquibase
spring.jpa.hibernate.ddl-auto=none

# Liquibase Configuration
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml
spring.liquibase.enabled=true
spring.liquibase.drop-first=false
```

### Key Settings:

- **`spring.jpa.hibernate.ddl-auto=none`**: Disables Hibernate's automatic schema generation. Liquibase handles all schema management.
- **`spring.liquibase.change-log`**: Points to the master changelog file.
- **`spring.liquibase.enabled=true`**: Enables Liquibase on application startup.
- **`spring.liquibase.drop-first=false`**: Prevents dropping the entire database on startup (use with caution in dev environments).

## Liquibase Tracking Tables

Liquibase creates two tracking tables in your database:

1. **`DATABASECHANGELOG`**: Records which changesets have been executed
2. **`DATABASECHANGELOGLOCK`**: Prevents concurrent migrations

**Do not manually modify these tables.**

## Step-by-Step Migration Process

### 1. Creating a New Migration

When you need to make database changes (add table, modify column, add index, etc.):

#### Step 1: Create a new changelog file

Create a new YAML file in `src/main/resources/db/changelog/changes/`:

```bash
# File naming convention: vX.Y.Z-description.yaml
# Example: v1.1.0-add-notifications-table.yaml
```

#### Step 2: Write the changeset

Example changeset for adding a new table:

```yaml
databaseChangeLog:
  - changeSet:
      id: 10-create-notifications-table
      author: your-name
      changes:
        - createTable:
            tableName: notifications
            columns:
              - column:
                  name: id
                  type: UUID
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: user_id
                  type: UUID
                  constraints:
                    nullable: false
              - column:
                  name: message
                  type: TEXT
                  constraints:
                    nullable: false
              - column:
                  name: is_read
                  type: BOOLEAN
                  defaultValueBoolean: false
              - column:
                  name: created_at
                  type: TIMESTAMP
```

#### Step 3: Include in master changelog

Add the new changelog to `db.changelog-master.yaml`:

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/changes/v1.0.0-initial-schema.yaml
  - include:
      file: db/changelog/changes/v1.1.0-add-notifications-table.yaml  # New file
```

#### Step 4: Create corresponding JPA Entity

Create or update the Java entity class to match your schema.

#### Step 5: Test the migration

```bash
# Start the application
mvn spring-boot:run

# Check logs for Liquibase execution
# Look for: "Liquibase: Successfully released change log lock"
```

### 2. Rolling Back a Migration

Liquibase supports rollback with explicit rollback definitions:

```yaml
databaseChangeLog:
  - changeSet:
      id: 10-create-notifications-table
      author: your-name
      changes:
        - createTable:
            tableName: notifications
            # ... columns ...
      rollback:
        - dropTable:
            tableName: notifications
```

To rollback programmatically, you can use Liquibase Maven plugin:

```bash
# Rollback last changeset
mvn liquibase:rollback -Dliquibase.rollbackCount=1

# Rollback to specific tag
mvn liquibase:rollback -Dliquibase.rollbackTag=v1.0.0
```

### 3. Modifying Existing Tables

**Important**: Never modify an already-executed changeset. Always create a new changeset.

#### Example: Adding a new column

```yaml
databaseChangeLog:
  - changeSet:
      id: 11-add-avatar-url-to-users
      author: your-name
      changes:
        - addColumn:
            tableName: customers
            columns:
              - column:
                  name: avatar_url
                  type: VARCHAR(512)
      rollback:
        - dropColumn:
            tableName: customers
            columnName: avatar_url
```

#### Example: Modifying a column

```yaml
databaseChangeLog:
  - changeSet:
      id: 12-modify-customer-phone-length
      author: your-name
      changes:
        - modifyDataType:
            tableName: customers
            columnName: phone
            newDataType: VARCHAR(100)
```

#### Example: Adding an index

```yaml
databaseChangeLog:
  - changeSet:
      id: 13-add-index-bookings-status
      author: your-name
      changes:
        - createIndex:
            tableName: bookings
            indexName: idx_bookings_status
            columns:
              - column:
                  name: status
      rollback:
        - dropIndex:
            tableName: bookings
            indexName: idx_bookings_status
```

### 4. Data Migrations

For inserting, updating, or deleting data:

```yaml
databaseChangeLog:
  - changeSet:
      id: 14-insert-default-session-types
      author: your-name
      changes:
        - insert:
            tableName: session_types
            columns:
              - column:
                  name: id
                  valueComputed: "RANDOM_UUID()"
              - column:
                  name: tenant_id
                  value: "some-tenant-uuid"
              - column:
                  name: name
                  value: "Standard Consultation"
              - column:
                  name: duration_minutes
                  valueNumeric: 30
              - column:
                  name: price
                  valueNumeric: 50.00
              - column:
                  name: currency
                  value: "USD"
```

### 5. Environment-Specific Migrations

You can conditionally execute changesets based on environment:

```yaml
databaseChangeLog:
  - changeSet:
      id: 15-dev-test-data
      author: your-name
      context: dev
      changes:
        - insert:
            tableName: tenants
            columns:
              # ... test data columns
```

Then in `application.properties`:

```properties
# Only run dev context changesets in development
spring.liquibase.contexts=dev
```

## Common Liquibase Operations

### Validate Changesets Without Applying

```bash
mvn liquibase:status
```

### Generate SQL for Review (Without Executing)

```bash
mvn liquibase:updateSQL
```

### Clear Checksums (Use when changesets were modified before execution)

```bash
mvn liquibase:clearCheckSums
```

### Tag Database State

```bash
mvn liquibase:tag -Dliquibase.tag=v1.0.0
```

## Best Practices

### 1. Changeset IDs
- Use sequential numbering: `1-create-users-table`, `2-add-email-index`, etc.
- Make IDs descriptive and unique
- Never reuse changeset IDs

### 2. Never Modify Executed Changesets
- Once a changeset runs in production, it's immutable
- Create new changesets to alter previous changes
- Liquibase tracks checksums; modifications will cause errors

### 3. Always Include Rollback
- Define explicit rollback strategies when possible
- Test rollbacks in development
- Some operations (like data changes) may not be automatically reversible

### 4. Use Descriptive Authors
- Use your actual name or team identifier
- Helps track who made which changes

### 5. Test Migrations Thoroughly
- Test on empty database (fresh install)
- Test on existing database (upgrade path)
- Test rollbacks before deploying

### 6. Version Control
- Commit all changelog files to Git
- Never modify changesets that are in version control
- Review changelog changes in pull requests

### 7. Database-Agnostic Types
- Use Liquibase's generic types (VARCHAR, INTEGER, TIMESTAMP)
- Liquibase translates to database-specific types
- Improves portability across databases

## Switching Databases

Liquibase makes switching databases easy. Update `application.properties`:

### For PostgreSQL:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/schedulerdb
spring.datasource.driverClassName=org.postgresql.Driver
spring.datasource.username=postgres
spring.datasource.password=yourpassword
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

Add PostgreSQL dependency to `pom.xml`:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### For MySQL:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/schedulerdb
spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
```

Add MySQL dependency to `pom.xml`:

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

**No changes needed to Liquibase changelogs!**

## Troubleshooting

### Error: "Checksum mismatch"

**Cause**: A previously executed changeset was modified.

**Solution**:
```bash
# Clear checksums (use with caution)
mvn liquibase:clearCheckSums

# Or mark changeset as run
mvn liquibase:changelogSync
```

### Error: "Lock held by another process"

**Cause**: Previous migration didn't complete, lock wasn't released.

**Solution**:
```bash
# Release lock
mvn liquibase:releaseLocks
```

Or manually delete the lock from database:
```sql
DELETE FROM DATABASECHANGELOGLOCK WHERE ID = 1;
```

### Migration Fails Midway

**Solution**:
1. Check the `DATABASECHANGELOG` table to see which changesets succeeded
2. Fix the failing changeset
3. Restart the application (Liquibase will continue from where it failed)

## Production Deployment Checklist

- [ ] All changesets tested in staging environment
- [ ] Rollback procedures documented and tested
- [ ] Database backup created before migration
- [ ] Downtime window communicated (if needed)
- [ ] Rollback plan ready in case of issues
- [ ] Monitor application logs during migration
- [ ] Verify application functionality after migration

## Additional Resources

- [Liquibase Documentation](https://docs.liquibase.com/)
- [Liquibase Best Practices](https://www.liquibase.org/get-started/best-practices)
- [Spring Boot Liquibase Integration](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.liquibase)

## Current Schema Version

**Version**: 1.0.0
**Tables**: 8 (tenants, admins, business_users, customers, session_types, bookings, blocked_slots, payments)
**Last Updated**: 2025-12-06
