import React, { useState, useEffect } from 'react';
import { DataTable } from '../components/DataTable';
import { FilterBar } from '../components/FilterBar';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { Event } from '../lib/api';
import { Button } from '../components/ui/Button';
import { Plus } from 'lucide-react';
import { fetchEventsApi, apiRequest } from '../lib/api';

// Local type definitions
interface FilterState {
  search: string;
  status: string;
  dateRange: string;
  ownOnly?: boolean;
}

export function EventsList() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [filters, setFilters] = useState<FilterState>({
    search: '',
    status: 'all',
    dateRange: 'all'
  });
  // Modal state for create/edit
  const [showModal, setShowModal] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [selectedEvent, setSelectedEvent] = useState<Event | null>(null);
  const [eventForm, setEventForm] = useState<Partial<Event>>({ title: '', location: '', startTime: '', endTime: '', description: '' });
  // CRUD Handlers
  const handleCreate = () => {
    setShowModal(true);
    setEditMode(false);
    setSelectedEvent(null);
    setEventForm({ title: '', location: '', startTime: '', endTime: '', description: '' });
  };

  const handleDelete = async (event: Event) => {
    if (window.confirm(`Are you sure you want to delete event "${event.title || (event as any).name}"?`)) {
      try {
        const api = await import('../lib/api');
        await api.deleteEventApi(event.id);
        setEvents(prev => prev.filter(e => e.id !== event.id));
      } catch (err: any) {
        alert(err.message || 'Failed to delete event');
      }
    }
  };

  const handleModalSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      // Format startTime and endTime as 'yyyy-MM-dd HH:mm:ss' for backend
      const formatDateTime = (dt: string | undefined) => {
        if (!dt) return '';
        const d = new Date(dt);
        const pad = (n: number) => n.toString().padStart(2, '0');
        return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
      };
      const payload = {
        ...eventForm,
        startTime: formatDateTime(eventForm.startTime),
        endTime: formatDateTime(eventForm.endTime)
      };
      if (editMode && selectedEvent) {
        await apiRequest(`/events/${selectedEvent.id}`, {
          method: 'PUT',
          body: JSON.stringify(payload)
        });
      } else {
        await apiRequest('/events', {
          method: 'POST',
          body: JSON.stringify(payload)
        });
      }
      setShowModal(false);
      setEditMode(false);
      setSelectedEvent(null);
      setEventForm({ title: '', location: '', startTime: '', endTime: '', description: '' });
      // Refresh events
      setIsLoading(true);
      fetchEventsApi(0, 100)
        .then((data) => {
          setEvents(Array.isArray(data) ? data : []);
          setError(null);
        })
        .catch((err) => {
          setError(err.message || 'Failed to load events');
        })
        .finally(() => setIsLoading(false));
    } catch (err: any) {
      alert(err.message || 'Failed to save event');
    }
  };
  const [events, setEvents] = useState<Event[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setIsLoading(true);
    fetchEventsApi(0, 100)
      .then((data) => {
        setEvents(Array.isArray(data) ? data : []);
        setError(null);
      })
      .catch((err) => {
        setError(err.message || 'Failed to load events');
      })
      .finally(() => setIsLoading(false));
  }, []);

  const filteredEvents = events.filter((event) => {
    // Use title for search, fallback to name, and location
    const matchesSearch =
      (event.title?.toLowerCase().includes(filters.search.toLowerCase()) ||
        event.name?.toLowerCase().includes(filters.search.toLowerCase()) ||
        event.location?.toLowerCase().includes(filters.search.toLowerCase()));

    // Use eventStatus for filtering, fallback to status
    const eventStatus = (event.eventStatus || event.status || '').toLowerCase();
    const filterStatus = filters.status.toLowerCase();
    let matchesStatus = true;
    if (filterStatus !== 'all') {
      if (filterStatus === 'ongoing') {
        // Accept both 'ongoing' and 'active' as ongoing for compatibility
        matchesStatus = eventStatus === 'ongoing' || eventStatus === 'active';
      } else {
        matchesStatus = eventStatus === filterStatus;
      }
    }

    // Use startTime for date, fallback to date
    let matchesDate = true;
    const eventDateStr = event.startTime || event.date;
    const eventDate = eventDateStr ? new Date(eventDateStr) : null;
    const now = new Date();
    if (filters.dateRange === 'upcoming' && eventDate) {
      matchesDate = eventDate >= now;
    } else if (filters.dateRange === 'past' && eventDate) {
      matchesDate = eventDate < now;
    }

    // Filter own events if enabled
    let matchesOwnership = true;
    if (filters.ownOnly && user) {
      const creatorId = event.createdBy || event.organizer?.id || event.organizerId || event.organizer;
      const isOrganizer = event.createdBy === user.fullName || 
                         event.createdBy === user.name || 
                         event.createdBy === user.id || 
                         event.createdBy === user.email ||
                         creatorId == user.id ||
                         (typeof creatorId === 'object' && creatorId?.id == user.id);
      matchesOwnership = isOrganizer;
    }

    return matchesSearch && matchesStatus && matchesDate && matchesOwnership;
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-primary">Events</h1>
          <p className="text-text-secondary mt-1">
            Manage and track all your organization's events.
          </p>
        </div>
        <div className="flex gap-2">
          {/* Show "Own Events" toggle for Admin and Organizer */}
          {(typeof user?.role === 'string' ? user?.role : (user?.role as any)?.name) !== 'Attendee' && (
            <Button 
              variant={filters.ownOnly ? 'primary' : 'outline'}
              onClick={() => setFilters(prev => ({ ...prev, ownOnly: !prev.ownOnly }))}
            >
              {filters.ownOnly ? 'My Events' : 'All Events'}
            </Button>
          )}
          <Button leftIcon={<Plus className="h-4 w-4" />} onClick={handleCreate}>New Event</Button>
        </div>
      </div>

      <FilterBar
        filters={filters}
        onFilterChange={(key: string | number | symbol, value: any) => {
          setFilters((prev: FilterState) => ({
            ...prev,
            [key as string]: value
          }));
        }}
        onExport={() => alert('Exporting data...')}
      />

      {error && (
        <div className="text-red-500 text-sm">{error}</div>
      )}
      <DataTable
        data={filteredEvents}
        isLoading={isLoading}
        onDelete={handleDelete}
        onRowClick={(event) => {
          console.log('EventsList onRowClick:', event.id);
          navigate(`/event-management/${event.id}`);
        }}
      />



      {/* Modal for add/edit event */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30">
          <div className="bg-white rounded-xl shadow-lg p-8 w-full max-w-md relative">
            <button onClick={() => {
              setShowModal(false);
              setEditMode(false);
              setSelectedEvent(null);
            }} className="absolute top-3 right-3 text-slate-400 hover:text-slate-700">&times;</button>
            <h3 className="text-lg font-bold mb-4">{editMode ? 'Edit Event' : 'Add New Event'}</h3>
            <form onSubmit={handleModalSubmit} className="space-y-4">
              <input
                className="w-full border rounded px-3 py-2"
                value={eventForm.title}
                onChange={e => setEventForm({ ...eventForm, title: e.target.value })}
                placeholder="Event Title"
                required
              />
              <input
                className="w-full border rounded px-3 py-2"
                value={eventForm.location}
                onChange={e => setEventForm({ ...eventForm, location: e.target.value })}
                placeholder="Location"
                required
              />
              <input
                className="w-full border rounded px-3 py-2"
                type="datetime-local"
                value={eventForm.startTime}
                onChange={e => setEventForm({ ...eventForm, startTime: e.target.value })}
                placeholder="Start Time"
                required
              />
              <input
                className="w-full border rounded px-3 py-2"
                type="datetime-local"
                value={eventForm.endTime}
                onChange={e => setEventForm({ ...eventForm, endTime: e.target.value })}
                placeholder="End Time"
                required
              />
              <textarea
                className="w-full border rounded px-3 py-2"
                value={eventForm.description}
                onChange={e => setEventForm({ ...eventForm, description: e.target.value })}
                placeholder="Description"
                rows={3}
              />
              <div className="flex justify-end gap-2">
                <Button type="button" onClick={() => {
                  setShowModal(false);
                  setEditMode(false);
                  setSelectedEvent(null);
                }} className="bg-slate-200 text-slate-700">Cancel</Button>
                <Button type="submit" className="bg-primary text-white">{editMode ? 'Save Changes' : 'Add Event'}</Button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="flex items-center justify-between text-sm text-text-secondary mt-4">
        <p>
          Showing {filteredEvents.length} of {events.length} events
        </p>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" disabled>
            Previous
          </Button>
          <Button variant="outline" size="sm" disabled>
            Next
          </Button>
        </div>
      </div>
    </div>
  );
}