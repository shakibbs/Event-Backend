import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiRequest } from '../lib/api';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { useAuth } from '../hooks/useAuth';
import { hasPermission } from '../utils/rolePermissions';

export function PermissionManagement() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const canManage = hasPermission(user, 'role.manage.all');

  // Check if user has permission to access this page
  useEffect(() => {
    if (!canManage) {
      navigate('/dashboard');
    }
  }, [user, navigate, canManage]);

  const [permissions, setPermissions] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [selectedPermission, setSelectedPermission] = useState<any | null>(null);
  const [permissionForm, setPermissionForm] = useState({ name: '', description: '' });
  const [refresh, setRefresh] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    apiRequest('/permissions')
      .then(setPermissions)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, [refresh]);


  const handleCreateOrEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editMode && selectedPermission) {
        await apiRequest(`/permissions/${selectedPermission.id}`, {
          method: 'PUT',
          body: JSON.stringify(permissionForm)
        });
      } else {
        await apiRequest('/permissions', {
          method: 'POST',
          body: JSON.stringify(permissionForm)
        });
      }
      setPermissionForm({ name: '', description: '' });
      setShowModal(false);
      setEditMode(false);
      setSelectedPermission(null);
      setRefresh(r => r + 1);
    } catch (e: any) {
      setError(e.message);
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Delete this permission?')) return;
    try {
      await apiRequest(`/permissions/${id}`, { method: 'DELETE' });
      setRefresh(r => r + 1);
    } catch (e: any) {
      setError(e.message);
    }
  };

  const filteredPermissions = permissions.filter(permission =>
    permission.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    permission.description?.toLowerCase().includes(searchQuery.toLowerCase())
  );
  return (
    <div className="w-full px-0">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-bold">Permission Management</h2>
        <Button onClick={() => {
          setShowModal(true);
          setEditMode(false);
          setPermissionForm({ name: '', description: '' });
          setSelectedPermission(null);
        }} className="bg-primary text-white">Add Permission</Button>
      </div>
      {error && <div className="mb-4 text-red-600">{error}</div>}
      <div className="mb-4">
        <Input
          type="text"
          placeholder="Search by name or description..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full max-w-md"
        />
      </div>
      <div className="w-full bg-white rounded-xl shadow border border-slate-200 overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-slate-50">
              <th className="px-6 py-3 text-left font-semibold text-slate-500">Name</th>
              <th className="px-6 py-3 text-left font-semibold text-slate-500">Description</th>
              <th className="px-6 py-3 text-right font-semibold text-slate-500">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={3} className="text-center py-8">Loading permissions...</td></tr>
            ) : filteredPermissions.length === 0 ? (
              <tr><td colSpan={3} className="text-center py-8 text-slate-400">No permissions found.</td></tr>
            ) : filteredPermissions.map((perm: any) => (
              <tr key={perm.id} className="border-t border-slate-100 hover:bg-slate-50 transition">
                <td className="px-6 py-3 font-medium">{perm.name}</td>
                <td className="px-6 py-3">{perm.description}</td>
                <td className="px-6 py-3 text-right flex gap-2 justify-end">
                  <Button
                    onClick={() => {
                      setShowModal(true);
                      setEditMode(true);
                      setSelectedPermission(perm);
                      setPermissionForm({ name: perm.name || '', description: perm.description || '' });
                    }}
                    variant="outline"
                    size="sm"
                    className="h-8 px-3 mr-2"
                  >
                    Edit
                  </Button>
                  <Button
                    onClick={() => handleDelete(perm.id)}
                    variant="outline"
                    size="sm"
                    className="h-8 px-3 border-red-500 text-red-600 hover:bg-red-50 hover:border-red-600"
                  >
                    Delete
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Modal for add/edit permission */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30">
          <div className="bg-white rounded-xl shadow-lg p-8 w-full max-w-md relative">
            <button onClick={() => {
              setShowModal(false);
              setEditMode(false);
              setSelectedPermission(null);
            }} className="absolute top-3 right-3 text-slate-400 hover:text-slate-700">&times;</button>
            <h3 className="text-lg font-bold mb-4">{editMode ? 'Edit Permission' : 'Add New Permission'}</h3>
            <form onSubmit={handleCreateOrEdit} className="space-y-4">
              <Input value={permissionForm.name} onChange={e => setPermissionForm({ ...permissionForm, name: e.target.value })} placeholder="Permission Name" required />
              <Input value={permissionForm.description} onChange={e => setPermissionForm({ ...permissionForm, description: e.target.value })} placeholder="Description" required />
              <div className="flex justify-end gap-2">
                <Button type="button" onClick={() => {
                  setShowModal(false);
                  setEditMode(false);
                  setSelectedPermission(null);
                }} className="bg-slate-200 text-slate-700">Cancel</Button>
                <Button type="submit" className="bg-primary text-white">{editMode ? 'Save Changes' : 'Add Permission'}</Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
