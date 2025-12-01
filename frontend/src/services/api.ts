import axios from 'axios';
import type { LoginResponse, Tenant, SessionType, Booking } from '../types';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add token to requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Auth API
export const authAPI = {
  login: (email: string, password: string) =>
    api.post<LoginResponse>('/auth/login', { email, password }),
};

// Admin API
export const adminAPI = {
  getTenants: () => api.get<Tenant[]>('/admin/tenants'),
  getTenant: (id: string) => api.get<Tenant>(`/admin/tenants/${id}`),
  createTenant: (data: Partial<Tenant>) => api.post<Tenant>('/admin/tenants', data),
  updateTenant: (id: string, data: Partial<Tenant>) =>
    api.put<Tenant>(`/admin/tenants/${id}`, data),
  deleteTenant: (id: string) => api.delete(`/admin/tenants/${id}`),
};

// Business API
export const businessAPI = {
  getTenant: () => api.get<Tenant>('/business/tenant'),
  getSessionTypes: () => api.get<SessionType[]>('/business/sessions'),
  createSessionType: (data: Partial<SessionType>) =>
    api.post<SessionType>('/business/sessions', data),
  updateSessionType: (id: string, data: Partial<SessionType>) =>
    api.put<SessionType>(`/business/sessions/${id}`, data),
  deleteSessionType: (id: string) => api.delete(`/business/sessions/${id}`),
  getBookings: () => api.get<Booking[]>('/business/bookings'),
  cancelBooking: (id: string) => api.delete(`/business/bookings/${id}`),
};

// Customer API
export const customerAPI = {
  getTenantBySlug: (slug: string) => api.get<Tenant>(`/customer/tenants/${slug}`),
  getSessionTypes: (tenantId: string) =>
    api.get<SessionType[]>(`/customer/tenants/${tenantId}/sessions`),
  createBooking: (tenantId: string, data: any) =>
    api.post<Booking>(`/customer/tenants/${tenantId}/bookings`, data),
};

export default api;
