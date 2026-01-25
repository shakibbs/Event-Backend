import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiRequest } from '../lib/api';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Select } from '../components/ui/Select';
import { useAuth } from '../hooks/useAuth';
import { hasPermission, hasAnyPermission } from '../utils/rolePermissions';


export function UserManagement() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const canView = hasAnyPermission(user, ['user.view.all', 'user.manage.all']);
  const canManage = hasPermission(user, 'user.manage.all');

  // Check if user has permission to access this page
  useEffect(() => {
    if (!canView) {
      navigate('/dashboard');
    }
  }, [user, navigate, canView]);

  const [users, setUsers] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [selectedUser, setSelectedUser] = useState<any | null>(null);
  const [userForm, setUserForm] = useState({ name: '', email: '', password: '', role: '' });
  const [refresh, setRefresh] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');

  // User-role management modal state
  const [showRoleModal, setShowRoleModal] = useState(false);
  const [roleModalUser, setRoleModalUser] = useState<any | null>(null);
  const [allRoles, setAllRoles] = useState<any[]>([]);
  const [roleLoading, setRoleLoading] = useState(false);
  const [selectedRoleId, setSelectedRoleId] = useState<string>('');

  useEffect(() => {
    apiRequest('/users')
      .then(setUsers)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, [refresh]);

  // Fetch all roles for dropdown
  useEffect(() => {
    if (showRoleModal) {
      setRoleLoading(true);
      apiRequest('/roles')
        .then((roles) => setAllRoles(roles))
        .catch(() => setAllRoles([]))
        .finally(() => setRoleLoading(false));
    }
  }, [showRoleModal]);


  const handleCreateOrEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editMode && selectedUser) {
        await apiRequest(`/users/${selectedUser.id}`, {
          method: 'PUT',
          body: JSON.stringify({
            name: userForm.name,
            email: userForm.email,
            password: userForm.password || undefined, // Only send if changed
            role: userForm.role
          })
        });
      } else {
        await apiRequest('/users', {
          method: 'POST',
          body: JSON.stringify(userForm)
        });
      }
      setUserForm({ name: '', email: '', password: '', role: '' });
      setShowModal(false);
      setEditMode(false);
      setSelectedUser(null);
      setRefresh(r => r + 1);
    } catch (e: any) {
      setError(e.message);
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Delete this user?')) return;
    try {
      await apiRequest(`/users/${id}`, { method: 'DELETE' });
      setRefresh(r => r + 1);
    } catch (e: any) {
      setError(e.message);
    }
  };

  // Open user-role management modal
  const handleOpenRoleModal = (user: any) => {
    setRoleModalUser(user);
    setSelectedRoleId('');
    setShowRoleModal(true);
  };

  // Close user-role management modal
  const handleCloseRoleModal = () => {
    setShowRoleModal(false);
    setRoleModalUser(null);
    setSelectedRoleId('');
  };

  // Assign role to user
  const handleAssignRole = async () => {
    if (!roleModalUser || !selectedRoleId) return;
    setRoleLoading(true);
    try {
      await apiRequest(`/users/${roleModalUser.id}/roles/${selectedRoleId}`, { method: 'POST' });
      // Refresh users and modal user state
      const updatedUsers = await apiRequest('/users');
      setUsers(updatedUsers);
      // Update modal user to reflect new role
      const updated = updatedUsers.find((u: any) => u.id === roleModalUser.id);
      setRoleModalUser(updated);
      setSelectedRoleId('');
    } catch (e: any) {
      setError(e.message);
    } finally {
      setRoleLoading(false);
    }
  };

  // Remove role from user
  const handleRemoveRole = async () => {
    if (!roleModalUser || !roleModalUser.role) return;
    setRoleLoading(true);
    try {
      await apiRequest(`/users/${roleModalUser.id}/roles/${roleModalUser.role.id}`, { method: 'DELETE' });
      // Refresh users and modal user state
      const updatedUsers = await apiRequest('/users');
      setUsers(updatedUsers);
      const updated = updatedUsers.find((u: any) => u.id === roleModalUser.id);
      setRoleModalUser(updated);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setRoleLoading(false);
    }
  };

  const filteredUsers = users.filter(user =>
    user.fullName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    user.email?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="w-full px-0">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-bold">User Management</h2>
        {canManage && (
          <Button onClick={() => {
            setShowModal(true);
            setEditMode(false);
            setUserForm({ name: '', email: '', password: '', role: '' });
            setSelectedUser(null);
          }} className="bg-primary text-white">Add User</Button>
        )}
      </div>
      {error && <div className="mb-4 text-red-600">{error}</div>}
      <div className="mb-4">
        <Input
          type="text"
          placeholder="Search by name or email..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full max-w-md"
        />
      </div>
      <div className="bg-white rounded-xl shadow border border-slate-200 overflow-x-auto">
        <table className="min-w-full text-sm">
          <thead>
            <tr className="bg-slate-50">
              <th className="px-6 py-3 text-left font-semibold text-slate-500">Name</th>
              <th className="px-6 py-3 text-left font-semibold text-slate-500">Email</th>
              <th className="px-6 py-3 text-left font-semibold text-slate-500">Role</th>
              {canManage && <th className="px-6 py-3 text-right font-semibold text-slate-500">Actions</th>}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={canManage ? 4 : 3} className="text-center py-8">Loading users...</td></tr>
            ) : filteredUsers.length === 0 ? (
              <tr><td colSpan={canManage ? 4 : 3} className="text-center py-8 text-slate-400">No users found.</td></tr>
            ) : filteredUsers.map((user: any) => (
              <tr key={user.id} className="border-t border-slate-100 hover:bg-slate-50 transition">
                <td className="px-6 py-3 font-medium">{user.name || user.fullName || user.email}</td>
                <td className="px-6 py-3">{user.email}</td>
                <td className="px-6 py-3 capitalize">{user.role?.name || user.role}</td>
                <td className="px-6 py-3 text-right flex gap-2 justify-end">
                  {canManage && (
                    <>
                      <Button
                        onClick={() => handleOpenRoleModal(user)}
                        variant="outline"
                        size="sm"
                        className="h-8 px-3 mr-2"
                      >
                        Manage Roles
                      </Button>
                      <Button
                        onClick={() => {
                          setShowModal(true);
                          setEditMode(true);
                          setSelectedUser(user);
                          setUserForm({
                            name: user.name || '',
                            email: user.email || '',
                            password: '',
                            role: user.role?.name || user.role || ''
                          });
                        }}
                        variant="outline"
                        size="sm"
                        className="h-8 px-3 mr-2"
                      >
                        Edit
                      </Button>
                      <Button
                        onClick={() => handleDelete(user.id)}
                        variant="outline"
                        size="sm"
                        className="h-8 px-3 border-red-500 text-red-600 hover:bg-red-50 hover:border-red-600"
                      >
                        Delete
                      </Button>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Modal for add/edit user */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30">
          <div className="bg-white rounded-xl shadow-lg p-8 w-full max-w-md relative">
            <button onClick={() => {
              setShowModal(false);
              setEditMode(false);
              setSelectedUser(null);
            }} className="absolute top-3 right-3 text-slate-400 hover:text-slate-700">&times;</button>
            <h3 className="text-lg font-bold mb-4">{editMode ? 'Edit User' : 'Add New User'}</h3>
            <form onSubmit={handleCreateOrEdit} className="space-y-4">
              <Input value={userForm.name} onChange={e => setUserForm({ ...userForm, name: e.target.value })} placeholder="Name" required />
              <Input value={userForm.email} onChange={e => setUserForm({ ...userForm, email: e.target.value })} placeholder="Email" required type="email" />
              <Input value={userForm.password} onChange={e => setUserForm({ ...userForm, password: e.target.value })} placeholder="Password" type="password" required={!editMode} />
              <Select
                label="Role"
                options={allRoles.map((role: any) => ({ value: role.id, label: role.name }))}
                value={userForm.role}
                onChange={e => setUserForm({ ...userForm, role: e.target.value })}
                required
              />
              <div className="flex justify-end gap-2">
                <Button type="button" onClick={() => {
                  setShowModal(false);
                  setEditMode(false);
                  setSelectedUser(null);
                }} className="bg-slate-200 text-slate-700">Cancel</Button>
                <Button type="submit" className="bg-primary text-white">{editMode ? 'Save Changes' : 'Add User'}</Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* User-role management modal */}
      {showRoleModal && roleModalUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-white rounded-2xl shadow-2xl p-0 w-full max-w-xl relative border border-slate-200">
            <div className="flex items-center justify-between px-8 pt-7 pb-2 border-b border-slate-100">
              <h3 className="text-xl font-bold text-slate-800">Manage Role for <span className="text-primary">{roleModalUser.name || roleModalUser.fullName}</span></h3>
              <button onClick={handleCloseRoleModal} className="text-2xl text-slate-400 hover:text-slate-700 font-bold ml-4">&times;</button>
            </div>
            <div className="px-8 pt-5 pb-2">
              <div className="mb-4">
                <div className="font-semibold text-slate-700 text-base mb-2">Current Role</div>
                {roleModalUser.role ? (
                  <div className="flex items-center gap-3">
                    <span className="bg-slate-100 px-3 py-1 rounded-lg text-slate-700 font-medium text-sm">{roleModalUser.role.name || roleModalUser.role}</span>
                    <Button
                      type="button"
                      onClick={handleRemoveRole}
                      variant="outline"
                      size="sm"
                      className="h-8 px-3 border-red-500 text-red-600 hover:bg-red-50 hover:border-red-600"
                    >
                      Remove
                    </Button>
                  </div>
                ) : (
                  <span className="text-slate-400">No role assigned.</span>
                )}
              </div>
              <div className="mb-4">
                <div className="font-semibold text-slate-700 text-base mb-2">Assign New Role</div>
                <select
                  value={selectedRoleId}
                  onChange={e => setSelectedRoleId(e.target.value)}
                  className="border rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                  disabled={roleLoading}
                >
                  <option value="">Select role...</option>
                  {allRoles.map((role: any) => (
                    <option key={role.id} value={role.id}>{role.name}</option>
                  ))}
                </select>
                <Button
                  type="button"
                  onClick={handleAssignRole}
                  variant="outline"
                  size="sm"
                  className="h-8 px-3 border-green-500 text-green-600 hover:bg-green-50 hover:border-green-600 ml-3"
                  disabled={!selectedRoleId || roleLoading}
                >
                  Assign
                </Button>
              </div>
            </div>
            <div className="flex justify-end px-8 pb-7 pt-4">
              <Button
                type="button"
                onClick={handleCloseRoleModal}
                variant="outline"
                size="sm"
                className="h-8 px-3"
              >
                Close
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
