import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useUser, useAuth as useClerkAuth } from '@clerk/clerk-react';
import { useEffect } from 'react';
import { setupApiAuth } from './services/api';
import Home from './pages/Home';
import Login from './pages/Login';
import AdminDashboard from './pages/AdminDashboard';
import BusinessDashboard from './pages/BusinessDashboard';
import CustomerPortal from './pages/CustomerPortal';
import BookingSuccess from './pages/BookingSuccess';
import BookingCancelled from './pages/BookingCancelled';
import './App.css';

const ProtectedRoute = ({ children, allowedRoles }: { children: React.ReactNode; allowedRoles: string[] }) => {
  const { user } = useUser();

  if (!user) {
    return <Navigate to="/login" />;
  }

  const userRole = user.publicMetadata?.role as string || 'CUSTOMER';

  if (!allowedRoles.includes(userRole)) {
    return <Navigate to="/login" />;
  }

  return <>{children}</>;
};

function AppRoutes() {
  const { user, isLoaded } = useUser();
  const { getToken } = useClerkAuth();
  const userRole = user?.publicMetadata?.role as string || 'CUSTOMER';

  // Setup API authentication with Clerk token
  useEffect(() => {
    setupApiAuth(getToken);
  }, [getToken]);

  // Show loading while Clerk is initializing
  if (!isLoaded) {
    return <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>Loading...</div>;
  }

  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/login" element={<Login />} />
      <Route
        path="/admin/*"
        element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <AdminDashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/business/*"
        element={
          <ProtectedRoute allowedRoles={['BUSINESS']}>
            <BusinessDashboard />
          </ProtectedRoute>
        }
      />
      <Route path="/book/:slug" element={<CustomerPortal />} />
      <Route path="/book/:slug/:sessionId" element={<CustomerPortal />} />
      <Route path="/booking-success" element={<BookingSuccess />} />
      <Route path="/booking-cancelled" element={<BookingCancelled />} />
    </Routes>
  );
}

function App() {
  return (
    <Router>
      <AppRoutes />
    </Router>
  );
}

export default App;
