import { LucideIcon } from 'lucide-react';

export type ViewType =
  'dashboard' |
  'events' |
  'calendar' |
  'analytics' |
  'settings' |
  'users' |
  'roles' |
  'permissions' |
  'activity';

export type PublicViewType = 'landing' | 'login' | 'register' | 'about';

export interface Role {
  id?: string | number;
  name: string;
  permissions?: any[];
}

export interface User {
  id: string | number;
  name?: string;
  fullName?: string;
  email: string;
  role?: Role | string;
  avatar?: string;
}

export interface MetricData {
  label: string;
  value: string;
  change?: number;
  trend: number;
  trendLabel: string;
  icon: LucideIcon;
}

export interface Event {
  id: string | number;
  title: string;
  description?: string;
  startTime?: string; // maps to backend startTime
  endTime?: string;
  location?: string;
  visibility?: string;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
  approvalStatus?: string;
  approvedById?: string | number;
  approvedByName?: string;
  approvedAt?: string;
  eventStatus?: string; // maps to backend eventStatus
  deleted?: boolean;
  attendees?: number;
  capacity?: number;
  // For compatibility with old code
  name?: string; // alias for title
  date?: string; // alias for startTime
  status?: string; // alias for eventStatus
  maxAttendees?: number; // alias for capacity
  organizer?: string;
  image?: string;
  price?: string;
  category?: string;
}