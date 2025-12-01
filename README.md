# Session Scheduler - Multi-Tenant Booking Platform

A comprehensive, multi-tenant session scheduling platform built with Spring Boot (Java) and React (TypeScript). This MVP allows businesses to manage appointments, classes, and bookings while providing their customers with a seamless booking experience.

## 🚀 Features Implemented

### Admin Portal
- ✅ Create, update, and delete tenants
- ✅ View all tenants with status and subscription information
- ✅ Tenant management dashboard

### Business Owner Portal
- ✅ Manage session types (create, update, delete)
- ✅ View and manage bookings
- ✅ Access booking link for customers
- ✅ View tenant information

### Customer Portal
- ✅ Browse available sessions for a business
- ✅ Book sessions with customer information
- ✅ Responsive booking interface

### Technical Features
- ✅ JWT-based authentication and authorization
- ✅ Multi-tenant architecture with data isolation
- ✅ RESTful API with Spring Boot
- ✅ H2 in-memory database (development)
- ✅ React frontend with TypeScript
- ✅ Role-based access control (Admin, Business, Customer)

## 📋 Prerequisites

Before running the application, ensure you have the following installed:

- **Java 17+** - [Download](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
- **Maven 3.6+** - [Download](https://maven.apache.org/download.cgi)
- **Node.js 18+** - [Download](https://nodejs.org/)
- **npm or yarn** - Comes with Node.js

## 🛠️ Installation & Setup

### Backend Setup

1. **Navigate to the backend directory:**
   ```bash
   cd Scheduler/backend
   ```

2. **Build the project:**
   ```bash
   mvn clean install
   ```

3. **Run the Spring Boot application:**
   ```bash
   mvn spring-boot:run
   ```

   The backend will start on **http://localhost:8080**

   **Expected Output:**
   ```
   Started BookingApplication in X.XXX seconds
   Created default admin user - username: admin, password: admin
   Created demo tenant: demo-yoga
   ```

4. **Verify the backend is running:**
   - Open your browser to http://localhost:8080/h2-console
   - JDBC URL: `jdbc:h2:mem:schedulerdb`
   - Username: `sa`
   - Password: (leave empty)

### Frontend Setup

1. **Open a new terminal and navigate to the frontend directory:**
   ```bash
   cd Scheduler/frontend
   ```

2. **Install dependencies:**
   ```bash
   npm install
   ```

3. **Start the development server:**
   ```bash
   npm run dev
   ```

   The frontend will start on **http://localhost:5173**

4. **Access the application:**
   - Open your browser to http://localhost:5173

## 🔑 Default Credentials

### Admin Account
- **Email:** `admin@localhost`
- **Password:** `admin`

After logging in as admin, you'll have access to:
- Create and manage tenants
- View all tenants in the system
- Delete tenants

### Demo Business Account
A demo tenant "demo-yoga" is automatically created. To access it:

1. Log in as admin
2. View the tenant list to find the business owner email
3. Default business owner credentials are created with:
   - Email: (same as tenant email)
   - Password: `changeme123`

## 📱 User Flows

### Admin Flow
1. Login with admin credentials (`admin@localhost` / `admin`) at http://localhost:5173/login
2. You'll be redirected to `/admin`
3. View the list of tenants
4. Click "Add Tenant" to create a new business
5. Fill in:
   - Business Name
   - Slug (URL identifier, e.g., "yoga-studio")
   - Email
   - Phone (optional)
   - Description (optional)
6. Click "Create Tenant"
7. A business owner account will be automatically created with default password `changeme123`

### Business Owner Flow
1. Login with business owner credentials
2. You'll be redirected to `/business`
3. View your booking link (share this with customers)
4. **Manage Session Types:**
   - Click "Add Session Type"
   - Fill in session details (name, duration, price, capacity)
   - Click "Create"
5. **View Bookings:**
   - Click the "Bookings" tab
   - View all upcoming bookings
   - Cancel bookings if needed

### Customer Flow
1. Visit the business booking page: `http://localhost:5173/book/{tenant-slug}`
   - Example: `http://localhost:5173/book/demo-yoga`
2. Browse available session types
3. Click "Book Now" on a session
4. Fill in your information:
   - First Name, Last Name
   - Email, Phone
   - Preferred Date & Time
   - Notes (optional)
5. Click "Confirm Booking"
6. You'll see a success message

## 🗂️ Project Structure

```
Scheduler/
├── backend/                          # Spring Boot Backend
│   ├── src/main/java/com/scheduler/booking/
│   │   ├── model/                   # JPA Entities
│   │   ├── repository/              # Spring Data Repositories
│   │   ├── service/                 # Business Logic
│   │   ├── controller/              # REST Controllers
│   │   ├── security/                # JWT & Security Config
│   │   ├── config/                  # App Configuration
│   │   ├── dto/                     # Data Transfer Objects
│   │   └── BookingApplication.java  # Main Application
│   ├── src/main/resources/
│   │   └── application.properties   # Configuration
│   └── pom.xml                      # Maven Dependencies
│
└── frontend/                        # React Frontend
    ├── src/
    │   ├── components/              # Reusable Components
    │   ├── pages/                   # Page Components
    │   │   ├── Login.tsx           # Login Page
    │   │   ├── AdminDashboard.tsx  # Admin Portal
    │   │   ├── BusinessDashboard.tsx # Business Portal
    │   │   └── CustomerPortal.tsx  # Customer Booking
    │   ├── services/                # API Services
    │   ├── context/                 # React Context (Auth)
    │   ├── types/                   # TypeScript Types
    │   ├── App.tsx                  # Main App Component
    │   └── main.tsx                 # Entry Point
    ├── package.json                 # Dependencies
    └── vite.config.ts               # Vite Configuration
```

## 🔌 API Endpoints

### Authentication
- `POST /api/auth/login` - Login (returns JWT token)

### Admin Endpoints (Requires ADMIN role)
- `GET /api/admin/tenants` - Get all tenants
- `GET /api/admin/tenants/{id}` - Get tenant by ID
- `POST /api/admin/tenants` - Create new tenant
- `PUT /api/admin/tenants/{id}` - Update tenant
- `DELETE /api/admin/tenants/{id}` - Delete tenant

### Business Endpoints (Requires BUSINESS role)
- `GET /api/business/tenant` - Get tenant info
- `GET /api/business/sessions` - Get session types
- `POST /api/business/sessions` - Create session type
- `PUT /api/business/sessions/{id}` - Update session type
- `DELETE /api/business/sessions/{id}` - Delete session type
- `GET /api/business/bookings` - Get bookings
- `DELETE /api/business/bookings/{id}` - Cancel booking

### Customer Endpoints (Public)
- `GET /api/customer/tenants/{slug}` - Get tenant by slug
- `GET /api/customer/tenants/{tenantId}/sessions` - Get available sessions
- `POST /api/customer/tenants/{tenantId}/bookings` - Create booking

## 🧪 Testing the Application

### Test Scenario 1: Admin Creates a Tenant
1. Login as admin (admin@localhost/admin)
2. Click "Add Tenant"
3. Create a tenant with slug "test-gym"
4. Note: A business owner account is automatically created

### Test Scenario 2: Business Owner Manages Sessions
1. Login with the business owner email and password `changeme123`
2. Create a session type (e.g., "Personal Training - 60 min, $50")
3. View the booking link
4. Switch to the "Bookings" tab

### Test Scenario 3: Customer Books a Session
1. Open a new incognito window
2. Go to `http://localhost:5173/book/test-gym`
3. Click "Book Now" on a session
4. Fill in customer details and select a date/time
5. Confirm booking
6. Switch back to the business owner view and refresh - you'll see the new booking

## 🔧 Configuration

### Backend Configuration
Edit `backend/src/main/resources/application.properties`:

```properties
# Server Port
server.port=8080

# Database (H2 for development)
spring.datasource.url=jdbc:h2:mem:schedulerdb

# JWT Secret (change in production!)
jwt.secret=your-secret-key-here
jwt.expiration=86400000

# CORS
cors.allowed-origins=http://localhost:3000,http://localhost:5173
```

### Frontend Configuration
Edit `frontend/vite.config.ts` to change the proxy or port:

```typescript
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080'
      }
    }
  }
})
```

## 🐛 Troubleshooting

### Backend Issues

**Issue: Port 8080 already in use**
```bash
# Kill the process using port 8080
# On Mac/Linux:
lsof -ti:8080 | xargs kill -9

# On Windows:
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

**Issue: Maven build fails**
```bash
# Clean and rebuild
mvn clean install -U
```

**Issue: Database errors**
- Check that H2 console is accessible at http://localhost:8080/h2-console
- Restart the backend application

### Frontend Issues

**Issue: Port 5173 already in use**
```bash
# Kill the process or change the port in vite.config.ts
```

**Issue: API calls failing (CORS errors)**
- Ensure backend is running on port 8080
- Check that CORS is configured correctly in SecurityConfig.java

**Issue: Dependencies not installing**
```bash
# Clear cache and reinstall
rm -rf node_modules package-lock.json
npm install
```

## 📦 Building for Production

### Backend
```bash
cd backend
mvn clean package
java -jar target/booking-platform-1.0.0.jar
```

### Frontend
```bash
cd frontend
npm run build
# Output will be in the 'dist' folder
```

## 🚧 Known Limitations (MVP)

- ✋ Payment processing is not implemented (Stripe integration ready but not connected)
- ✋ Email notifications are not sent (service layer is ready)
- ✋ No availability management (time slots are manually entered)
- ✋ No calendar integration
- ✋ Limited validation on booking times
- ✋ H2 in-memory database (data is lost on restart)

## 🔮 Next Steps for Production

1. **Database Migration:**
   - Replace H2 with PostgreSQL
   - Update `application.properties` with PostgreSQL configuration
   - Run database migrations

2. **Security Enhancements:**
   - Change JWT secret to environment variable
   - Implement refresh tokens
   - Add rate limiting
   - Enable HTTPS

3. **Feature Enhancements:**
   - Integrate Stripe for payment processing
   - Implement SendGrid for email notifications
   - Add availability management with time slots
   - Implement booking cancellation policies
   - Add customer portal with login

4. **DevOps:**
   - Set up CI/CD pipeline
   - Deploy to cloud (AWS/GCP/Azure)
   - Set up monitoring and logging
   - Configure auto-scaling

## 📝 License

This project is for demonstration purposes.

## 🤝 Support

For issues or questions:
1. Check the troubleshooting section
2. Review the API documentation
3. Check the H2 console for database issues

## 🎉 Success Indicators

You'll know the application is working correctly when:
- ✅ Backend starts without errors and shows "Started BookingApplication"
- ✅ Frontend loads at http://localhost:5173
- ✅ You can login as admin
- ✅ You can create a tenant
- ✅ You can login as business owner
- ✅ You can create session types
- ✅ Customers can view sessions at /book/{slug}
- ✅ Customers can complete bookings
- ✅ Business owners can see bookings in their dashboard

---

**Happy Scheduling! 📅**
