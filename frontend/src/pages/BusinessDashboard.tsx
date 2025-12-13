import { useState, useEffect } from 'react';
import { useUser, useClerk } from '@clerk/clerk-react';
import { businessAPI } from '../services/api';
import type { SessionType, Booking, Tenant, BusinessHours } from '../types';
import WeeklySchedule from '../components/WeeklySchedule';
import BusinessCalendar from '../components/BusinessCalendar';
import ImageUpload from '../components/ImageUpload';
import { getUserTimezone, getTimezoneAbbreviation } from '../utils/timezone';

export default function BusinessDashboard() {
  const { user } = useUser();
  const { signOut } = useClerk();
  const [tenant, setTenant] = useState<Tenant | null>(null);
  const [sessions, setSessions] = useState<SessionType[]>([]);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [activeTab, setActiveTab] = useState<'sessions' | 'calendar' | 'bookings'>('sessions');
  const [showModal, setShowModal] = useState(false);
  const [blockedSlots, setBlockedSlots] = useState<any[]>([]);
  const [businessHours, setBusinessHours] = useState<BusinessHours[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [profileData, setProfileData] = useState({
    logoUrl: '',
    description: ''
  });
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    durationMinutes: 30,
    price: 25,
    currency: 'USD',
    capacity: 1,
    category: '',
    meetingLink: '',
    meetingPassword: ''
  });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      console.log('Loading business dashboard data...');
      const [tenantRes, sessionsRes, bookingsRes, blockedSlotsRes, businessHoursRes] = await Promise.all([
        businessAPI.getTenant(),
        businessAPI.getSessionTypes(),
        businessAPI.getBookings(),
        businessAPI.getBlockedSlots(),
        businessAPI.getBusinessHours()
      ]);
      console.log('Data loaded:', {
        tenant: tenantRes.data,
        sessionsCount: sessionsRes.data.length,
        bookingsCount: bookingsRes.data.length,
        blockedSlotsCount: blockedSlotsRes.data.length,
        bookingsSample: bookingsRes.data[0] // Log first booking to see structure
      });

      const tenantData = tenantRes.data;
      setTenant(tenantData);
      setSessions(Array.isArray(sessionsRes.data) ? sessionsRes.data : []);
      setBookings(Array.isArray(bookingsRes.data) ? bookingsRes.data : []);
      setBlockedSlots(Array.isArray(blockedSlotsRes.data) ? blockedSlotsRes.data : []);
      setBusinessHours(Array.isArray(businessHoursRes.data) ? businessHoursRes.data : []);

      // Initialize profile data with tenant info
      setProfileData({
        logoUrl: tenantData.logoUrl || '',
        description: tenantData.description || ''
      });

      // Auto-detect and update timezone if it's still UTC (default)
      if (tenantData.timezone === 'UTC') {
        const detectedTimezone = getUserTimezone();
        console.log('Detected timezone:', detectedTimezone);

        try {
          // Update tenant timezone to detected timezone
          await businessAPI.updateTenantTimezone(detectedTimezone);
          setTenant({ ...tenantData, timezone: detectedTimezone });
          console.log('✅ Timezone auto-updated to:', detectedTimezone);
        } catch (error) {
          console.error('Failed to update timezone:', error);
        }
      }

      setLoading(false);
    } catch (error: any) {
      console.error('Failed to load data', error);
      setError(error.response?.data?.message || error.message || 'Failed to load dashboard data');
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    console.log('📝 Creating session type with data:', formData);
    try {
      const response = await businessAPI.createSessionType(formData);
      console.log('✅ Session type created successfully:', response);
      setShowModal(false);
      setFormData({ name: '', description: '', durationMinutes: 30, price: 25, currency: 'USD', capacity: 1, category: '', meetingLink: '', meetingPassword: '' });
      loadData();
      alert('Session type created successfully!');
    } catch (error: any) {
      console.error('❌ Error creating session type:', error);
      console.error('Error response:', error.response);
      console.error('Error status:', error.response?.status);
      console.error('Error data:', error.response?.data);
      const errorMessage = error.response?.data?.message || error.message || 'Failed to create session type';
      alert('Error: ' + errorMessage);
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

  const handleSaveSchedule = async (schedule: any) => {
    console.log('Saving schedule:', schedule);
    try {
      // Convert schedule object to BusinessHours array
      const businessHoursData: Partial<BusinessHours>[] = [];
      const dayMap: Record<string, BusinessHours['dayOfWeek']> = {
        'Monday': 'MONDAY',
        'Tuesday': 'TUESDAY',
        'Wednesday': 'WEDNESDAY',
        'Thursday': 'THURSDAY',
        'Friday': 'FRIDAY',
        'Saturday': 'SATURDAY',
        'Sunday': 'SUNDAY'
      };

      Object.entries(schedule).forEach(([day, data]: [string, any]) => {
        if (data.enabled) {
          businessHoursData.push({
            dayOfWeek: dayMap[day],
            startTime: data.startTime,
            endTime: data.endTime,
            enabled: true
          });
        }
      });

      await businessAPI.updateAllBusinessHours(businessHoursData);
      setBusinessHours(businessHoursData as BusinessHours[]);
      alert('Schedule saved successfully!');
    } catch (error: any) {
      console.error('Failed to save schedule:', error);
      const errorMessage = error.response?.data?.message || 'Failed to save schedule';
      alert('Error: ' + errorMessage);
    }
  };

  const formatDateTime = (timestamp: number | undefined | null): string => {
    try {
      if (!timestamp && timestamp !== 0) {
        console.error('No timestamp provided:', timestamp);
        return 'No Date';
      }

      // Convert epoch timestamp to date
      const date = new Date(timestamp);
      if (isNaN(date.getTime())) {
        console.error('Invalid epoch timestamp:', timestamp);
        return 'Invalid Date';
      }

      return date.toLocaleString('en-US', {
        weekday: 'short',
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (error) {
      console.error('Error formatting date:', timestamp, error);
      return 'Invalid Date';
    }
  };

  const handleBlockSlot = async (date: Date, startTimeEpoch: string, endTimeEpoch: string) => {
    console.log('Blocking slot:', { date, startTimeEpoch, endTimeEpoch });
    try {
      // Send epoch timestamps as numbers to the backend
      const response = await businessAPI.createBlockedSlot({
        startTime: parseInt(startTimeEpoch),
        endTime: parseInt(endTimeEpoch),
        reason: 'Blocked by business'
      });
      setBlockedSlots([...blockedSlots, response.data]);
      alert('Time slot blocked successfully!');
    } catch (error: any) {
      console.error('Failed to block slot:', error);
      const errorMessage = error.response?.data?.message || 'Failed to block time slot';
      alert('Error: ' + errorMessage);
    }
  };

  const handleUnblockSlot = async (slotId: string) => {
    try {
      await businessAPI.deleteBlockedSlot(slotId);
      setBlockedSlots(blockedSlots.filter(slot => slot.id !== slotId));
      alert('Time slot unblocked successfully!');
    } catch (error: any) {
      console.error('Failed to unblock slot:', error);
      const errorMessage = error.response?.data?.message || 'Failed to unblock time slot';
      alert('Error: ' + errorMessage);
    }
  };

  const handleLogoUpload = async (file: File): Promise<string> => {
    try {
      console.log('Uploading logo:', file.name);
      const response = await businessAPI.uploadLogo(file);
      const logoUrl = response.data.logoUrl;

      // Update local state
      setProfileData({ ...profileData, logoUrl });

      // Reload tenant data to get the updated logo
      const tenantRes = await businessAPI.getTenant();
      setTenant(tenantRes.data);

      alert('Logo uploaded successfully!');
      return logoUrl;
    } catch (error: any) {
      console.error('Failed to upload logo:', error);
      throw error;
    }
  };

  const handleSaveProfile = async () => {
    try {
      setIsSavingProfile(true);
      // Only save description (logo is handled separately via upload)
      const response = await businessAPI.updateTenantProfile({
        description: profileData.description
      });
      setTenant(response.data);
      alert('Profile updated successfully!');
    } catch (error: any) {
      console.error('Failed to save profile:', error);
      const errorMessage = error.response?.data?.message || 'Failed to save profile';
      alert('Error: ' + errorMessage);
    } finally {
      setIsSavingProfile(false);
    }
  };

  const copyToClipboard = async (text: string, label: string) => {
    try {
      await navigator.clipboard.writeText(text);
      alert(`✓ ${label} copied to clipboard!`);
    } catch (error) {
      console.error('Failed to copy:', error);
      alert('Failed to copy to clipboard');
    }
  };

  return (
    <div>
      <div className="navbar">
        <div>
          <div className="navbar-title">{tenant?.name || 'Business Dashboard'}</div>
          <div style={{ fontSize: '14px', color: '#6b7280' }}>Welcome, {user?.firstName} {user?.lastName}</div>
        </div>
        <button onClick={() => signOut()} className="button button-secondary">
          Logout
        </button>
      </div>

      <div className="container">
        {tenant && (
          <div style={{
            padding: '12px 16px',
            background: '#f0fdf4',
            border: '1px solid #bbf7d0',
            borderRadius: '8px',
            marginBottom: '20px',
            fontSize: '14px',
            color: '#166534',
            display: 'flex',
            alignItems: 'center',
            gap: '8px'
          }}>
            🌍 <strong>Business Timezone:</strong> {tenant.timezone} ({getTimezoneAbbreviation(tenant.timezone)})
          </div>
        )}

        {loading && (
          <div style={{ padding: '40px', textAlign: 'center' }}>
            <p>Loading dashboard...</p>
          </div>
        )}

        {error && (
          <div className="card" style={{ background: '#fee2e2', color: '#991b1b', padding: '20px', marginBottom: '20px' }}>
            <h3 style={{ margin: '0 0 10px 0' }}>Error Loading Dashboard</h3>
            <p style={{ margin: 0 }}>{error}</p>
            <button onClick={loadData} className="button button-primary" style={{ marginTop: '12px' }}>
              Retry
            </button>
          </div>
        )}

        {!loading && !error && tenant && (
          <div className="card">
            <h3>Your Booking Link</h3>
            <div style={{
              marginTop: '12px',
              display: 'flex',
              gap: '8px',
              alignItems: 'center'
            }}>
              <div style={{
                flex: 1,
                padding: '12px',
                background: '#f3f4f6',
                borderRadius: '4px',
                fontFamily: 'monospace',
                fontSize: '14px',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap'
              }}>
                http://localhost:5173/book/{tenant.slug}
              </div>
              <button
                onClick={() => copyToClipboard(`http://localhost:5173/book/${tenant.slug}`, 'Booking link')}
                className="button button-secondary"
                style={{
                  padding: '12px 16px',
                  fontSize: '14px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  whiteSpace: 'nowrap'
                }}
              >
                📋 Copy
              </button>
            </div>
          </div>
        )}

        {!loading && !error && tenant && (
          <div className="card">
            <h3 style={{ marginBottom: '16px' }}>Business Profile</h3>
            <p style={{ color: '#6b7280', fontSize: '14px', marginBottom: '20px' }}>
              Customize your public booking page with a logo and description to make it more personal and professional.
            </p>

            <ImageUpload
              currentImageUrl={profileData.logoUrl}
              onUpload={handleLogoUpload}
            />

            <div className="form-group">
              <label className="form-label">Description</label>
              <textarea
                className="form-input"
                value={profileData.description}
                onChange={(e) => setProfileData({ ...profileData, description: e.target.value })}
                rows={4}
                placeholder="Describe your business, services, or what customers can expect..."
                maxLength={2000}
              />
              <small style={{ color: '#6b7280', fontSize: '12px', display: 'block', marginTop: '4px' }}>
                {profileData.description.length}/2000 characters
              </small>
            </div>

            <button
              onClick={handleSaveProfile}
              className="button button-primary"
              disabled={isSavingProfile}
            >
              {isSavingProfile ? 'Saving...' : 'Save Description'}
            </button>
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
              onClick={() => {
                console.log('Switching to calendar tab');
                setActiveTab('calendar');
              }}
              style={{
                padding: '12px 24px',
                border: 'none',
                background: 'none',
                cursor: 'pointer',
                borderBottom: activeTab === 'calendar' ? '2px solid #4f46e5' : 'none',
                color: activeTab === 'calendar' ? '#4f46e5' : '#6b7280',
                fontWeight: activeTab === 'calendar' ? '600' : '400'
              }}
            >
              Calendar
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
                {tenant && sessions.map((session) => (
                  <div key={session.id} className="card" style={{ margin: 0 }}>
                    <h4>{session.name}</h4>
                    <p style={{ color: '#6b7280', fontSize: '14px', margin: '8px 0' }}>
                      {session.description}
                    </p>
                    <div style={{ marginTop: '12px', fontSize: '14px' }}>
                      <div>Duration: {session.durationMinutes} minutes</div>
                      <div>Price: {session.currency === 'EUR' ? '€' : '$'}{session.price}</div>
                      <div>Capacity: {session.capacity} people</div>
                      {session.meetingLink && (
                        <div style={{ marginTop: '8px', padding: '8px', background: '#eff6ff', borderRadius: '4px' }}>
                          📹 <a href={session.meetingLink} target="_blank" rel="noopener noreferrer" style={{ color: '#3b82f6', textDecoration: 'none' }}>
                            Google Meet Link
                          </a>
                        </div>
                      )}
                    </div>
                    <div style={{
                      marginTop: '16px',
                      padding: '12px',
                      background: '#f9fafb',
                      borderRadius: '6px',
                      border: '1px solid #e5e7eb'
                    }}>
                      <div style={{
                        fontSize: '12px',
                        color: '#6b7280',
                        marginBottom: '8px',
                        fontWeight: '500'
                      }}>
                        Direct Booking Link:
                      </div>
                      <div style={{
                        display: 'flex',
                        gap: '8px',
                        alignItems: 'center'
                      }}>
                        <div style={{
                          flex: 1,
                          fontSize: '11px',
                          fontFamily: 'monospace',
                          color: '#4b5563',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap'
                        }}>
                          http://localhost:5173/book/{tenant.slug}/{session.id}
                        </div>
                        <button
                          onClick={() => copyToClipboard(
                            `http://localhost:5173/book/${tenant.slug}/${session.id}`,
                            `Link for "${session.name}"`
                          )}
                          style={{
                            background: 'none',
                            border: 'none',
                            cursor: 'pointer',
                            fontSize: '16px',
                            padding: '4px 8px',
                            color: '#6b7280',
                            transition: 'color 0.2s'
                          }}
                          onMouseEnter={(e) => {
                            e.currentTarget.style.color = '#4f46e5';
                          }}
                          onMouseLeave={(e) => {
                            e.currentTarget.style.color = '#6b7280';
                          }}
                          title="Copy session link"
                        >
                          📋
                        </button>
                      </div>
                    </div>
                    <button
                      onClick={() => handleDeleteSession(session.id)}
                      className="button button-danger"
                      style={{ marginTop: '12px', fontSize: '12px', padding: '6px 12px', width: '100%' }}
                    >
                      Delete
                    </button>
                  </div>
                ))}
              </div>
            </>
          )}

          {activeTab === 'calendar' && (
            <div style={{ padding: '20px', background: '#f9fafb', minHeight: '400px' }}>
              <h3 style={{ marginBottom: '16px', color: '#1f2937', fontSize: '20px' }}>Calendar View</h3>
              <p style={{ marginBottom: '24px', color: '#6b7280' }}>
                Debug Info - Bookings: {bookings.length}, Blocked Slots: {blockedSlots.length}
              </p>

              <div style={{ marginBottom: '32px', background: 'white', padding: '20px', borderRadius: '8px' }}>
                <h4 style={{ marginBottom: '16px' }}>Weekly Schedule</h4>
                <WeeklySchedule onSave={handleSaveSchedule} initialHours={businessHours} />
              </div>

              <div style={{ background: 'white', padding: '20px', borderRadius: '8px' }}>
                <h4 style={{ marginBottom: '16px' }}>Business Calendar</h4>
                <BusinessCalendar
                  bookings={bookings}
                  blockedSlots={blockedSlots}
                  onBlockSlot={handleBlockSlot}
                  onUnblockSlot={handleUnblockSlot}
                />
              </div>
            </div>
          )}

          {activeTab === 'bookings' && (
            <>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                <h3 style={{ margin: 0 }}>Upcoming Bookings</h3>
                <button
                  onClick={loadData}
                  className="button button-secondary"
                  style={{ fontSize: '14px', padding: '8px 16px' }}
                >
                  🔄 Refresh
                </button>
              </div>
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
                      <td>{formatDateTime(booking.startTime)}</td>
                      <td>{booking.customer?.firstName} {booking.customer?.lastName}</td>
                      <td>
                        <span style={{
                          padding: '4px 8px',
                          borderRadius: '4px',
                          fontSize: '12px',
                          fontWeight: '500',
                          background: booking.status === 'CONFIRMED' ? '#d1fae5' : 
                                      booking.status === 'PENDING_PAYMENT' ? '#fef3c7' : 
                                      booking.status === 'PAYMENT_FAILED' ? '#fee2e2' : '#fee2e2',
                          color: booking.status === 'CONFIRMED' ? '#065f46' : 
                                 booking.status === 'PENDING_PAYMENT' ? '#92400e' : 
                                 booking.status === 'PAYMENT_FAILED' ? '#991b1b' : '#991b1b'
                        }}>
                          {booking.status === 'PENDING_PAYMENT' ? 'Payment Pending' : booking.status}
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
                <label className="form-label">Price</label>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <input
                    type="number"
                    className="form-input"
                    value={formData.price}
                    onChange={(e) => setFormData({ ...formData, price: parseFloat(e.target.value) })}
                    min="0"
                    step="0.01"
                    required
                    style={{ flex: 1 }}
                  />
                  <select
                    className="form-input"
                    value={formData.currency}
                    onChange={(e) => setFormData({ ...formData, currency: e.target.value })}
                    style={{ width: '100px' }}
                  >
                    <option value="USD">USD ($)</option>
                    <option value="EUR">EUR (€)</option>
                  </select>
                </div>
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

              <div style={{ borderTop: '1px solid #e5e7eb', paddingTop: '16px', marginTop: '16px' }}>
                <h4 style={{ marginBottom: '12px', fontSize: '16px', color: '#1f2937' }}>📹 Virtual Meeting (Optional)</h4>
                <p style={{ fontSize: '14px', color: '#6b7280', marginBottom: '16px' }}>
                  Add a Google Meet link for virtual sessions. Create a meeting at <a href="https://meet.google.com/" target="_blank" rel="noopener noreferrer" style={{ color: '#4f46e5' }}>meet.google.com</a>
                </p>

                <div className="form-group">
                  <label className="form-label">Google Meet Link</label>
                  <input
                    type="url"
                    className="form-input"
                    value={formData.meetingLink}
                    onChange={(e) => setFormData({ ...formData, meetingLink: e.target.value })}
                    placeholder="https://meet.google.com/abc-defg-hij"
                  />
                  <small style={{ color: '#6b7280', fontSize: '12px', display: 'block', marginTop: '4px' }}>
                    This link will be included in booking confirmation emails
                  </small>
                </div>

                <div className="form-group">
                  <label className="form-label">Meeting Password (Optional)</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.meetingPassword}
                    onChange={(e) => setFormData({ ...formData, meetingPassword: e.target.value })}
                    placeholder="Optional meeting password"
                  />
                </div>
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
