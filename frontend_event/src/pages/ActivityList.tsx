

import React, { useEffect, useState } from 'react';
import { apiRequest } from '../lib/api';
import { useAuth } from '../context/AuthContext';
import { Button } from '../components/ui/Button';
import { LogIn, LogOut, Calendar, PlusCircle, Edit3, Trash2, Lock, User, Award, Shield, CheckCircle, XCircle } from 'lucide-react';

const ACTIVITY_TYPES = [
  'USER_LOGIN', 'USER_LOGOUT', 'EVENT_CREATED', 'EVENT_UPDATED', 'EVENT_DELETED',
  'PASSWORD_CHANGED', 'ROLE_ASSIGNED', 'ROLE_REVOKED', 'PERMISSION_GRANTED', 'PERMISSION_REVOKED',
  'USER_CREATED', 'USER_UPDATED', 'USER_DELETED'
];

function iconForType(type: string) {
  switch (type) {
    case 'USER_LOGIN': return <LogIn className="w-5 h-5" />;
    case 'USER_LOGOUT': return <LogOut className="w-5 h-5" />;
    case 'EVENT_CREATED': return <PlusCircle className="w-5 h-5" />;
    case 'EVENT_UPDATED': return <Edit3 className="w-5 h-5" />;
    case 'EVENT_DELETED': return <Trash2 className="w-5 h-5" />;
    case 'PASSWORD_CHANGED': return <Lock className="w-5 h-5" />;
    case 'ROLE_ASSIGNED': return <Award className="w-5 h-5" />;
    case 'ROLE_REVOKED': return <Shield className="w-5 h-5" />;
    case 'PERMISSION_GRANTED': return <CheckCircle className="w-5 h-5" />;
    case 'PERMISSION_REVOKED': return <XCircle className="w-5 h-5" />;
    case 'USER_CREATED': return <User className="w-5 h-5" />;
    case 'USER_UPDATED': return <Edit3 className="w-5 h-5" />;
    case 'USER_DELETED': return <Trash2 className="w-5 h-5" />;
    default: return <Calendar className="w-5 h-5" />;
  }
}

function formatDate(dateStr: string) {
  if (!dateStr) return 'Unknown date';
  const d = new Date(dateStr);
  return d.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}

function UserAvatar({ name }: { name: string }) {
  const initials = name ? name.split(' ').map((n: string) => n[0]).join('').toUpperCase().slice(0, 2) : '?';
  return (
    <span className="inline-flex items-center justify-center w-6 h-6 rounded-full bg-accent text-white font-semibold text-xs">
      {initials}
    </span>
  );
}


