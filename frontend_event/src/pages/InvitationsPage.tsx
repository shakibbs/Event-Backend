

import React, { useEffect, useState } from 'react';
import { fetchEventsApi, fetchEventAttendeesApi } from '../lib/api';
import { Button } from '../components/ui/Button';
import { Sidebar } from '../components/layout/Sidebar';
import { useNavigate, useLocation } from 'react-router-dom';
import { sendBulkInvitationsApi } from '../lib/bulkInviteApi';

type InvitationsPageProps = {
  selectedEventId?: string | number | null;
  onBack?: () => void;
};

export default function InvitationsPage(props: InvitationsPageProps) {
  // Support selectedEventId from props or query params
  const location = useLocation();
  let selectedEventId: string | number | null = null;
  if (typeof props?.selectedEventId !== 'undefined' && props.selectedEventId !== null) {
    selectedEventId = props.selectedEventId;
  } else {
    const searchParams = new URLSearchParams(location.search);
    selectedEventId = searchParams.get('eventId');
  }
  const [events, setEvents] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  // Attendees per eventId
  const [attendees, setAttendees] = useState<Record<string, any[]>>({});
  const [loadingAttendees, setLoadingAttendees] = useState<Record<string, boolean>>({});
  const [inviteLoading, setInviteLoading] = useState<Record<string, boolean>>({});
  const [inviteError, setInviteError] = useState<Record<string, string | null>>({});
  const [inviteSuccess, setInviteSuccess] = useState<Record<string, boolean>>({});
  const navigate = useNavigate();

  useEffect(() => {
    setLoading(true);
    fetchEventsApi(0, 100)
      .then(setEvents)
      .catch((err) => setError(err.message || 'Failed to load events'))
      .finally(() => setLoading(false));
  }, []);

  // Fetch attendees for an event
  const handleShowAttendees = async (eventId: string | number) => {
    setLoadingAttendees((prev) => ({ ...prev, [eventId]: true }));
    try {
      const data = await fetchEventAttendeesApi(eventId);
      setAttendees((prev) => ({ ...prev, [eventId]: data }));
    } catch (e) {
      setAttendees((prev) => ({ ...prev, [eventId]: [] }));
    }
    setLoadingAttendees((prev) => ({ ...prev, [eventId]: false }));
  };

  // Handle CSV invite upload
  const handleInviteFile = async (eventId: string | number, file: File) => {
    setInviteLoading((prev) => ({ ...prev, [eventId]: true }));
    setInviteError((prev) => ({ ...prev, [eventId]: null }));
    setInviteSuccess((prev) => ({ ...prev, [eventId]: false }));
    try {
      await sendBulkInvitationsApi(eventId, file);
      setInviteSuccess((prev) => ({ ...prev, [eventId]: true }));
    } catch (e: any) {
      setInviteError((prev) => ({ ...prev, [eventId]: e?.message || 'Failed to send invitations' }));
    }
    setInviteLoading((prev) => ({ ...prev, [eventId]: false }));
  };

  return (
    <div className="flex h-screen w-full bg-surface-secondary font-sans text-text-primary">
      <main className="flex-1 flex flex-col h-screen overflow-hidden">
        <div className="flex items-center gap-4 p-4 bg-white border-b">
          <Button variant="secondary" onClick={props.onBack ? props.onBack : () => navigate('/events')}>
            ← Back
          </Button>
          <h1 className="text-2xl font-bold">Invitations Management</h1>
        </div>
        <div className="flex-1 overflow-y-auto p-4 sm:p-8">
          <p className="mb-6">Send invitations to all users for any event, upload a CSV of emails (optional), and track invitation status.</p>
          {loading && <div>Loading events...</div>}
          {error && <div className="text-red-500">{error}</div>}
          {!loading && !error && (
            <div className="space-y-8">
              {events.length === 0 && <div>No events found.</div>}
              {events.map(event => (
                <div
                  key={event.id}
                  className={`border rounded-xl p-4 shadow-sm bg-white ${selectedEventId == event.id ? 'ring-2 ring-primary' : ''}`}
                  ref={el => {
                    if (selectedEventId == event.id && el) {
                      setTimeout(() => el.scrollIntoView({ behavior: 'smooth', block: 'center' }), 200);
                    }
                  }}
                >
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-2">
                    <div>
                      <div className="font-semibold text-lg">{event.title}</div>
                      <div className="text-sm text-gray-500">{event.location} | {event.startTime}</div>
                    </div>
                    <div className="flex gap-2 mt-2 sm:mt-0">
                      <Button variant="primary" onClick={() => handleShowAttendees(event.id)}>
                        Show Attendees
                      </Button>
                      <label className="block">
                        <input
                          type="file"
                          accept=".csv"
                          className="hidden"
                          disabled={inviteLoading[event.id]}
                          onChange={e => {
                            const file = e.target.files?.[0];
                            if (file) handleInviteFile(event.id, file);
                            e.target.value = '';
                          }}
                        />
                        <Button
                          variant="secondary"
                          isLoading={inviteLoading[event.id]}
                          disabled={inviteLoading[event.id]}
                        >
                          Upload CSV
                        </Button>
                      </label>
                      {inviteError[event.id] && <span className="text-red-500 text-xs ml-2">{inviteError[event.id]}</span>}
                      {inviteSuccess[event.id] && <span className="text-green-600 text-xs ml-2">Invitations sent!</span>}
                    </div>
                  </div>
                  {/* Attendees tracking table/log */}
                  <div className="mt-2">
                    {loadingAttendees[event.id] ? (
                      <div className="text-sm text-gray-500">Loading attendees...</div>
                    ) : attendees[event.id] ? (
                      <AttendeesTable attendees={attendees[event.id]} />
                    ) : (
                      <div className="text-sm text-gray-600">Click "Show Attendees" to view the list and tracking/logs.</div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </main>
    </div>
  );
}

// Table to display attendees and their invitation status
function AttendeesTable({ attendees }: { attendees: any[] }) {
  if (!attendees.length) return <div className="text-gray-500">No attendees found for this event.</div>;
  return (
    <div>
      <div className="mb-2 font-medium">Total Attendees: {attendees.length}</div>
      <table className="w-full text-sm border mt-2">
        <thead>
          <tr className="bg-slate-100">
            <th className="px-2 py-1 text-left">Email</th>
            <th className="px-2 py-1 text-left">Status</th>
            <th className="px-2 py-1 text-left">Sent At</th>
            <th className="px-2 py-1 text-left">Responded At</th>
          </tr>
        </thead>
        <tbody>
          {attendees.map((att: any) => (
            <tr key={att.id}>
              <td className="px-2 py-1">{att.email}</td>
              <td className="px-2 py-1">
                {att.invitationStatus === 'PENDING' && <span className="text-yellow-600">Pending</span>}
                {att.invitationStatus === 'ACCEPTED' && <span className="text-green-700">Accepted</span>}
                {att.invitationStatus === 'DECLINED' && <span className="text-red-600">Declined</span>}
              </td>
              <td className="px-2 py-1">{att.invitationSentAt ? new Date(att.invitationSentAt).toLocaleString() : '-'}</td>
              <td className="px-2 py-1">{att.responseAt ? new Date(att.responseAt).toLocaleString() : '-'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
