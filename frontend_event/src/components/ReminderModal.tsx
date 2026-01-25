import React, { useState } from 'react';
import { Button } from './ui/Button';

interface ReminderModalProps {
  open: boolean;
  onClose: () => void;
  onSave: (datetime: string, type: string) => Promise<void>;
}

export function ReminderModal({ open, onClose, onSave }: ReminderModalProps) {
  const [datetime, setDatetime] = useState('');
  const [type, setType] = useState('email');
  const [loading, setLoading] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);

  if (!open) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setFeedback(null);
    try {
      await onSave(datetime, type);
      setFeedback('Reminder set!');
      setDatetime('');
      setType('email');
    } catch (err: any) {
      setFeedback(err.message || 'Failed to set reminder');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30">
      <div className="bg-white rounded-xl shadow-lg p-8 w-full max-w-md relative">
        <button onClick={onClose} className="absolute top-3 right-3 text-slate-400 hover:text-slate-700">&times;</button>
        <h3 className="text-lg font-bold mb-4">Set Reminder</h3>
        <form onSubmit={handleSubmit} className="space-y-4">
          <input
            className="w-full border rounded px-3 py-2"
            type="datetime-local"
            value={datetime}
            onChange={e => setDatetime(e.target.value)}
            required
          />
          <select
            className="w-full border rounded px-3 py-2"
            value={type}
            onChange={e => setType(e.target.value)}
          >
            <option value="email">Email</option>
            <option value="sms">SMS</option>
          </select>
          {feedback && <div className="text-sm text-green-600">{feedback}</div>}
          <div className="flex justify-end gap-2">
            <Button type="button" onClick={onClose} className="bg-slate-200 text-slate-700">Cancel</Button>
            <Button type="submit" className="bg-primary text-white" disabled={loading}>{loading ? 'Saving...' : 'Save'}</Button>
          </div>
        </form>
      </div>
    </div>
  );
}
