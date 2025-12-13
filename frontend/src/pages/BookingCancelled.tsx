import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

export default function BookingCancelled() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [countdown, setCountdown] = useState(5);
  const slug = searchParams.get('slug');

  useEffect(() => {
    const timer = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          // Redirect to business booking page if slug is available, otherwise home
          if (slug) {
            navigate(`/book/${slug}?payment=cancelled`);
          } else {
            navigate('/');
          }
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [navigate, slug]);

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
          background: '#fee2e2',
          borderRadius: '50%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          margin: '0 auto 24px',
          fontSize: '48px',
          color: '#ef4444'
        }}>
          ✕
        </div>

        <h1 style={{
          fontSize: '28px',
          fontWeight: '700',
          color: '#ef4444',
          margin: '0 0 12px 0'
        }}>
          Booking Cancelled
        </h1>

        <p style={{
          fontSize: '16px',
          color: '#6b7280',
          margin: '0 0 24px 0',
          lineHeight: '1.6'
        }}>
          Your payment was cancelled and no booking has been created.
          The time slot is still available if you'd like to try again.
        </p>

        <div style={{
          padding: '16px',
          background: '#fef3c7',
          borderRadius: '8px',
          marginBottom: '24px'
        }}>
          <p style={{
            fontSize: '14px',
            color: '#92400e',
            margin: 0
          }}>
            ⚠️ No charges were made to your card
          </p>
        </div>

        <button
          onClick={() => {
            if (slug) {
              navigate(`/book/${slug}?payment=cancelled`);
            } else {
              navigate('/');
            }
          }}
          className="button button-primary"
          style={{ width: '100%', marginBottom: '12px' }}
        >
          {slug ? 'Back to Booking Page' : 'Back to Home'}
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
