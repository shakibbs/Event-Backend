import React, { useEffect, useState } from 'react';
import { Calendar } from '../components/Calendar';
import { Button } from '../components/ui/Button';
import { Plus } from 'lucide-react';
import { fetchEventsApi } from '../lib/api';
import type { Event } from '../lib/api';

export function CalendarView() {
  const [events, setEvents] = useState<Event[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setIsLoading(true);
    fetchEventsApi()
      .then((arr) => {
        setEvents(arr);
        setError(null);
      })
      .catch((err) => {
        setError(err && err.message ? err.message : 'Failed to load events');
      })
      .finally(() => setIsLoading(false));
  }, []);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-primary">Calendar</h1>
          <p className="text-text-secondary mt-1">
            View your event schedule at a glance.
          </p>
        </div>
        <Button leftIcon={<Plus className="h-4 w-4" />}>Add to Calendar</Button>
      </div>
      {error && (
        <div className="text-red-500 text-sm">{error}</div>
      )}
      <Calendar events={events} isLoading={isLoading} />
    </div>
  );
}