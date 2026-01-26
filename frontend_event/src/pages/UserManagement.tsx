import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiRequest } from '../lib/api';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Select } from '../components/ui/Select';
import { useAuth } from '../hooks/useAuth';
import { hasPermission, hasAnyPermission, getUserPermissions } from '../utils/rolePermissions';


export function UserManagement() {
  const { user } = useAuth();
  // DEBUG: Log current user and permissions (detailed)
  console.log('Current user (detailed):', JSON.stringify(user, null, 2));
  const perms = getUserPermissions(user);
  console.log('User permissions:', perms);
  if (Array.isArray(perms)) {
    perms.forEach((p, i) => console.log(`Permission[${i}]:`, p));
  }
  const navigate = useNavigate();
  const canView = hasAnyPermission(user, ['user.view.all', 'user.manage.all', 'user.manage.own']);
  const canManageAll = hasPermission(user, 'user.manage.all');
  const canManageOwn = hasPermission(user, 'user.manage.own');

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
  const [userForm, setUserForm] = useState({ fullName: '', email: '', password: '', role: '' });
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

  // Always fetch all roles on mount if user can manage users
  useEffect(() => {
    if ((canManageAll || canManageOwn) && allRoles.length === 0) {
      setRoleLoading(true);
      apiRequest('/roles')
        .then((roles) => setAllRoles(roles))
        .catch(() => setAllRoles([]))
        .finally(() => setRoleLoading(false));
    }
  }, [canManageAll, canManageOwn, allRoles.length]);


  const handleCreateOrEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!userForm.role) {
      setError('Role is required.');
      return;
    }
    try {
      if (editMode && selectedUser) {
        await apiRequest(`/users/${selectedUser.id}`, {
          method: 'PUT',
          body: JSON.stringify({
            fullName: userForm.fullName,
            email: userForm.email,
            password: userForm.password || undefined, // Only send if changed
            roleId: userForm.role
          })
        });
      } else {
        await apiRequest('/users', {
          method: 'POST',
          body: JSON.stringify({
            fullName: userForm.fullName,
            email: userForm.email,
            password: userForm.password,
            roleId: userForm.role
          })
        });
      }
      setUserForm({ fullName: '', email: '', password: '', role: '' });
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

  // Only show Attendee users if admin lacks user.manage.all or user.view.all
  let filteredUsers = users;
  // If user has user.manage.own (and NOT user.manage.all or user.view.all), only show Attendee users
  if (!canView) {
    filteredUsers = [];
  } else if (
    hasPermission(user, 'user.manage.own') &&
    !hasAnyPermission(user, ['user.manage.all', 'user.view.all'])
  ) {
    filteredUsers = users.filter((u) => {
      let roleName = '';
      if (typeof u.role === 'string') {
        roleName = u.role;
      } else if (u.role && typeof u.role === 'object' && u.role.name) {
        roleName = u.role.name;
      }
      return roleName.toLowerCase() === 'attendee';
    });
  }
  // Apply search filter
  filteredUsers = filteredUsers.filter(user =>
    user.fullName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    user.email?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="w-full px-0">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-bold">User Management</h2>
        {(canManageAll || canManageOwn) && (
          <Button
            onClick={async () => {
              // If roles are not loaded, fetch them first
              let roles = allRoles;
              if (roles.length === 0) {
                roles = await apiRequest('/roles');
                setAllRoles(roles);
              }
              // Pre-select Attendee role for user.manage.own (not user.manage.all)
              let defaultRole = '';
              if (canManageOwn && !canManageAll) {
                const attendeeRole = roles.find((role: any) => role.name === 'Attendee');
                if (attendeeRole) defaultRole = attendeeRole.id;
              }
              setUserForm({ fullName: '', email: '', password: '', role: defaultRole });
              setSelectedUser(null);
              setShowModal(true);
              setEditMode(false);
            }}
            className="bg-primary text-white"
            disabled={canManageOwn && !canManageAll && (roleLoading || allRoles.filter((role: any) => role.name === 'Attendee').length === 0)}
          >
            Add User
          </Button>
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
              {(canManageAll || canManageOwn) && <th className="px-6 py-3 text-right font-semibold text-slate-500">Actions</th>}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={(canManageAll || canManageOwn) ? 4 : 3} className="text-center py-8">Loading users...</td></tr>
            ) : filteredUsers.length === 0 ? (
              <tr><td colSpan={(canManageAll || canManageOwn) ? 4 : 3} className="text-center py-8 text-slate-400">No users found.</td></tr>
            ) : filteredUsers.map((user: any) => {
              const isAttendee = (typeof user.role === 'string' ? user.role : user.role?.name) === 'Attendee';
              const showManage = canManageAll || (canManageOwn && isAttendee);
              return (
                <tr key={user.id} className="border-t border-slate-100 hover:bg-slate-50 transition">
                  <td className="px-6 py-3 font-medium">{user.name || user.fullName || user.email}</td>
                  <td className="px-6 py-3">{user.email}</td>
                  <td className="px-6 py-3 capitalize">{user.role?.name || user.role}</td>
                  <td className="px-6 py-3 text-right flex gap-2 justify-end">
                    {showManage && (
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
                            // Always use role ID for Select value
                            let roleId = '';
                            if (user.role && typeof user.role === 'object' && user.role.id) {
                              roleId = user.role.id;
                            } else if (user.role && typeof user.role === 'string') {
                              // Try to find role by name
                              const found = allRoles.find((r: any) => r.name === user.role);
                              if (found) roleId = found.id;
                            }
                            setUserForm({
                              fullName: user.fullName || user.name || '',
                              email: user.email || '',
                              password: '',
                              role: roleId
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
              );
            })}
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
              <Input value={userForm.fullName} onChange={e => setUserForm({ ...userForm, fullName: e.target.value })} placeholder="Full Name" required />
              <Input value={userForm.email} onChange={e => setUserForm({ ...userForm, email: e.target.value })} placeholder="Email" required type="email" />
              <Input value={userForm.password} onChange={e => setUserForm({ ...userForm, password: e.target.value })} placeholder="Password" type="password" required={!editMode} />
              {/* Only allow Attendee role for Admins with only user.manage.own */}
              {canManageOwn && !canManageAll ? (
                <Select
                  label="Role"
                  options={allRoles.filter((role: any) => role.name === 'Attendee').map((role: any) => ({ value: role.id, label: role.name }))}
                  value={userForm.role}
                  onChange={e => setUserForm({ ...userForm, role: e.target.value })}
                  required
                  disabled={editMode && (selectedUser && (selectedUser.role?.name !== 'Attendee' && selectedUser.role !== 'Attendee'))}
                />
              ) : (
                <Select
                  label="Role"
                  options={allRoles.map((role: any) => ({ value: role.id, label: role.name }))}
                  value={userForm.role}
                  onChange={e => setUserForm({ ...userForm, role: e.target.value })}
                  required
                />
              )}
              <div className="flex justify-end gap-2">
                <Button type="button" onClick={() => {
                  setShowModal(false);
                  setEditMode(false);
                  setSelectedUser(null);
                }} className="bg-slate-200 text-slate-700">Cancel</Button>
                <Button type="submit" className="bg-primary text-white" disabled={!userForm.role}>{editMode ? 'Save Changes' : 'Add User'}</Button>
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
