import { useState, useEffect } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { customerAPI, stripeAPI } from '../services/api';
import type { Tenant, SessionType } from '../types';
import BookingCalendar from '../components/BookingCalendar';
import { getTimezoneDifferenceMessage } from '../utils/timezone';

export default function CustomerPortal() {
  const { slug, sessionId } = useParams<{ slug: string; sessionId?: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [tenant, setTenant] = useState<Tenant | null>(null);
  const [sessions, setSessions] = useState<SessionType[]>([]);
  const [selectedSession, setSelectedSession] = useState<SessionType | null>(null);
  const [showCustomerForm, setShowCustomerForm] = useState(false);
  const [bookingData, setBookingData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    startTime: '',
    notes: ''
  });

  useEffect(() => {
    if (slug) {
      loadTenantData();
    }
  }, [slug]);

  // Show payment status message if present in URL
  useEffect(() => {
    const paymentStatus = searchParams.get('payment');
    if (paymentStatus === 'success') {
      // Show success message (you can customize this)
      setTimeout(() => {
        alert('Payment successful! Your booking has been confirmed. Check your email for details.');
        // Remove the query parameter
        navigate(`/book/${slug}`, { replace: true });
      }, 100);
    } else if (paymentStatus === 'cancelled') {
      // Show cancelled message
      setTimeout(() => {
        alert('Payment was cancelled. No charges were made.');
        // Remove the query parameter
        navigate(`/book/${slug}`, { replace: true });
      }, 100);
    }
  }, [searchParams, slug, navigate]);

  useEffect(() => {
    // Load selected session when sessionId changes
    if (sessionId && sessions.length > 0) {
      const session = sessions.find(s => s.id === sessionId);
      if (session) {
        setSelectedSession(session);
      }
    } else if (!sessionId) {
      setSelectedSession(null);
      setShowCustomerForm(false);
      setBookingData({
        firstName: '',
        lastName: '',
        email: '',
        phone: '',
        startTime: '',
        notes: ''
      });
    }
  }, [sessionId, sessions]);

  const loadTenantData = async () => {
    try {
      console.log('Loading tenant data for slug:', slug);
      const tenantRes = await customerAPI.getTenantBySlug(slug!);
      console.log('Tenant loaded:', tenantRes.data);
      setTenant(tenantRes.data);

      if (tenantRes.data?.id) {
        console.log('Loading sessions for tenant:', tenantRes.data.id);
        const sessionsRes = await customerAPI.getSessionTypes(tenantRes.data.id);
        console.log('Sessions loaded:', sessionsRes.data);
        setSessions(Array.isArray(sessionsRes.data) ? sessionsRes.data : []);
      } else {
        console.error('Tenant ID is missing');
        setSessions([]);
      }
    } catch (error: any) {
      console.error('Failed to load tenant data:', error);
      console.error('Error details:', {
        message: error.message,
        response: error.response?.data,
        status: error.response?.status
      });
      // Set empty arrays to prevent UI errors
      setSessions([]);
    }
  };

  const handleSelectSession = (session: SessionType) => {
    // Navigate to calendar view
    navigate(`/book/${slug}/${session.id}`);
  };

  const handleTimeSlotSelect = (datetime: string) => {
    setBookingData({ ...bookingData, startTime: datetime });
    setShowCustomerForm(true);
  };

  const handleBackToSessions = () => {
    navigate(`/book/${slug}`);
  };

  const handleBackToCalendar = () => {
    setShowCustomerForm(false);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedSession || !tenant) return;

    try {
      // Convert startTime to epoch timestamp (milliseconds)
      const startTimeEpoch = new Date(bookingData.startTime).getTime();

      console.log('Creating booking with epoch timestamp:', {
        startTime: bookingData.startTime,
        startTimeEpoch,
        sessionTypeId: selectedSession.id
      });

      // Create the booking first
      const bookingResponse = await customerAPI.createBooking(tenant.id, {
        sessionTypeId: selectedSession.id,
        startTime: startTimeEpoch,
        firstName: bookingData.firstName,
        lastName: bookingData.lastName,
        email: bookingData.email,
        phone: bookingData.phone,
        notes: bookingData.notes,
        participants: 1
      });

      console.log('Booking created:', bookingResponse.data);

      // Create Stripe checkout session and redirect to payment
      const checkoutResponse = await stripeAPI.createCheckoutSession(bookingResponse.data.id, slug!);
      console.log('Stripe checkout session created:', checkoutResponse.data);

      // Redirect to Stripe checkout
      window.location.href = checkoutResponse.data.checkoutUrl;

    } catch (error: any) {
      console.error('Failed to create booking:', error);
      const errorMessage = error.response?.data?.message || error.message || 'Failed to create booking. Please try again.';
      alert(errorMessage);
    }
  };

  // If sessionId is in URL, show calendar view (full page)
  if (sessionId && selectedSession) {
    return (
      <div style={{ minHeight: '100vh', background: '#f9fafb' }}>
        {/* Header with back button */}
        <div style={{
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          padding: '24px',
          boxShadow: '0 4px 6px rgba(0,0,0,0.1)'
        }}>
          <div className="container">
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '16px',
              marginBottom: '16px'
            }}>
              <button
                onClick={handleBackToSessions}
                style={{
                  background: 'rgba(255, 255, 255, 0.2)',
                  border: 'none',
                  borderRadius: '8px',
                  padding: '8px 16px',
                  color: 'white',
                  cursor: 'pointer',
                  fontSize: '14px',
                  fontWeight: '500',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  transition: 'all 0.2s'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = 'rgba(255, 255, 255, 0.3)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = 'rgba(255, 255, 255, 0.2)';
                }}
              >
                <span>←</span> Back to Sessions
              </button>
            </div>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '24px',
              flexWrap: 'wrap'
            }}>
              {tenant?.logoUrl && (
                <div style={{
                  width: '80px',
                  height: '80px',
                  borderRadius: '50%',
                  overflow: 'hidden',
                  background: 'white',
                  padding: '6px',
                  boxShadow: '0 4px 12px rgba(0,0,0,0.15)'
                }}>
                  <img
                    src={tenant.logoUrl}
                    alt={`${tenant.name} logo`}
                    style={{
                      width: '100%',
                      height: '100%',
                      objectFit: 'cover',
                      borderRadius: '50%'
                    }}
                    onError={(e) => {
                      (e.target as HTMLImageElement).style.display = 'none';
                    }}
                  />
                </div>
              )}
              <div>
                <h1 style={{
                  color: 'white',
                  fontSize: '28px',
                  fontWeight: '700',
                  margin: '0 0 8px 0',
                  textShadow: '0 2px 4px rgba(0,0,0,0.1)'
                }}>
                  {selectedSession.name}
                </h1>
                <p style={{
                  color: 'rgba(255, 255, 255, 0.95)',
                  fontSize: '16px',
                  margin: 0,
                  textShadow: '0 1px 2px rgba(0,0,0,0.1)'
                }}>
                  ${selectedSession.price} • {selectedSession.durationMinutes} minutes
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Calendar View */}
        {!showCustomerForm ? (
          <div className="container" style={{ paddingTop: '48px', paddingBottom: '48px' }}>
            <BookingCalendar
              sessionDurationMinutes={selectedSession.durationMinutes}
              onSelectSlot={handleTimeSlotSelect}
              tenantId={tenant?.id || ''}
              sessionTypeId={selectedSession.id}
            />
          </div>
        ) : (
          <div className="container" style={{ paddingTop: '48px', paddingBottom: '48px', maxWidth: '600px' }}>
            <div className="card" style={{ margin: 0 }}>
              <div style={{
                padding: '16px 24px',
                background: '#f9fafb',
                borderRadius: '8px',
                marginBottom: '24px',
                border: '1px solid #e5e7eb'
              }}>
                <p style={{ margin: 0, fontSize: '14px', color: '#6b7280', marginBottom: '8px' }}>
                  <strong>Selected Time:</strong>
                </p>
                <p style={{ margin: 0, fontSize: '16px', color: '#1f2937', fontWeight: '500' }}>
                  {new Date(bookingData.startTime).toLocaleString('en-US', {
                    weekday: 'long',
                    month: 'long',
                    day: 'numeric',
                    hour: 'numeric',
                    minute: '2-digit'
                  })}
                </p>
                <button
                  type="button"
                  onClick={handleBackToCalendar}
                  style={{
                    background: 'none',
                    border: 'none',
                    color: '#4f46e5',
                    fontSize: '14px',
                    cursor: 'pointer',
                    padding: '8px 0',
                    marginTop: '8px',
                    fontWeight: '500'
                  }}
                >
                  ← Change time
                </button>
              </div>

              <form onSubmit={handleSubmit}>
                <h2 style={{ marginBottom: '24px', fontSize: '24px', color: '#1f2937' }}>
                  Complete Your Booking
                </h2>

                <div className="form-group">
                  <label className="form-label">First Name</label>
                  <input
                    type="text"
                    className="form-input"
                    value={bookingData.firstName}
                    onChange={(e) => setBookingData({ ...bookingData, firstName: e.target.value })}
                    required
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Last Name</label>
                  <input
                    type="text"
                    className="form-input"
                    value={bookingData.lastName}
                    onChange={(e) => setBookingData({ ...bookingData, lastName: e.target.value })}
                    required
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Email</label>
                  <input
                    type="email"
                    className="form-input"
                    value={bookingData.email}
                    onChange={(e) => setBookingData({ ...bookingData, email: e.target.value })}
                    required
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Phone</label>
                  <input
                    type="tel"
                    className="form-input"
                    value={bookingData.phone}
                    onChange={(e) => setBookingData({ ...bookingData, phone: e.target.value })}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Notes (optional)</label>
                  <textarea
                    className="form-input"
                    value={bookingData.notes}
                    onChange={(e) => setBookingData({ ...bookingData, notes: e.target.value })}
                    rows={3}
                    placeholder="Any special requests or information..."
                  />
                </div>
                <div style={{
                  display: 'flex',
                  gap: '12px',
                  justifyContent: 'flex-end',
                  marginTop: '32px',
                  paddingTop: '24px',
                  borderTop: '1px solid #e5e7eb'
                }}>
                  <button
                    type="button"
                    onClick={handleBackToCalendar}
                    className="button button-secondary"
                  >
                    Back
                  </button>
                  <button type="submit" className="button button-primary" style={{ minWidth: '200px' }}>
                    Confirm Booking - ${selectedSession.price}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    );
  }

  // Show session list view
  return (
    <div style={{ minHeight: '100vh', background: '#f9fafb' }}>
      <div style={{
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        padding: '48px 24px',
        boxShadow: '0 4px 6px rgba(0,0,0,0.1)'
      }}>
        <div className="container">
          <div style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            textAlign: 'center',
            gap: '24px'
          }}>
            {tenant?.logoUrl && (
              <div style={{
                width: '120px',
                height: '120px',
                borderRadius: '50%',
                overflow: 'hidden',
                background: 'white',
                padding: '8px',
                boxShadow: '0 4px 12px rgba(0,0,0,0.15)'
              }}>
                <img
                  src={tenant.logoUrl}
                  alt={`${tenant.name} logo`}
                  style={{
                    width: '100%',
                    height: '100%',
                    objectFit: 'cover',
                    borderRadius: '50%'
                  }}
                  onError={(e) => {
                    (e.target as HTMLImageElement).style.display = 'none';
                  }}
                />
              </div>
            )}
            <div>
              <h1 style={{
                color: 'white',
                fontSize: '36px',
                fontWeight: '700',
                margin: '0 0 16px 0',
                textShadow: '0 2px 4px rgba(0,0,0,0.1)'
              }}>
                {tenant?.name}
              </h1>
              {tenant?.description && (
                <p style={{
                  color: 'rgba(255, 255, 255, 0.95)',
                  fontSize: '18px',
                  lineHeight: '1.6',
                  maxWidth: '700px',
                  margin: '0 auto',
                  textShadow: '0 1px 2px rgba(0,0,0,0.1)'
                }}>
                  {tenant.description}
                </p>
              )}
            </div>
          </div>
        </div>
      </div>

      <div className="container" style={{ paddingTop: '32px', paddingBottom: '48px' }}>
        <h2 style={{ marginBottom: '16px', fontSize: '28px', color: '#1f2937' }}>Available Sessions</h2>
        {tenant && getTimezoneDifferenceMessage(tenant.timezone) && (
          <div style={{
            padding: '12px 16px',
            background: '#eff6ff',
            border: '1px solid #bfdbfe',
            borderRadius: '8px',
            marginBottom: '24px',
            fontSize: '14px',
            color: '#1e40af'
          }}>
            ℹ️ {getTimezoneDifferenceMessage(tenant.timezone)}
          </div>
        )}
        <div className="grid">
          {sessions.map((session) => (
            <div key={session.id} className="card" style={{ margin: 0 }}>
              <h3 style={{ marginBottom: '12px', fontSize: '20px', color: '#1f2937' }}>{session.name}</h3>
              <p style={{ color: '#6b7280', fontSize: '14px', margin: '12px 0', lineHeight: '1.6' }}>
                {session.description}
              </p>
              <div style={{ marginTop: '16px', fontSize: '14px', color: '#4b5563' }}>
                <div style={{ marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span style={{ fontSize: '18px' }}>⏱️</span>
                  <strong>Duration:</strong> {session.durationMinutes} minutes
                </div>
                <div style={{ marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span style={{ fontSize: '18px' }}>💰</span>
                  <strong>Price:</strong> ${session.price}
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span style={{ fontSize: '18px' }}>👥</span>
                  <strong>Capacity:</strong> {session.capacity} {session.capacity === 1 ? 'person' : 'people'}
                </div>
              </div>
              <button
                onClick={() => handleSelectSession(session)}
                className="button button-primary"
                style={{ 
                  width: '100%', 
                  marginTop: '24px',
                  padding: '12px 24px',
                  fontSize: '16px',
                  fontWeight: '600'
                }}
              >
                Book Now
              </button>
            </div>
          ))}
        </div>

        {sessions.length === 0 && (
          <div className="card">
            <p style={{ textAlign: 'center', color: '#6b7280', fontSize: '16px' }}>
              No sessions available at the moment. Please check back later.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
