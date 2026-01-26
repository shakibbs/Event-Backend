import { useEffect, useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { isAttendee } from '../utils/rolePermissions';
import { useNavigate } from 'react-router-dom';
import { MetricCard } from '../components/MetricCard';
import { DataTable } from '../components/DataTable';
import { fetchEventsApi, apiRequest } from '../lib/api';
import { Button } from '../components/ui/Button';
import { Plus, Download, Calendar, Users, User } from 'lucide-react';
import type { Event } from '../lib/api';

export function Dashboard() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [events, setEvents] = useState<Event[]>([]);
  const [stats, setStats] = useState({
    totalEvents: 0,
    totalAttendees: 0,
    totalUsers: 0,
    upcomingEvents: 0
  });
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadData = async () => {
      setIsLoading(true);
      try {
        // 1. Fetch Events
        const eventsData = await fetchEventsApi(0, 100); // Fetch max allowed per page
        setEvents(eventsData);

        // 2. Fetch Users
        let usersData: any[] = [];
        try {
          const uData = await apiRequest('/users?size=1000');
          usersData = Array.isArray(uData) ? uData : (uData.content || []);
        } catch (e) {
          console.warn('Failed to fetch users for dashboard stats', e);
        }

        // 3. Calculate Metrics
        const now = new Date();
        const upcoming = eventsData.filter(e => {
          const dateStr = e.startTime || e.date;
          return dateStr && new Date(dateStr) > now;
        }).length;

        const attendees = eventsData.reduce((acc, curr) => {
          const count = Array.isArray(curr.attendees) ? curr.attendees.length : (typeof curr.attendees === 'number' ? curr.attendees : 0);
          return acc + count;
        }, 0);

        setStats({
          totalEvents: eventsData.length,
          totalAttendees: attendees,
          totalUsers: usersData.length,
          upcomingEvents: upcoming
        });
        setError(null);
      } catch (err: any) {
        setError(err.message || 'Failed to load dashboard data');
      } finally {
        setIsLoading(false);
      }
    };

    loadData();
  }, []);

  const attendee = isAttendee(user);
  const realMetrics = attendee
    ? [
        {
          label: 'My Upcoming Events',
          value: events.filter(e => Array.isArray(e.attendees) && e.attendees.some((a: any) => a.id === user?.id) && new Date(e.startTime || e.date) > new Date()).length.toString(),
          trend: 0,
          trendLabel: 'Events you are attending',
          change: 0,
          icon: Calendar
        },
        {
          label: 'Invitations',
          value: events.filter(e => Array.isArray(e.invited) && e.invited.some((a: any) => a.id === user?.id)).length.toString(),
          trend: 0,
          trendLabel: 'Pending invitations',
          change: 0,
          icon: Users
        }
      ]
    : [
        {
          label: 'Total Events',
          value: stats.totalEvents.toString(),
          trend: 0,
          trendLabel: 'All time',
          change: 0,
          icon: Calendar
        },
        {
          label: 'Total Attendees',
          value: stats.totalAttendees.toLocaleString(),
          trend: 0,
          trendLabel: 'Confirmed attendees',
          change: 0,
          icon: Users
        },
        {
          label: 'Total Users',
          value: stats.totalUsers.toLocaleString(),
          trend: 0,
          trendLabel: 'Registered users',
          change: 0,
          icon: User
        },
        {
          label: 'Upcoming Events',
          value: stats.upcomingEvents.toString(),
          trend: 10,
          trendLabel: 'Future scheduled',
          change: 0,
          icon: Calendar
        },
      ];

  // For Admin/SuperAdmin: show latest events; for Attendee: show their events
  const recentEvents = attendee
    ? events.filter(e => Array.isArray(e.attendees) && e.attendees.some((a: any) => a.id === user?.id)).slice(0, 5)
    : events.slice(0, 5);

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-primary">Dashboard</h1>
          <p className="text-text-secondary mt-1">
            {attendee
              ? "Welcome! Here are your upcoming events and invitations."
              : "Welcome back, here's what's happening today."}
          </p>
        </div>
        {!attendee && (
          <div className="flex gap-3">
            <Button
              variant="secondary"
              leftIcon={<Download className="h-4 w-4" />}> 
              Export Report
            </Button>
            <Button leftIcon={<Plus className="h-4 w-4" />}>Create Event</Button>
          </div>
        )}
      </div>

      {/* Metrics Grid */}
      <div className={`grid grid-cols-1 ${attendee ? 'md:grid-cols-2' : 'sm:grid-cols-2 md:grid-cols-4'} gap-4`}>
        {realMetrics.map((metric, index) =>
          <MetricCard key={metric.label} data={metric} index={index} />
        )}
      </div>

      {/* Recent Activity */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-primary">{attendee ? "My Recent Events" : "Recent Events"}</h2>
          {!attendee && (
            <Button variant="ghost" size="sm" onClick={() => navigate('/events')}>
              View All
            </Button>
          )}
        </div>
        {error && (
          <div className="text-red-500 text-sm">{error}</div>
        )}
        <DataTable data={recentEvents} isLoading={isLoading} />
      </div>
    </div>
  );
}