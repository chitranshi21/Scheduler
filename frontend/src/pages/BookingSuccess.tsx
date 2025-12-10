import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

export default function BookingSuccess() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [countdown, setCountdown] = useState(10);
  const sessionId = searchParams.get('session_id');

  useEffect(() => {
    const timer = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          navigate('/');
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [navigate]);

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: '#f9fafb',
      padding: '20px'
    }}>
      <div style={{
        maxWidth: '500px',
        width: '100%',
        background: 'white',
        borderRadius: '12px',
        boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
        padding: '48px 32px',
        textAlign: 'center'
      }}>
        <div style={{
          width: '80px',
          height: '80px',
          background: '#d1fae5',
          borderRadius: '50%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          margin: '0 auto 24px',
          fontSize: '48px',
          color: '#10b981'
        }}>
          ✓
        </div>

        <h1 style={{
          fontSize: '28px',
          fontWeight: '700',
          color: '#10b981',
          margin: '0 0 12px 0'
        }}>
          Payment Successful!
        </h1>

        <p style={{
          fontSize: '16px',
          color: '#6b7280',
          margin: '0 0 24px 0',
          lineHeight: '1.6'
        }}>
          Your booking has been confirmed and you will receive a confirmation email shortly
          with all the details including a calendar invite.
        </p>

        {sessionId && (
          <div style={{
            background: '#f3f4f6',
            padding: '12px',
            borderRadius: '8px',
            marginBottom: '24px'
          }}>
            <p style={{
              fontSize: '12px',
              color: '#6b7280',
              margin: '0 0 4px 0'
            }}>
              Transaction ID
            </p>
            <p style={{
              fontSize: '14px',
              fontFamily: 'monospace',
              color: '#1f2937',
              margin: 0,
              wordBreak: 'break-all'
            }}>
              {sessionId}
            </p>
          </div>
        )}

        <div style={{
          padding: '16px',
          background: '#eff6ff',
          borderRadius: '8px',
          marginBottom: '24px'
        }}>
          <p style={{
            fontSize: '14px',
            color: '#1e40af',
            margin: 0
          }}>
            📧 Check your email for booking details and calendar invite
          </p>
        </div>

        <button
          onClick={() => navigate('/')}
          className="button button-primary"
          style={{ width: '100%', marginBottom: '12px' }}
        >
          Back to Home
        </button>

        <p style={{
          fontSize: '13px',
          color: '#9ca3af',
          margin: 0
        }}>
          Redirecting in {countdown} seconds...
        </p>
      </div>
    </div>
  );
}
