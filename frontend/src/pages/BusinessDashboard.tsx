import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { businessAPI } from '../services/api';
import type { SessionType, Booking, Tenant } from '../types';

export default function BusinessDashboard() {
  const { logout, user } = useAuth();
  const [tenant, setTenant] = useState<Tenant | null>(null);
  const [sessions, setSessions] = useState<SessionType[]>([]);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [activeTab, setActiveTab] = useState<'sessions' | 'bookings'>('sessions');
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    durationMinutes: 30,
    price: 25,
    capacity: 1,
    category: ''
  });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [tenantRes, sessionsRes, bookingsRes] = await Promise.all([
        businessAPI.getTenant(),
        businessAPI.getSessionTypes(),
        businessAPI.getBookings()
      ]);
      setTenant(tenantRes.data);
      setSessions(sessionsRes.data);
      setBookings(bookingsRes.data);
    } catch (error) {
      console.error('Failed to load data', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await businessAPI.createSessionType(formData);
      setShowModal(false);
      setFormData({ name: '', description: '', durationMinutes: 30, price: 25, capacity: 1, category: '' });
      loadData();
    } catch (error) {
      alert('Failed to create session type');
    }
  };

  const handleDeleteSession = async (id: string) => {
    if (window.confirm('Are you sure you want to delete this session type?')) {
      try {
        await businessAPI.deleteSessionType(id);
        loadData();
      } catch (error) {
        alert('Failed to delete session type');
      }
    }
  };

  const handleCancelBooking = async (id: string) => {
    if (window.confirm('Cancel this booking? The customer will be notified.')) {
      try {
        await businessAPI.cancelBooking(id);
        loadData();
      } catch (error) {
        alert('Failed to cancel booking');
      }
    }
  };

  return (
    <div>
      <div className="navbar">
        <div>
          <div className="navbar-title">{tenant?.name || 'Business Dashboard'}</div>
          <div style={{ fontSize: '14px', color: '#6b7280' }}>Welcome, {user?.name}</div>
        </div>
        <button onClick={logout} className="button button-secondary">
          Logout
        </button>
      </div>

      <div className="container">
        {tenant && (
          <div className="card">
            <h3>Your Booking Link</h3>
            <div style={{
              marginTop: '12px',
              padding: '12px',
              background: '#f3f4f6',
              borderRadius: '4px',
              fontFamily: 'monospace'
            }}>
              http://localhost:5173/book/{tenant.slug}
            </div>
          </div>
        )}

        <div className="card">
          <div style={{ borderBottom: '1px solid #e5e7eb', marginBottom: '24px' }}>
            <button
              onClick={() => setActiveTab('sessions')}
              style={{
                padding: '12px 24px',
                border: 'none',
                background: 'none',
                cursor: 'pointer',
                borderBottom: activeTab === 'sessions' ? '2px solid #4f46e5' : 'none',
                color: activeTab === 'sessions' ? '#4f46e5' : '#6b7280',
                fontWeight: activeTab === 'sessions' ? '600' : '400'
              }}
            >
              Session Types
            </button>
            <button
              onClick={() => setActiveTab('bookings')}
              style={{
                padding: '12px 24px',
                border: 'none',
                background: 'none',
                cursor: 'pointer',
                borderBottom: activeTab === 'bookings' ? '2px solid #4f46e5' : 'none',
                color: activeTab === 'bookings' ? '#4f46e5' : '#6b7280',
                fontWeight: activeTab === 'bookings' ? '600' : '400'
              }}
            >
              Bookings ({bookings.length})
            </button>
          </div>

          {activeTab === 'sessions' && (
            <>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '16px' }}>
                <h3>Session Types</h3>
                <button onClick={() => setShowModal(true)} className="button button-primary">
                  + Add Session Type
                </button>
              </div>
              <div className="grid">
                {sessions.map((session) => (
                  <div key={session.id} className="card" style={{ margin: 0 }}>
                    <h4>{session.name}</h4>
                    <p style={{ color: '#6b7280', fontSize: '14px', margin: '8px 0' }}>
                      {session.description}
                    </p>
                    <div style={{ marginTop: '12px', fontSize: '14px' }}>
                      <div>Duration: {session.durationMinutes} minutes</div>
                      <div>Price: ${session.price}</div>
                      <div>Capacity: {session.capacity} people</div>
                    </div>
                    <button
                      onClick={() => handleDeleteSession(session.id)}
                      className="button button-danger"
                      style={{ marginTop: '12px', fontSize: '12px', padding: '6px 12px' }}
                    >
                      Delete
                    </button>
                  </div>
                ))}
              </div>
            </>
          )}

          {activeTab === 'bookings' && (
            <>
              <h3 style={{ marginBottom: '16px' }}>Upcoming Bookings</h3>
              <table className="table">
                <thead>
                  <tr>
                    <th>Session</th>
                    <th>Date & Time</th>
                    <th>Customer</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {bookings.map((booking) => (
                    <tr key={booking.id}>
                      <td>{booking.sessionType?.name}</td>
                      <td>{new Date(booking.startTime).toLocaleString()}</td>
                      <td>{booking.customer?.firstName} {booking.customer?.lastName}</td>
                      <td>
                        <span style={{
                          padding: '4px 8px',
                          borderRadius: '4px',
                          fontSize: '12px',
                          background: booking.status === 'CONFIRMED' ? '#d1fae5' : '#fee2e2',
                          color: booking.status === 'CONFIRMED' ? '#065f46' : '#991b1b'
                        }}>
                          {booking.status}
                        </span>
                      </td>
                      <td>
                        {booking.status !== 'CANCELLED' && (
                          <button
                            onClick={() => handleCancelBooking(booking.id)}
                            className="button button-danger"
                            style={{ fontSize: '12px', padding: '6px 12px' }}
                          >
                            Cancel
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </>
          )}
        </div>
      </div>

      {showModal && (
        <div className="modal">
          <div className="modal-content">
            <div className="modal-header">Create New Session Type</div>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label className="form-label">Session Name</label>
                <input
                  type="text"
                  className="form-input"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="e.g., 30-min Yoga Class"
                  required
                />
              </div>
              <div className="form-group">
                <label className="form-label">Description</label>
                <textarea
                  className="form-input"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  rows={3}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Duration (minutes)</label>
                <input
                  type="number"
                  className="form-input"
                  value={formData.durationMinutes}
                  onChange={(e) => setFormData({ ...formData, durationMinutes: parseInt(e.target.value) })}
                  min="15"
                  required
                />
              </div>
              <div className="form-group">
                <label className="form-label">Price ($)</label>
                <input
                  type="number"
                  className="form-input"
                  value={formData.price}
                  onChange={(e) => setFormData({ ...formData, price: parseFloat(e.target.value) })}
                  min="0"
                  step="0.01"
                  required
                />
              </div>
              <div className="form-group">
                <label className="form-label">Capacity</label>
                <input
                  type="number"
                  className="form-input"
                  value={formData.capacity}
                  onChange={(e) => setFormData({ ...formData, capacity: parseInt(e.target.value) })}
                  min="1"
                  required
                />
              </div>
              <div className="modal-footer">
                <button type="button" onClick={() => setShowModal(false)} className="button button-secondary">
                  Cancel
                </button>
                <button type="submit" className="button button-primary">
                  Create
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
