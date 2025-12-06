import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { customerAPI } from '../services/api';
import type { Tenant, SessionType } from '../types';
import BookingCalendar from '../components/BookingCalendar';

export default function CustomerPortal() {
  const { slug } = useParams<{ slug: string }>();
  const [tenant, setTenant] = useState<Tenant | null>(null);
  const [sessions, setSessions] = useState<SessionType[]>([]);
  const [selectedSession, setSelectedSession] = useState<SessionType | null>(null);
  const [showBookingForm, setShowBookingForm] = useState(false);
  const [showCustomerForm, setShowCustomerForm] = useState(false);
  const [bookingData, setBookingData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    startTime: '',
    notes: ''
  });
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (slug) {
      loadTenantData();
    }
  }, [slug]);

  const loadTenantData = async () => {
    try {
      const tenantRes = await customerAPI.getTenantBySlug(slug!);
      setTenant(tenantRes.data);

      const sessionsRes = await customerAPI.getSessionTypes(tenantRes.data.id);
      setSessions(sessionsRes.data);
    } catch (error) {
      console.error('Failed to load tenant data', error);
    }
  };

  const handleSelectSession = (session: SessionType) => {
    setSelectedSession(session);
    setShowBookingForm(true);
    setShowCustomerForm(false);
    setSuccess(false);
  };

  const handleTimeSlotSelect = (datetime: string) => {
    setBookingData({ ...bookingData, startTime: datetime });
    setShowCustomerForm(true);
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

      await customerAPI.createBooking(tenant.id, {
        sessionTypeId: selectedSession.id,
        startTime: startTimeEpoch,
        firstName: bookingData.firstName,
        lastName: bookingData.lastName,
        email: bookingData.email,
        phone: bookingData.phone,
        notes: bookingData.notes,
        participants: 1
      });

      setSuccess(true);
      setBookingData({ firstName: '', lastName: '', email: '', phone: '', startTime: '', notes: '' });
      setTimeout(() => {
        setShowBookingForm(false);
        setSelectedSession(null);
        setSuccess(false);
      }, 3000);
    } catch (error: any) {
      console.error('Failed to create booking:', error);
      const errorMessage = error.response?.data?.message || error.message || 'Failed to create booking. Please try again.';
      alert(errorMessage);
    }
  };

  return (
    <div style={{ minHeight: '100vh', background: '#f9fafb' }}>
      <div style={{
        background: 'white',
        padding: '24px',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
      }}>
        <div className="container">
          <h1>{tenant?.name}</h1>
          <p style={{ color: '#6b7280', marginTop: '8px' }}>{tenant?.description}</p>
        </div>
      </div>

      <div className="container" style={{ paddingTop: '32px' }}>
        <h2 style={{ marginBottom: '24px' }}>Available Sessions</h2>
        <div className="grid">
          {sessions.map((session) => (
            <div key={session.id} className="card" style={{ margin: 0 }}>
              <h3>{session.name}</h3>
              <p style={{ color: '#6b7280', fontSize: '14px', margin: '12px 0' }}>
                {session.description}
              </p>
              <div style={{ marginTop: '16px', fontSize: '14px' }}>
                <div style={{ marginBottom: '8px' }}>
                  <strong>Duration:</strong> {session.durationMinutes} minutes
                </div>
                <div style={{ marginBottom: '8px' }}>
                  <strong>Price:</strong> ${session.price}
                </div>
                <div>
                  <strong>Capacity:</strong> {session.capacity} {session.capacity === 1 ? 'person' : 'people'}
                </div>
              </div>
              <button
                onClick={() => handleSelectSession(session)}
                className="button button-primary"
                style={{ width: '100%', marginTop: '16px' }}
              >
                Book Now
              </button>
            </div>
          ))}
        </div>

        {sessions.length === 0 && (
          <div className="card">
            <p style={{ textAlign: 'center', color: '#6b7280' }}>
              No sessions available at the moment. Please check back later.
            </p>
          </div>
        )}
      </div>

      {showBookingForm && selectedSession && (
        <div className="modal">
          <div className="modal-content" style={{ maxWidth: showCustomerForm ? '600px' : '920px' }}>
            <div className="modal-header">
              <div>
                <h2 style={{ margin: 0, fontSize: '20px' }}>Book: {selectedSession.name}</h2>
                <p style={{ margin: '4px 0 0 0', fontSize: '14px', color: '#6b7280', fontWeight: 'normal' }}>
                  ${selectedSession.price} • {selectedSession.durationMinutes} minutes
                </p>
              </div>
              <button
                onClick={() => {
                  setShowBookingForm(false);
                  setSelectedSession(null);
                  setShowCustomerForm(false);
                }}
                style={{
                  background: 'none',
                  border: 'none',
                  fontSize: '24px',
                  color: '#6b7280',
                  cursor: 'pointer',
                  padding: '0',
                  lineHeight: '1'
                }}
              >
                ×
              </button>
            </div>
            {success ? (
              <div style={{
                padding: '48px 24px',
                textAlign: 'center'
              }}>
                <div style={{
                  width: '64px',
                  height: '64px',
                  background: '#d1fae5',
                  borderRadius: '50%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  margin: '0 auto 16px',
                  fontSize: '32px',
                  color: '#10b981'
                }}>
                  ✓
                </div>
                <h3 style={{ color: '#10b981', fontSize: '24px', fontWeight: '600', margin: '0 0 8px 0' }}>
                  Booking Confirmed!
                </h3>
                <p style={{ color: '#6b7280', fontSize: '16px', margin: 0 }}>
                  Check your email for confirmation details.
                </p>
              </div>
            ) : !showCustomerForm ? (
              <div>
                <BookingCalendar
                  sessionDurationMinutes={selectedSession.durationMinutes}
                  onSelectSlot={handleTimeSlotSelect}
                  tenantId={tenant?.id || ''}
                  sessionTypeId={selectedSession.id}
                />
              </div>
            ) : (
              <form onSubmit={handleSubmit}>
                <div style={{
                  padding: '16px 24px',
                  background: '#f9fafb',
                  borderRadius: '8px',
                  marginBottom: '24px'
                }}>
                  <p style={{ margin: 0, fontSize: '14px', color: '#6b7280' }}>
                    <strong>Selected Time:</strong>{' '}
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
                    onClick={() => setShowCustomerForm(false)}
                    style={{
                      background: 'none',
                      border: 'none',
                      color: '#4f46e5',
                      fontSize: '14px',
                      cursor: 'pointer',
                      padding: '4px 0',
                      marginTop: '8px'
                    }}
                  >
                    ← Change time
                  </button>
                </div>

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
                <div className="modal-footer">
                  <button
                    type="button"
                    onClick={() => setShowCustomerForm(false)}
                    className="button button-secondary"
                  >
                    Back
                  </button>
                  <button type="submit" className="button button-primary">
                    Confirm Booking - ${selectedSession.price}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
