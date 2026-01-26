import React, { useState } from 'react';
import { Button } from './ui/Button';

interface BulkInviteModalProps {
  open: boolean;
  onClose: () => void;
  onUpload: (file: File) => Promise<void>;
}

export function BulkInviteModal({ open, onClose, onUpload }: BulkInviteModalProps) {
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);

  if (!open) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) return;
    setLoading(true);
    setFeedback(null);
    try {
      await onUpload(file);
      setFeedback('Bulk invitations processing started!');
      setFile(null);
    } catch (err: any) {
      setFeedback(err.message || 'Failed to upload CSV');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30">
      <div className="bg-white rounded-xl shadow-lg p-8 w-full max-w-md relative">
        <button onClick={onClose} className="absolute top-3 right-3 text-slate-400 hover:text-slate-700">&times;</button>
        <h3 className="text-lg font-bold mb-4">Bulk Invite via CSV</h3>
        <form onSubmit={handleSubmit} className="space-y-4">
          <input
            type="file"
            accept=".csv"
            onChange={e => setFile(e.target.files?.[0] || null)}
            className="w-full border rounded px-3 py-2"
            required
          />
          {feedback && <div className="text-sm text-green-600">{feedback}</div>}
          <div className="flex justify-end gap-2">
            <Button type="button" onClick={onClose} className="bg-slate-200 text-slate-700">Cancel</Button>
            <Button type="submit" className="bg-primary text-white" disabled={loading || !file}>{loading ? 'Uploading...' : 'Upload & Invite'}</Button>
          </div>
        </form>
      </div>
    </div>
  );
}
