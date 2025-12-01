import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { adminAPI } from '../services/api';
import type { Tenant } from '../types';

export default function AdminDashboard() {
  const { logout, user } = useAuth();
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    slug: '',
    email: '',
    phone: '',
    description: '',
    subscriptionTier: 'BASIC'
  });

  useEffect(() => {
    loadTenants();
  }, []);

  const loadTenants = async () => {
    try {
      const response = await adminAPI.getTenants();
      setTenants(response.data);
    } catch (error) {
      console.error('Failed to load tenants', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await adminAPI.createTenant(formData);
      setShowModal(false);
      setFormData({ name: '', slug: '', email: '', phone: '', description: '', subscriptionTier: 'BASIC' });
      loadTenants();
    } catch (error) {
      alert('Failed to create tenant');
    }
  };

  const handleDelete = async (id: string) => {
    if (window.confirm('Are you sure you want to delete this tenant?')) {
      try {
        await adminAPI.deleteTenant(id);
        loadTenants();
      } catch (error) {
        alert('Failed to delete tenant');
      }
    }
  };

  return (
    <div>
      <div className="navbar">
        <div>
          <div className="navbar-title">Admin Dashboard</div>
          <div style={{ fontSize: '14px', color: '#6b7280' }}>Welcome, {user?.name}</div>
        </div>
        <button onClick={logout} className="button button-secondary">
          Logout
        </button>
      </div>

      <div className="container">
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
            <h2>Tenants</h2>
            <button onClick={() => setShowModal(true)} className="button button-primary">
              + Add Tenant
            </button>
          </div>

          {loading ? (
            <p>Loading...</p>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Slug</th>
                  <th>Email</th>
                  <th>Status</th>
                  <th>Subscription</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {tenants.map((tenant) => (
                  <tr key={tenant.id}>
                    <td>{tenant.name}</td>
                    <td>{tenant.slug}</td>
                    <td>{tenant.email}</td>
                    <td>
                      <span style={{
                        padding: '4px 8px',
                        borderRadius: '4px',
                        fontSize: '12px',
                        background: tenant.status === 'ACTIVE' ? '#d1fae5' : '#fee2e2',
                        color: tenant.status === 'ACTIVE' ? '#065f46' : '#991b1b'
                      }}>
                        {tenant.status}
                      </span>
                    </td>
                    <td>{tenant.subscriptionTier}</td>
                    <td>
                      <button
                        onClick={() => handleDelete(tenant.id)}
                        className="button button-danger"
                        style={{ fontSize: '12px', padding: '6px 12px' }}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {showModal && (
        <div className="modal">
          <div className="modal-content">
            <div className="modal-header">Create New Tenant</div>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label className="form-label">Business Name</label>
                <input
                  type="text"
                  className="form-input"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  required
                />
              </div>
              <div className="form-group">
                <label className="form-label">Slug (URL identifier)</label>
                <input
                  type="text"
                  className="form-input"
                  value={formData.slug}
                  onChange={(e) => setFormData({ ...formData, slug: e.target.value.toLowerCase() })}
                  placeholder="e.g., yoga-studio"
                  required
                />
              </div>
              <div className="form-group">
                <label className="form-label">Email</label>
                <input
                  type="email"
                  className="form-input"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  required
                />
              </div>
              <div className="form-group">
                <label className="form-label">Phone</label>
                <input
                  type="text"
                  className="form-input"
                  value={formData.phone}
                  onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
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
              <div className="modal-footer">
                <button type="button" onClick={() => setShowModal(false)} className="button button-secondary">
                  Cancel
                </button>
                <button type="submit" className="button button-primary">
                  Create Tenant
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
