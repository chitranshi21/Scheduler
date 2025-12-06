import axios from 'axios';
import type { Tenant, SessionType, Booking } from '../types';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Setup function to inject Clerk's getToken function
let getClerkToken: (() => Promise<string | null>) | null = null;

export const setupApiAuth = (getToken: () => Promise<string | null>) => {
  getClerkToken = getToken;
};

// Add Clerk token to requests
api.interceptors.request.use(async (config) => {
  console.log('🔐 API Interceptor - Getting token...');
  if (getClerkToken) {
    try {
      const token = await getClerkToken();
      console.log('🔐 Token retrieved:', token ? 'Yes ✓' : 'No ✗');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
        console.log('🔐 Authorization header set');
      } else {
        console.warn('⚠️ No token available');
      }
    } catch (error) {
      console.error('❌ Error getting token:', error);
    }
  } else {
    console.warn('⚠️ getClerkToken function not set up');
  }
  return config;
}, (error) => {
  console.error('❌ Request interceptor error:', error);
  return Promise.reject(error);
});

// Auth API
export const authAPI = {
  getCurrentUser: () => api.get('/auth/me'),
  test: () => api.get('/auth/test'),
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
  getBlockedSlots: () => api.get<any[]>('/business/blocked-slots'),
  createBlockedSlot: (data: { startTime: number; endTime: number; reason?: string }) =>
    api.post<any>('/business/blocked-slots', data),
  deleteBlockedSlot: (id: string) => api.delete(`/business/blocked-slots/${id}`),
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
