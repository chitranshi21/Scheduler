import { SignIn, SignUp, useUser } from '@clerk/clerk-react';
import { useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';

// Helper function to get redirect URL based on role
const getRedirectUrl = (role: string): string => {
  if (role === 'ADMIN') {
    return '/admin';
  } else if (role === 'BUSINESS') {
    return '/business';
  }
  return '/'; // CUSTOMER or no role
};

export default function Login() {
  const { isSignedIn, user, isLoaded } = useUser();
  const navigate = useNavigate();
  const [showSignUp, setShowSignUp] = useState(false);

  useEffect(() => {
    // Wait for Clerk to fully load before checking authentication
    if (!isLoaded) return;

    if (isSignedIn && user) {
      const userRole = user.publicMetadata?.role as string || 'CUSTOMER';
      const redirectUrl = getRedirectUrl(userRole);
      
      // Small delay to ensure Clerk state is fully updated
      const timer = setTimeout(() => {
        navigate(redirectUrl, { replace: true });
      }, 100);

      return () => clearTimeout(timer);
    }
  }, [isSignedIn, user, isLoaded, navigate]);

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      padding: '20px'
    }}>
      <div style={{
        maxWidth: '500px',
        width: '100%',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '24px'
      }}>
        <h1 style={{
          textAlign: 'center',
          color: 'white',
          fontSize: '32px',
          fontWeight: 'bold',
          textShadow: '2px 2px 4px rgba(0,0,0,0.2)'
        }}>
          Session Scheduler
        </h1>

        <div style={{
          width: '100%',
          display: 'flex',
          justifyContent: 'center'
        }}>
          {showSignUp ? (
            <SignUp
              routing="hash"
            />
          ) : (
            <SignIn
              routing="hash"
            />
          )}
        </div>

        <div style={{
          textAlign: 'center',
          color: 'white'
        }}>
          <button
            onClick={() => setShowSignUp(!showSignUp)}
            style={{
              background: 'transparent',
              border: 'none',
              color: 'white',
              textDecoration: 'underline',
              cursor: 'pointer',
              fontSize: '14px'
            }}
          >
            {showSignUp ? 'Already have an account? Sign in' : "Don't have an account? Sign up"}
          </button>
        </div>

        <div style={{
          marginTop: '16px',
          padding: '16px',
          background: 'rgba(255, 255, 255, 0.9)',
          borderRadius: '8px',
          width: '100%'
        }}>
          <p style={{ fontSize: '14px', color: '#374151', marginBottom: '8px', fontWeight: 'bold' }}>
            Note for Setup:
          </p>
          <p style={{ fontSize: '12px', color: '#6b7280', marginBottom: '8px' }}>
            After creating a user in Clerk, you need to set their role in the public metadata:
          </p>
          <ul style={{ fontSize: '12px', color: '#6b7280', paddingLeft: '20px' }}>
            <li>Go to Clerk Dashboard Users</li>
            <li>Select the user</li>
            <li>Add to public_metadata: {`{ "role": "ADMIN" }`} or {`{ "role": "BUSINESS" }`}</li>
          </ul>
        </div>
      </div>
    </div>
  );
}
