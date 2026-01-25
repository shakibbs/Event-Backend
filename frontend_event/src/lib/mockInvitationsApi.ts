// Mock API for fetching invitations for an event
// This simulates what a real /api/events/{eventId}/invitations endpoint would return
export interface InvitationLog {
  id: number;
  email: string;
  status: 'PENDING' | 'ACCEPTED' | 'DECLINED';
  sentAt: string;
  respondedAt?: string;
}

// Simulate a DB of invitations per event
const mockInvitations: Record<string | number, InvitationLog[]> = {
  1: [
    { id: 1, email: 'alice@example.com', status: 'PENDING', sentAt: '2026-01-20T10:00:00Z' },
    { id: 2, email: 'bob@example.com', status: 'ACCEPTED', sentAt: '2026-01-20T10:00:00Z', respondedAt: '2026-01-21T09:00:00Z' },
    { id: 3, email: 'carol@example.com', status: 'DECLINED', sentAt: '2026-01-20T10:00:00Z', respondedAt: '2026-01-22T12:00:00Z' },
  ],
  2: [
    { id: 4, email: 'dave@example.com', status: 'PENDING', sentAt: '2026-01-22T11:00:00Z' },
  ],
};

export async function fetchInvitationsForEvent(eventId: string | number): Promise<InvitationLog[]> {
  // Simulate network delay
  await new Promise((resolve) => setTimeout(resolve, 300));
  return mockInvitations[eventId] || [];
}