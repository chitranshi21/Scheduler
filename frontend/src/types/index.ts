export interface User {
  id: string;
  email: string;
  userType: string;
  tenantId?: string;
  name?: string;
}

export interface LoginResponse {
  token: string;
  email: string;
  userType: string;
  userDetails: any;
}

export interface Tenant {
  id: string;
  name: string;
  slug: string;
  email: string;
  phone?: string;
  logoUrl?: string;
  description?: string;
  status: string;
  subscriptionTier: string;
  timezone: string; // IANA timezone identifier (e.g., "Europe/Amsterdam")
  createdAt: string;
}

export interface SessionType {
  id: string;
  tenantId: string;
  name: string;
  description?: string;
  durationMinutes: number;
  price: number;
  currency: string;
  capacity: number;
  category?: string;
  color?: string;
  isActive: boolean;
  meetingLink?: string;
  meetingPassword?: string;
}

export interface Customer {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
}

export interface Booking {
  id: string;
  tenantId: string;
  customerId: string;
  sessionTypeId: string;
  startTime: number; // Epoch timestamp in milliseconds
  endTime: number;   // Epoch timestamp in milliseconds
  status: string;
  participants: number;
  notes?: string;
  sessionType?: SessionType;
  customer?: Customer;
}

export interface BusinessHours {
  id: string;
  dayOfWeek: 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';
  startTime: string; // Format: "HH:mm"
  endTime: string;   // Format: "HH:mm"
  enabled: boolean;
}

export interface BlockedSlot {
  id: string;
  tenantId: string;
  startTime: number; // Epoch timestamp in milliseconds (LocalDateTime from backend)
  endTime: number;   // Epoch timestamp in milliseconds (LocalDateTime from backend)
  reason?: string;
  createdBy?: string;
  createdAt?: number;
  updatedAt?: number;
}