export function ActivityList() {
  const { user } = useAuth();
  const [activities, setActivities] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [users, setUsers] = useState<any[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [activityType, setActivityType] = useState<string>('');
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');

  // Fetch all users for SuperAdmin
  useEffect(() => {
    if (user && user.roles && user.roles.includes('SuperAdmin')) {
      apiRequest('/users?size=1000')
        .then((data) => {
          setUsers(Array.isArray(data) ? data : (data.content || []));
        })
        .catch(() => setUsers([]));
    }
  }, [user]);

  // Build API URL based on filters and role
  function buildApiUrl() {
    // SuperAdmin: can filter by user, type, date
    if (user && user.roles && user.roles.includes('SuperAdmin')) {
      if (activityType) {
        return `/history/activity/type/${activityType}` + (selectedUserId ? `?userId=${selectedUserId}` : '');
      }
      if (startDate && endDate) {
        let url = `/history/activity/range?startDate=${encodeURIComponent(startDate)}&endDate=${encodeURIComponent(endDate)}`;
        if (selectedUserId) url += `&userId=${selectedUserId}`;
        return url;
      }
      if (selectedUserId) {
        return `/history/activity?userId=${selectedUserId}`;
      }
      return '/history/activity?all=true';
    }
    // Admin/Attendee: only own activity, can filter by type/date
    if (activityType) {
      return `/history/activity/type/${activityType}`;
    }
    if (startDate && endDate) {
      return `/history/activity/range?startDate=${encodeURIComponent(startDate)}&endDate=${encodeURIComponent(endDate)}`;
    }
    return '/history/activity';
  }

  // Fetch activities when filters change
  useEffect(() => {
    setLoading(true);
    setError(null);
    const url = buildApiUrl();
    apiRequest(url)
      .then(setActivities)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedUserId, activityType, startDate, endDate, user]);

  // Reset filters when user changes
  useEffect(() => {
    setActivityType('');
    setStartDate('');
    setEndDate('');
    setSelectedUserId(null);
  }, [user]);

  if (loading) return <div>Loading activity...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div className="min-h-screen bg-surface p-4 md:p-8">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-primary mb-2">User Activity History</h1>
          <p className="text-text-secondary">Track and monitor all user activities and system events</p>
        </div>

        {/* Filter Bar */}
        <div className="bg-surface rounded-xl shadow-md p-6 border border-surface-tertiary mb-8 space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* SuperAdmin user filter */}
            {user && user.roles && user.roles.includes('SuperAdmin') && (
              <div className="flex flex-col">
                <label htmlFor="userFilter" className="text-sm font-semibold text-primary mb-2">User</label>
                <select
                  id="userFilter"
                  className="border border-surface-tertiary rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent focus:border-transparent transition-all"
                  value={selectedUserId || ''}
                  onChange={e => setSelectedUserId(e.target.value || null)}
                >
                  <option value="">All users</option>
                  {users.map((u: any) => (
                    <option key={u.id} value={u.id}>{u.name || u.username || u.email || `User #${u.id}`}</option>
                  ))}
                </select>
              </div>
            )}
            {/* Activity type filter */}
            <div className="flex flex-col">
              <label htmlFor="typeFilter" className="text-sm font-semibold text-primary mb-2">Activity Type</label>
              <select
                id="typeFilter"
                className="border border-surface-tertiary rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent focus:border-transparent transition-all"
                value={activityType}
                onChange={e => setActivityType(e.target.value)}
              >
                <option value="">All types</option>
                {ACTIVITY_TYPES.map(type => (
                  <option key={type} value={type}>{type.replace(/_/g, ' ')}</option>
                ))}
              </select>
            </div>
            {/* Date range filter */}
            <div className="flex flex-col">
              <label htmlFor="startDate" className="text-sm font-semibold text-primary mb-2">From Date</label>
              <input
                id="startDate"
                type="datetime-local"
                className="border border-surface-tertiary rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent focus:border-transparent transition-all"
                value={startDate}
                onChange={e => setStartDate(e.target.value)}
                max={endDate || undefined}
              />
            </div>
            <div className="flex flex-col">
              <label htmlFor="endDate" className="text-sm font-semibold text-primary mb-2">To Date</label>
              <input
                id="endDate"
                type="datetime-local"
                className="border border-surface-tertiary rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent focus:border-transparent transition-all"
                value={endDate}
                onChange={e => setEndDate(e.target.value)}
                min={startDate || undefined}
              />
            </div>
          </div>
          <div className="flex gap-3 pt-2">
            <Button
              onClick={() => { setStartDate(''); setEndDate(''); setActivityType(''); setSelectedUserId(null); }}
              variant="secondary"
              size="sm"
            >
              Clear Filters
            </Button>
          </div>
        </div>

        {/* Activity List */}
        <div className="space-y-4">
          {loading && <div className="text-text-secondary text-center py-12">Loading activity...</div>}
          {error && <div className="text-red-500 text-center py-12">Error: {error}</div>}
          {!loading && !error && activities.length === 0 && (
            <div className="text-text-secondary text-center py-12 flex flex-col items-center bg-surface-secondary rounded-xl border border-surface-tertiary p-8">
              <Calendar className="w-12 h-12 text-surface-tertiary mb-4" />
              <p className="text-sm">No activity found.</p>
            </div>
          )}
          {activities.map((a: any, i: number) => (
            <div
              key={a.id || i}
              className="activity-card flex items-start gap-4 p-6 bg-surface rounded-xl shadow-sm border border-surface-tertiary hover:shadow-md transition-all"
            >
              <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-accent/10 text-accent flex-shrink-0">
                {iconForType(a.activityTypeCode)}
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-2">
                  <span className="text-sm font-semibold text-primary">{a.activityTypeName || a.activityTypeCode || 'Activity'}</span>
                  {a.username && (
                    <span className="flex items-center gap-1 text-xs text-text-secondary">
                      <UserAvatar name={a.username} />
                      <span>{a.username}</span>
                      {a.role && <span className="ml-1 px-2 py-0.5 rounded bg-surface-secondary text-text-secondary text-xs">{a.role}</span>}
                    </span>
                  )}
                </div>
                <p className="text-sm text-text-primary mb-2">
                  {a.description || <span className="italic text-text-secondary">No description</span>}
                </p>
                <div className="flex flex-wrap items-center gap-3 text-xs text-text-secondary">
                  <span>{formatDate(a.activityDate)}</span>
                  {a.ip && <span>• IP: {a.ip}</span>}
                  {a.deviceId && <span>• Device: {a.deviceId}</span>}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
