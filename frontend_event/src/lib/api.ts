// Fetch attendees for a specific event (real backend)
// Fetch single event by ID
export async function fetchEventApi(eventId: string | number) {
  return apiRequest(`/events/${eventId}`, {
    method: 'GET',
  });
}

// Approve or reject event (unified action)
export async function approveEventApi(eventId: string | number, remarks: string = '') {
  return apiRequest(`/events/${eventId}/action`, {
    method: 'POST',
    body: JSON.stringify({ eventId: Number(eventId), action: 'APPROVE', remarks })
  });
}
export async function rejectEventApi(eventId: string | number, remarks: string) {
  return apiRequest(`/events/${eventId}/action`, {
    method: 'POST',
    body: JSON.stringify({ eventId: Number(eventId), action: 'REJECT', remarks })
  });
}

// Hold event
export async function holdEventApi(eventId: string | number) {
  return apiRequest(`/events/${eventId}/hold`, {
    method: 'POST'
  });
}

// Reactivate event
export async function reactivateEventApi(eventId: string | number) {
  return apiRequest(`/events/${eventId}/reactivate`, {
    method: 'POST'
  });
}

// Invite users to event (CSV file upload)
export async function inviteUserApi(eventId: string | number, file?: File) {
  const formData = new FormData();
  if (file) formData.append('file', file);
  
  // For multipart form data, we need to bypass the normal apiRequest
  const publicEndpoints = ['/auth/login', '/auth/register', '/events/public', '/events/respond'];
  const path = `/events/${eventId}/invite`;
  const isPublic = publicEndpoints.some(e => path.startsWith(e));
  
  let headers: Record<string, string> = {};
  if (!isPublic) {
    const token = localStorage.getItem('eventflow_token');
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
  }
  
  const response = await fetch(`http://localhost:8083/api${path}`, {
    method: 'POST',
    body: formData,
    headers, // Don't set Content-Type, let browser set multipart/form-data
    credentials: 'include',
  });
  
  let data;
  try {
    data = await response.json();
  } catch {
    data = {};
  }
  
  if (!response.ok) {
    const error: any = data;
    throw new Error(error.message || response.statusText);
  }
  
  return data;
}

// Fetch invited users for an event (assume endpoint exists)
export async function fetchInvitedUsersApi(eventId: string | number) {
  // If you have a real endpoint, update this path
  return apiRequest(`/events/${eventId}/invited`, {
    method: 'GET'
  });
}
export async function fetchEventAttendeesApi(eventId: string | number) {
  return apiRequest(`/events/${eventId}/attendees`, {
    method: 'GET',
  });
}
// Delete event by ID
export async function deleteEventApi(eventId: string | number) {
  return apiRequest(`/events/${eventId}`, {
    method: 'DELETE',
  });
}

// Update event
export async function updateEventApi(eventId: string | number, eventData: { title: string; description: string; startTime: string; endTime: string; location: string; visibility: string }) {
  return apiRequest(`/events/${eventId}`, {
    method: 'PUT',
    body: JSON.stringify(eventData)
  });
}
// Fetch public upcoming events for landing page (no auth required)
export async function fetchPublicEventsApi(): Promise<Event[]> {
  const data = await apiRequest<any>('/events/public', {
    method: 'GET',
  });
  if (Array.isArray(data)) return data;
  if (data && Array.isArray(data.events)) return data.events;
  return [];
}
const API_BASE_URL = 'http://localhost:8083/api';

// --- Types ---
export interface Event {
  id: number | string;
  title: string;
  description?: string;
  visibility?: string;
  status?: string;
  [key: string]: any;
}

export interface ApiError {
  message?: string;
  [key: string]: any;
}

// --- API Request Helper ---
export async function apiRequest<T = any>(path: string, options: RequestInit = {}): Promise<T> {
  // Attach JWT token if present and not a public endpoint
  const publicEndpoints = [
    '/auth/login',
    '/auth/register',
    '/events/public',
    '/events/respond'
  ];
  let headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };
  const isPublic = publicEndpoints.some(e => path.startsWith(e));
  if (!isPublic) {
    const token = localStorage.getItem('eventflow_token');
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
  }
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
    credentials: 'include',
  });
  let data;
  try {
    data = await response.json();
  } catch {
    data = {};
  }
  if (!response.ok) {
    const error: ApiError = data;
    throw new Error(error.message || response.statusText);
  }
  return data;
}

export async function loginApi(email: string, password: string) {
  return apiRequest('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password })
  });
}

export async function registerApi(name: string, email: string, password: string) {
  return apiRequest('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ name, email, password })
  });
}

// Fetch events for authenticated users (role/permission-based)
export async function fetchEventsApi(page = 0, size = 100): Promise<Event[]> {
  // Use /api/events for authenticated users, which is paginated
  const data = await apiRequest<any>(`/events?page=${page}&size=${size}`, {
    method: 'GET',
  });
  // Spring Data Page: { content: Event[], ... }
  if (data && Array.isArray(data.content)) return data.content;
  return [];
}
