import React from 'react';
import { motion } from 'framer-motion';
import type { Event } from '../lib/api';
import { Badge } from './ui/Badge';
import { Button } from './ui/Button';
import { MoreHorizontal, MapPin, Users, Calendar } from 'lucide-react';
import { formatDate, formatCurrency } from '../lib/utils';
interface DataTableProps {
  data: Event[];
  isLoading?: boolean;
  onEdit?: (event: Event) => void;
  onDelete?: (event: Event) => void;
  renderActions?: (event: Event) => React.ReactNode;
  onRowClick?: (event: Event) => void;
}
export function DataTable({ data, isLoading, onEdit, onDelete, renderActions, onRowClick }: DataTableProps) {
  // Debug: log the data being rendered
  // console.log('DataTable data:', data);
    const handleRowClick = (event: Event, e: React.MouseEvent) => {
      // Prevent click if action button was clicked
      if ((e.target as HTMLElement).closest('button')) return;
      if (typeof onRowClick === 'function') onRowClick(event);
    };
  if (isLoading) {
    return (
      <div className="w-full h-64 flex items-center justify-center bg-white rounded-xl border border-surface-border">
        <div className="animate-pulse flex flex-col items-center">
          <div className="h-4 w-32 bg-slate-200 rounded mb-4"></div>
          <div className="h-4 w-48 bg-slate-200 rounded"></div>
        </div>
      </div>);

  }
  if (data.length === 0) {
    return (
      <div className="w-full h-64 flex flex-col items-center justify-center bg-white rounded-xl border border-surface-border text-center p-8">
        <div className="w-12 h-12 bg-slate-100 rounded-full flex items-center justify-center mb-4">
          <Calendar className="h-6 w-6 text-slate-400" />
        </div>
        <h3 className="text-lg font-medium text-primary mb-1">
          No events found
        </h3>
        <p className="text-text-secondary text-sm max-w-xs">
          Try adjusting your search or filters to find what you're looking for.
        </p>
      </div>);

  }
  return (
    <div className="w-full overflow-hidden bg-white rounded-xl border border-surface-border shadow-sm">
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b border-surface-border bg-slate-50/50">
              <th className="py-4 px-6 text-xs font-semibold text-text-secondary uppercase tracking-wider">
                Event Name
              </th>
              <th className="py-4 px-6 text-xs font-semibold text-text-secondary uppercase tracking-wider">
                Date & Time
              </th>
              <th className="py-4 px-6 text-xs font-semibold text-text-secondary uppercase tracking-wider">
                Location
              </th>
              <th className="py-4 px-6 text-xs font-semibold text-text-secondary uppercase tracking-wider">
                Attendees
              </th>
              <th className="py-4 px-6 text-xs font-semibold text-text-secondary uppercase tracking-wider">
                Status
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-surface-border">
            {data.map((event, index) =>
            <motion.tr
              key={event.id}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.2, delay: index * 0.05 }}
              className="group hover:bg-slate-50 transition-colors duration-150 cursor-pointer"
              onClick={e => handleRowClick(event, e)}>

                <td className="py-4 px-6">
                  <div className="flex flex-col">
                    <span
                      className="font-medium text-primary text-sm underline cursor-pointer"
                      onClick={e => {
                        e.stopPropagation();
                        console.log('Event name clicked:', event.id);
                        if (typeof onRowClick === 'function') onRowClick(event);
                      }}
                    >
                      {event.title || event.name}
                    </span>
                    <span className="text-xs text-text-muted mt-0.5">
                      {event.organizer ? `Org: ${event.organizer}` : ''}
                    </span>
                  </div>
                </td>
                <td className="py-4 px-6">
                  <div className="flex items-center text-sm text-text-secondary">
                    <Calendar className="h-3.5 w-3.5 mr-2 text-text-muted" />
                    {(() => {
                      // Prefer startTime, fallback to date, then createdAt
                      const dateVal = event.startTime || event.date || event.createdAt || null;
                      if (!dateVal) return 'N/A';
                      try {
                        return formatDate(dateVal);
                      } catch {
                        return 'Invalid date';
                      }
                    })()}
                  </div>
                </td>
                <td className="py-4 px-6">
                  <div className="flex items-center text-sm text-text-secondary">
                    <MapPin className="h-3.5 w-3.5 mr-2 text-text-muted" />
                    <span className="truncate max-w-[150px]">
                      {event.location}
                    </span>
                  </div>
                </td>
                <td className="py-4 px-6">
                  <div className="flex items-center text-sm text-text-secondary tabular-nums">
                    <Users className="h-3.5 w-3.5 mr-2 text-text-muted" />
                    {event.attendees ?? '-'}{' '}
                    <span className="text-text-muted mx-1">/</span>{' '}
                    {event.capacity ?? event.maxAttendees ?? '-'}
                  </div>
                  <div className="w-24 h-1 bg-slate-100 rounded-full mt-1.5 overflow-hidden">
                    <div
                      className="h-full bg-accent rounded-full"
                      style={{
                        width: event.attendees && (event.capacity ?? event.maxAttendees)
                          ? `${(event.attendees / (event.capacity ?? event.maxAttendees)) * 100}%`
                          : '0%'
                      }}
                    />

                  </div>
                </td>
                <td className="py-4 px-6">
                  <Badge status={event.eventStatus || event.status} />
                </td>
              </motion.tr>
            )}
          </tbody>
        </table>
      </div>
    </div>);

}