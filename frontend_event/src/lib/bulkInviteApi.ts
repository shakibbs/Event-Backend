import { apiRequest } from '../lib/api';

export async function sendBulkInvitationsApi(eventId: string | number, file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return apiRequest(`/events/${eventId}/invite`, {
    method: 'POST',
    body: formData,
  });
}
