

import { useEffect, useState, useRef } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useParams, useNavigate } from 'react-router-dom';
import { Button } from '../components/ui/Button';
import { fetchEventApi, deleteEventApi, approveEventApi, rejectEventApi, holdEventApi, reactivateEventApi, fetchEventAttendeesApi, inviteUserApi, updateEventApi } from '../lib/api';
import { Calendar, MapPin, Info, AtSign, Users, Edit, Trash2, CheckCircle, XCircle, PauseCircle, RefreshCw, Mail } from 'lucide-react';

export default function EventManagement() {
  const { user } = useAuth();
  const { eventId } = useParams();
  const navigate = useNavigate();
  const [event, setEvent] = useState<any>(null);
  const [attendees, setAttendees] = useState<any[]>([]);
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [inviteFile, setInviteFile] = useState<File | null>(null);
  const [inviteLoading, setInviteLoading] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [editLoading, setEditLoading] = useState(false);
  const [editForm, setEditForm] = useState({
    title: '',
    description: '',
    startTime: '',
    endTime: '',
    location: '',
    visibility: 'PUBLIC'
  });
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!eventId) return;
    setLoading(true);
    Promise.all([
      fetchEventApi(eventId),
      fetchEventAttendeesApi(eventId)
    ])
      .then(([eventData, attendeesData]) => {
        // Check access permissions before loading event details
        const roleName = typeof user?.role === 'string' ? user?.role : (user?.role && typeof user?.role === 'object' ? (user?.role as any).name : '');
        
        // Check if user can access this event
        let canAccess = false;
        if (roleName === 'SuperAdmin') {
          canAccess = true;
        } else if (roleName === 'Admin') {
          // Admin can only access events they organize (created)
          const creatorId = eventData.createdBy || eventData.organizer?.id || eventData.organizerId || eventData.organizer;
          const userComparison = 
            eventData.createdBy === user?.fullName || 
            eventData.createdBy === user?.name || 
            eventData.createdBy === user?.id || 
            eventData.createdBy === user?.email ||
            creatorId == user?.id || // Allow numeric ID comparison
            (typeof creatorId === 'object' && creatorId?.id == user?.id);
          
          if (!userComparison) {
            console.warn('Admin access denied for event:', eventData);
            setError("You don't have permission to access this event");
            setLoading(false);
            return;
          }
          canAccess = true;
        } else if (roleName === 'Attendee') {
          // Attendee can only access public events or events they're invited to
          const isPublic = eventData.visibility === 'PUBLIC';
          const isAttendee = Array.isArray(attendeesData) && 
                            attendeesData.some((a: any) => a.email === user?.email);
          if (!isPublic && !isAttendee) {
            setError("You don't have permission to access this event");
            setLoading(false);
            return;
          }
          canAccess = true;
        }

        if (!canAccess) {
          setError("You don't have permission to access this event");
          setLoading(false);
          return;
        }

        setEvent(eventData);
        setEditForm({
          title: eventData.title || '',
          description: eventData.description || '',
          startTime: eventData.startTime || '',
          endTime: eventData.endTime || '',
          location: eventData.location || '',
          visibility: eventData.visibility || 'PUBLIC'
        });
        setAttendees(Array.isArray(attendeesData) ? attendeesData : (attendeesData && typeof attendeesData === 'object' && Array.isArray(attendeesData.content) ? attendeesData.content : []));
        setError(null);
      })
      .catch((err) => setError(err.message || 'Failed to load event data'))
      .finally(() => setLoading(false));
  }, [eventId, user]);


  const handleDelete = async () => {
    if (!eventId) return;
    if (window.confirm('Are you sure you want to delete this event?')) {
      try {
        await deleteEventApi(eventId);
        alert('Event deleted successfully!');
        navigate('/events');
      } catch (err: any) {
        console.error('Delete event error:', err);
        alert('Failed to delete event: ' + (err?.message || 'Unknown error'));
      }
    }
  };

  const handleEdit = async () => {
    setShowEditModal(true);
  };

  const handleHold = async () => {
    if (!eventId) return;
    try {
      await holdEventApi(eventId);
      alert('Event held successfully!');
      window.location.reload();
    } catch (err: any) {
      console.error('Hold event error:', err);
      alert('Failed to hold event: ' + (err?.message || 'Unknown error'));
    }
  };

  const handleReactivate = async () => {
    if (!eventId) return;
    try {
      await reactivateEventApi(eventId);
      alert('Event reactivated successfully!');
      window.location.reload();
    } catch (err: any) {
      console.error('Reactivate event error:', err);
      alert('Failed to reactivate event: ' + (err?.message || 'Unknown error'));
    }
  };

  const handleApprove = async () => {
    if (!eventId) return;
    try {
      await approveEventApi(eventId);
      alert('Event approved successfully!');
      window.location.reload();
    } catch (err: any) {
      console.error('Approve event error:', err);
      alert('Failed to approve event: ' + (err?.message || 'Unknown error'));
    }
  };

  const handleReject = async () => {
    if (!eventId) return;
    try {
      await rejectEventApi(eventId, 'Rejected by admin');
      alert('Event rejected successfully!');
      window.location.reload();
    } catch (err: any) {
      console.error('Reject event error:', err);
      alert('Failed to reject event: ' + (err?.message || 'Unknown error'));
    }
  };

  const handleInvite = () => {
    setShowInviteModal(true);
  };

  const handleInviteSubmit = async () => {
    if (!eventId) return;
    setInviteLoading(true);
    try {
      await inviteUserApi(eventId, inviteFile || undefined);
      alert('Invitations sent!');
      setShowInviteModal(false);
      setInviteFile(null);
    } catch (err: any) {
      alert('Failed to send invitations: ' + (err?.message || 'Unknown error'));
    } finally {
      setInviteLoading(false);
    }
  };

  const handleEditSubmit = async () => {
    if (!eventId) return;
    setEditLoading(true);
    try {
      await updateEventApi(eventId, editForm);
      alert('Event updated successfully!');
      setShowEditModal(false);
      window.location.reload();
    } catch (err: any) {
      alert('Failed to update event: ' + (err?.message || 'Unknown error'));
    } finally {
      setEditLoading(false);
    }
  };



  if (loading) return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-50 to-slate-100">
      <div className="text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto mb-4"></div>
        <p className="text-slate-600 font-medium">Loading event details...</p>
      </div>
    </div>
  );

  if (error) return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-50 to-slate-100 p-4">
      <div className="bg-white rounded-xl shadow-lg border border-red-200 max-w-md w-full p-8 text-center">
        <div className="flex justify-center mb-4">
          <div className="h-12 w-12 bg-red-100 rounded-full flex items-center justify-center">
            <svg className="h-6 w-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4v.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
        </div>
        <h2 className="text-xl font-bold text-slate-900 mb-2">Access Denied</h2>
        <p className="text-slate-600 mb-6">{error}</p>
        <button 
          onClick={() => navigate('/events')}
          className="bg-blue-500 hover:bg-blue-600 text-white font-medium py-2 px-6 rounded-lg transition-colors"
        >
          Back to Events
        </button>
      </div>
    </div>
  );

  if (!event) return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-50 to-slate-100 p-4">
      <div className="bg-white rounded-xl shadow-lg border border-amber-200 max-w-md w-full p-8 text-center">
        <div className="flex justify-center mb-4">
          <div className="h-12 w-12 bg-amber-100 rounded-full flex items-center justify-center">
            <svg className="h-6 w-6 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3v.5m0 3v.5" />
            </svg>
          </div>
        </div>
        <h2 className="text-xl font-bold text-slate-900 mb-2">Event Not Found</h2>
        <p className="text-slate-600 mb-6">The event you're looking for doesn't exist or has been deleted.</p>
        <button 
          onClick={() => navigate('/events')}
          className="bg-blue-500 hover:bg-blue-600 text-white font-medium py-2 px-6 rounded-lg transition-colors"
        >
          Back to Events
        </button>
      </div>
    </div>
  );

  let canManageEvent = false;
  let canViewEvent = false;
  let showEdit = false, showDelete = false, showApprove = false, showReject = false, showHold = false, showReactivate = false, showInvite = false;
  
  if (user && event) {
    // Extract role name - handle both string and object structures
    const roleName = typeof user.role === 'string' ? user.role : (user.role && typeof user.role === 'object' ? (user.role as any).name : '');
    
    if (roleName === 'SuperAdmin') {
      // SuperAdmin can manage and view all events
      canManageEvent = true;
      canViewEvent = true;
    } else if (roleName === 'Admin') {
      // Admin can manage only their own events but view all events
      const creatorId = event.createdBy || event.organizer?.id || event.organizerId || event.organizer;
      canManageEvent = event.createdBy === user.fullName || 
                      event.createdBy === user.name || 
                      event.createdBy === user.id || 
                      event.createdBy === user.email ||
                      creatorId == user.id ||
                      (typeof creatorId === 'object' && creatorId?.id == user.id);
      canViewEvent = true; // Admin can view all events with event.view.all permission
    } else if (roleName === 'Attendee') {
      // Attendee can view public events or events they are invited to
      if (event.visibility === 'PUBLIC') {
        canViewEvent = true;
      } else if (Array.isArray(attendees) && attendees.some((a: any) => a.email === user.email)) {
        canViewEvent = true;
      }
    }
    
    showEdit = canManageEvent && (event.approvalStatus === 'PENDING' || event.approvalStatus === 'APPROVED' || event.eventStatus === 'INACTIVE' || event.eventStatus === 'UPCOMING');
    showDelete = canManageEvent && (event.approvalStatus === 'PENDING' || event.approvalStatus === 'REJECTED' || event.approvalStatus === 'APPROVED' || event.eventStatus === 'INACTIVE' || event.eventStatus === 'UPCOMING' || event.eventStatus === 'HOLD');
    showApprove = canManageEvent && event.approvalStatus === 'PENDING';
    showReject = canManageEvent && event.approvalStatus === 'PENDING';
    showHold = canManageEvent && event.approvalStatus === 'APPROVED' && (event.eventStatus !== 'INACTIVE' && event.eventStatus !== 'HOLD');
    showReactivate = canManageEvent && (event.eventStatus === 'INACTIVE' || event.eventStatus === 'HOLD');
    showInvite = canManageEvent && event.approvalStatus === 'APPROVED' && (event.eventStatus !== 'INACTIVE' && event.eventStatus !== 'HOLD');
  }

  if (!canViewEvent) return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 p-4 md:p-8">
      <div className="max-w-5xl mx-auto">
        <Button onClick={() => navigate('/events')} variant="secondary" size="sm" className="mb-6">← Back to Events</Button>
        <div className="bg-white rounded-xl shadow-lg border border-red-200 p-8 text-center">
          <XCircle className="w-12 h-12 text-red-500 mx-auto mb-4" />
          <h2 className="text-2xl font-bold text-red-600 mb-2">Access Denied</h2>
          <p className="text-slate-600">You do not have permission to view this event.</p>
        </div>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-surface p-4 md:p-8">
      <div className="max-w-7xl mx-auto">
        {/* Header Section */}
        <div className="flex items-center justify-between mb-8">
          <Button onClick={() => navigate('/events')} variant="secondary" size="md" className="flex items-center gap-2">
            <span>←</span> Back
          </Button>
          <div className="flex-1" />
        </div>

        {/* Main Grid Layout */}
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* Event Details and Attendees */}
          <div className="lg:col-span-3 space-y-6">
            {/* Event Header Card */}
            <div className="bg-surface rounded-xl shadow-md p-0 overflow-hidden border border-surface-tertiary hover:shadow-lg transition-all">
              {/* Gradient Header */}
              <div className="bg-gradient-to-r from-slate-800 to-slate-900 p-8 text-white">
                <h2 className="text-2xl font-bold mb-2 text-white">{event.title}</h2>
                <p className="text-gray-300 text-sm">{event.organizer && `Organized by ${event.organizer}`}</p>
              </div>

              {/* Status Badges */}
              <div className="px-8 pt-6 pb-2 flex flex-wrap gap-3 items-center border-b border-surface-tertiary">
                <span className={`inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-bold ${
                  event.eventStatus === 'COMPLETED' ? 'bg-emerald-100 text-emerald-700' :
                  event.eventStatus === 'ACTIVE' ? 'bg-blue-100 text-blue-700' :
                  event.eventStatus === 'INACTIVE' ? 'bg-slate-100 text-slate-700' :
                  event.eventStatus === 'PENDING' ? 'bg-amber-100 text-amber-700' :
                  event.eventStatus === 'HOLD' ? 'bg-rose-100 text-rose-700' :
                  'bg-slate-100 text-slate-700'
                }`}>
                  Status: {event.eventStatus}
                </span>
                <span className={`inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-bold ${
                  event.approvalStatus === 'APPROVED' ? 'bg-green-100 text-green-700' :
                  event.approvalStatus === 'REJECTED' ? 'bg-red-100 text-red-700' :
                  'bg-orange-100 text-orange-700'
                }`}>
                  Approval: {event.approvalStatus}
                </span>
              </div>

              {/* Event Details Grid */}
              <div className="p-8 grid grid-cols-1 md:grid-cols-2 gap-8">
                <div>
                  <div className="flex items-center gap-3 mb-3">
                    <Calendar className="w-5 h-5 text-accent" />
                    <span className="text-sm font-semibold text-primary">Start Time</span>
                  </div>
                  <p className="text-text-secondary text-sm ml-8 bg-surface-secondary px-4 py-2 rounded-lg">{event.startTime}</p>
                </div>
                <div>
                  <div className="flex items-center gap-3 mb-3">
                    <Calendar className="w-5 h-5 text-accent" />
                    <span className="text-sm font-semibold text-primary">End Time</span>
                  </div>
                  <p className="text-text-secondary text-sm ml-8 bg-surface-secondary px-4 py-2 rounded-lg">{event.endTime}</p>
                </div>
                <div className="md:col-span-2">
                  <div className="flex items-center gap-3 mb-3">
                    <MapPin className="w-5 h-5 text-accent" />
                    <span className="text-sm font-semibold text-primary">Location</span>
                  </div>
                  <p className="text-text-secondary text-sm ml-8 bg-surface-secondary px-4 py-2 rounded-lg">{event.location}</p>
                </div>
                <div className="md:col-span-2">
                  <div className="flex items-center gap-3 mb-3">
                    <Info className="w-5 h-5 text-accent" />
                    <span className="text-sm font-semibold text-primary">Description</span>
                  </div>
                  <p className="text-text-secondary text-sm leading-relaxed ml-8 bg-surface-secondary px-4 py-3 rounded-lg">{event.description || 'No description provided'}</p>
                </div>
              </div>
            </div>

            {/* Attendees Section */}
            <div className="bg-surface rounded-xl shadow-md p-0 overflow-hidden border border-surface-tertiary">
              {/* Gradient Header */}
              <div className="bg-gradient-to-r from-slate-800 to-slate-900 px-8 py-5">
                <div className="flex items-center gap-3">
                  <Users className="w-5 h-5 text-white" />
                  <h3 className="text-lg font-semibold text-white">Attendees List</h3>
                  <span className="ml-auto bg-slate-700 text-white px-3 py-1 rounded-full text-xs font-semibold border border-slate-600">
                    {Array.isArray(attendees) ? attendees.length : 0} people
                  </span>
                </div>
              </div>
              <div className="p-8">
              <div className="overflow-x-auto">
                {Array.isArray(attendees) && attendees.length === 0 ? (
                  <div className="text-center py-12">
                    <Users className="w-12 h-12 text-surface-tertiary mx-auto mb-4" />
                    <p className="text-text-secondary text-sm">No attendees registered yet</p>
                    <p className="text-text-tertiary text-xs mt-2">Attendees will appear here once invited</p>
                  </div>
                ) : (
                  <div className="space-y-2">
                    {attendees.map((a: any) => {
                      const name = a.userFullName || a.fullName || a.name || a.email || '-';
                      const initials = name && typeof name === 'string' ? name.split(' ').map((n: string) => n[0]).join('').toUpperCase().slice(0,2) : '?';
                      const email = a.email || a.userEmail || '-';
                      const status = a.invitationStatus || a.status || 'PENDING';
                      return (
                        <div key={a.id} className="flex items-center justify-between p-4 bg-surface-secondary hover:bg-surface-tertiary rounded-lg transition-all border border-surface-tertiary">
                          <div className="flex items-center gap-4 flex-1">
                            <span className="inline-flex items-center justify-center w-10 h-10 rounded-full bg-accent text-white font-semibold text-xs shadow-md">
                              {initials}
                            </span>
                            <div className="flex-1 min-w-0">
                              <p className="text-sm font-semibold text-primary">{name}</p>
                              <p className="text-xs text-text-secondary flex items-center gap-1"><AtSign className="w-3 h-3" />{email}</p>
                            </div>
                          </div>
                          <span className={`flex-shrink-0 inline-flex items-center gap-2 px-3 py-1 rounded-lg text-xs font-semibold ${
                            status === 'ACCEPTED' ? 'bg-emerald-100 text-emerald-700' :
                            status === 'DECLINED' ? 'bg-rose-100 text-rose-700' :
                            'bg-amber-100 text-amber-700'
                          }`}>
                            <span className={`inline-block w-2 h-2 rounded-full ${
                              status === 'ACCEPTED' ? 'bg-emerald-500' :
                              status === 'DECLINED' ? 'bg-rose-500' :
                              'bg-amber-500'
                            }`}></span>
                            {status}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
              </div>
            </div>
          </div>

          {/* Right Column - Actions Sidebar */}
          <div className="lg:col-span-1">
            <div className="bg-surface rounded-xl shadow-md p-6 border border-surface-tertiary sticky top-8 space-y-3">
              <h3 className="text-xl font-bold text-primary mb-6 flex items-center gap-2">
                <span className="w-5 h-5 rounded-full bg-accent"></span>
                Actions
              </h3>
              
              {showEdit && (
                <Button onClick={handleEdit} variant="secondary" className="w-full justify-start gap-2">
                  <Edit className="w-4 h-4" /> Edit Event
                </Button>
              )}
              
              {showInvite && (
                <Button onClick={handleInvite} variant="secondary" className="w-full justify-start gap-2">
                  <Mail className="w-4 h-4" /> Send Invites
                </Button>
              )}
              
              {showApprove && (
                <Button onClick={handleApprove} variant="primary" className="w-full justify-start gap-2">
                  <CheckCircle className="w-4 h-4" /> Approve
                </Button>
              )}
              
              {showReject && (
                <Button onClick={handleReject} variant="primary" className="w-full justify-start gap-2">
                  <XCircle className="w-4 h-4" /> Reject
                </Button>
              )}
              
              {showHold && (
                <Button onClick={handleHold} variant="secondary" className="w-full justify-start gap-2">
                  <PauseCircle className="w-4 h-4" /> Hold
                </Button>
              )}
              
              {showReactivate && (
                <Button onClick={handleReactivate} variant="primary" className="w-full justify-start gap-2">
                  <RefreshCw className="w-4 h-4" /> Reactivate
                </Button>
              )}
              
              {showDelete && (
                <Button onClick={handleDelete} variant="danger" className="w-full justify-start gap-2">
                  <Trash2 className="w-4 h-4" /> Delete Event
                </Button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Invite Modal */}
      {showInviteModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-40 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md relative max-h-[90vh] overflow-y-auto">
            <div className="sticky top-0 bg-accent px-6 py-4 flex items-center justify-between">
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                <Mail className="w-5 h-5" /> Send Invitations
              </h3>
              <button className="text-white hover:text-accent-light text-2xl" onClick={() => setShowInviteModal(false)}>×</button>
            </div>
            <div className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-semibold text-primary mb-2">Upload CSV File (Optional)</label>
                <div className="border-2 border-dashed border-surface-tertiary rounded-lg p-4 text-center hover:border-accent transition-colors">
                  <input
                    type="file"
                    accept=".csv"
                    ref={fileInputRef}
                    onChange={e => setInviteFile(e.target.files && e.target.files[0] ? e.target.files[0] : null)}
                    className="hidden"
                    id="csv-upload"
                  />
                  <label htmlFor="csv-upload" className="cursor-pointer">
                    <p className="text-text-primary font-medium">Click to upload CSV or drag and drop</p>
                    <p className="text-xs text-text-secondary mt-1">CSV format: email, fullName</p>
                  </label>
                </div>
                {inviteFile && (
                  <div className="mt-3 flex items-center gap-2 bg-emerald-50 border border-emerald-200 rounded-lg p-3">
                    <CheckCircle className="w-4 h-4 text-emerald-600" />
                    <span className="text-sm text-emerald-700 font-medium">{inviteFile?.name}</span>
                  </div>
                )}
              </div>
            </div>
            <div className="border-t border-slate-200 px-6 py-4 flex gap-3 justify-end">
              <Button onClick={() => setShowInviteModal(false)} variant="secondary">Cancel</Button>
              <Button onClick={handleInviteSubmit} isLoading={inviteLoading} leftIcon={<Mail className="w-4 h-4" />}>
                Send Invitations
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Edit Modal */}
      {showEditModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-40 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md relative max-h-[90vh] overflow-y-auto">
            <div className="sticky top-0 bg-accent px-6 py-4 flex items-center justify-between">
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                <Edit className="w-5 h-5" /> Edit Event
              </h3>
              <button className="text-white hover:text-accent-light text-2xl" onClick={() => setShowEditModal(false)}>×</button>
            </div>
            <div className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-semibold text-primary mb-1">Event Title</label>
                <input
                  type="text"
                  value={editForm.title}
                  onChange={e => setEditForm({...editForm, title: e.target.value})}
                  className="w-full border border-surface-tertiary rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-accent focus:border-transparent transition-all"
                  placeholder="Enter event title"
                />
                    </div>
              <div>
                <label className="block text-sm font-semibold text-primary mb-1">Description</label>
                <textarea
                  value={editForm.description}
                  onChange={e => setEditForm({...editForm, description: e.target.value})}
                  className="w-full border border-surface-tertiary rounded-lg px-3 py-2 text-sm h-20 focus:ring-2 focus:ring-accent focus:border-transparent transition-all"
                  placeholder="Enter event description"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-semibold text-primary mb-1">Start Time</label>
                  <input
                    type="text"
                    value={editForm.startTime}
                    onChange={e => setEditForm({...editForm, startTime: e.target.value})}
                    placeholder="yyyy-MM-dd HH:mm:ss"
                    className="w-full border border-surface-tertiary rounded-lg px-3 py-2 text-xs focus:ring-2 focus:ring-accent focus:border-transparent transition-all"
                  />
                </div>
                <div>
                  <label className="block text-sm font-semibold text-primary mb-1">End Time</label>
                  <input
                    type="text"
                    value={editForm.endTime}
                    onChange={e => setEditForm({...editForm, endTime: e.target.value})}
                    placeholder="yyyy-MM-dd HH:mm:ss"
                    className="w-full border border-surface-tertiary rounded-lg px-3 py-2 text-xs focus:ring-2 focus:ring-accent focus:border-transparent transition-all"
                  />
                </div>
              </div>
              <div>
                <label className="block text-sm font-semibold text-primary mb-1">Location</label>
                <input
                  type="text"
                  value={editForm.location}
                  onChange={e => setEditForm({...editForm, location: e.target.value})}
                  className="w-full border border-surface-tertiary rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-accent focus:border-transparent transition-all"
                  placeholder="Enter event location"
                />
              </div>
              <div>
                <label className="block text-sm font-semibold text-primary mb-1">Visibility</label>
                <select
                  value={editForm.visibility}
                  onChange={e => setEditForm({...editForm, visibility: e.target.value})}
                  className="w-full border border-surface-tertiary rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-accent focus:border-transparent transition-all"
                >
                  <option value="PUBLIC">Public</option>
                  <option value="PRIVATE">Private</option>
                </select>
              </div>
            </div>
            <div className="border-t border-surface-tertiary px-6 py-4 flex gap-3 justify-end">
              <Button onClick={() => setShowEditModal(false)} variant="secondary">Cancel</Button>
              <Button onClick={handleEditSubmit} isLoading={editLoading} leftIcon={<Edit className="w-4 h-4" />}>
                Save Changes
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
