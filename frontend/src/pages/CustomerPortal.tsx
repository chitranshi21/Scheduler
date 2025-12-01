import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { customerAPI } from '../services/api';
import type { Tenant, SessionType } from '../types';

export default function CustomerPortal() {
  const { slug } = useParams<{ slug: string }>();
  const [tenant, setTenant] = useState<Tenant | null>(null);
  const [sessions, setSessions] = useState<SessionType[]>([]);
  const [selectedSession, setSelectedSession] = useState<SessionType | null>(null);
  const [showBookingForm, setShowBookingForm] = useState(false);
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
    setSuccess(false);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedSession || !tenant) return;

    try {
      await customerAPI.createBooking(tenant.id, {
        sessionTypeId: selectedSession.id,
        startTime: new Date(bookingData.startTime).toISOString(),
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
    } catch (error) {
      alert('Failed to create booking. Please try again.');
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
          <div className="modal-content">
            <div className="modal-header">
              Book: {selectedSession.name}
            </div>
            {success ? (
              <div style={{
                padding: '24px',
                textAlign: 'center',
                color: '#10b981',
                fontSize: '18px',
                fontWeight: '500'
              }}>
                ✓ Booking confirmed! Check your email for details.
              </div>
            ) : (
              <form onSubmit={handleSubmit}>
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
                  <label className="form-label">Preferred Date & Time</label>
                  <input
                    type="datetime-local"
                    className="form-input"
                    value={bookingData.startTime}
                    onChange={(e) => setBookingData({ ...bookingData, startTime: e.target.value })}
                    required
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Notes (optional)</label>
                  <textarea
                    className="form-input"
                    value={bookingData.notes}
                    onChange={(e) => setBookingData({ ...bookingData, notes: e.target.value })}
                    rows={3}
                  />
                </div>
                <div className="modal-footer">
                  <button
                    type="button"
                    onClick={() => {
                      setShowBookingForm(false);
                      setSelectedSession(null);
                    }}
                    className="button button-secondary"
                  >
                    Cancel
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
