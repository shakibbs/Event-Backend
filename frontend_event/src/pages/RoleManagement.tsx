import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiRequest } from '../lib/api';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { useAuth } from '../hooks/useAuth';
import { hasPermission } from '../utils/rolePermissions';

// Permission assignment modal UI (add/remove)
function PermissionAssignmentModal({
  open, onClose, role, allPermissions, assignedPermissions, loading, onToggle
}: {
  open: boolean,
  onClose: () => void,
  role: any,
  allPermissions: any[],
  assignedPermissions: Set<string>,
  loading: boolean,
  onToggle: (permId: string, assigned: boolean) => void
}) {
  if (!open || !role) return null;

  const [showAssign, setShowAssign] = React.useState(false);
  const [selectedToAdd, setSelectedToAdd] = React.useState<string>('');

  // Permissions not assigned to this role
  const unassignedPermissions = allPermissions.filter(
    perm => !assignedPermissions.has(String(perm.id))
  );

  const handleAssign = async () => {
    if (!selectedToAdd) return;
    await onToggle(selectedToAdd, false); // false = add
    setSelectedToAdd('');
    setShowAssign(false);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="bg-white rounded-2xl shadow-2xl p-0 w-full max-w-xl relative border border-slate-200">
        <div className="flex items-center justify-between px-8 pt-7 pb-2 border-b border-slate-100">
          <h3 className="text-xl font-bold text-slate-800">Manage Permissions for <span className="text-primary">{role.name}</span></h3>
          <button onClick={onClose} className="text-2xl text-slate-400 hover:text-slate-700 font-bold ml-4">&times;</button>
        </div>
        <div className="px-8 pt-5 pb-2">
          <div className="flex items-center justify-between mb-4">
            <div className="font-semibold text-slate-700 text-base">Assigned Permissions</div>
            <Button
              type="button"
              onClick={() => setShowAssign(v => !v)}
              variant="outline"
              size="sm"
              className="h-8 px-3 border-green-500 text-green-600 hover:bg-green-50 hover:border-green-600"
            >
              <span className="inline-block align-middle mr-1">+</span>Assign Permission
            </Button>
          </div>
          {showAssign && (
            <div className="mb-4 flex gap-2 items-center bg-slate-50 p-3 rounded-lg border border-slate-200">
              <select
                value={selectedToAdd}
                onChange={e => setSelectedToAdd(e.target.value)}
                className="border rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
              >
                <option value="">Select permission...</option>
                {unassignedPermissions.map(perm => (
                  <option key={perm.id} value={perm.id}>{perm.name}</option>
                ))}
              </select>
              <Button type="button" onClick={handleAssign} className="bg-green-500 text-white px-4 py-2 rounded-lg text-sm font-semibold" disabled={!selectedToAdd}>Add</Button>
            </div>
          )}
          <div className="max-h-80 overflow-y-auto divide-y divide-slate-100 bg-slate-50 rounded-lg border border-slate-200">
            {[...assignedPermissions].length === 0 && (
              <div className="text-slate-400 text-center py-8">No permissions assigned to this role.</div>
            )}
            {allPermissions.filter(perm => assignedPermissions.has(String(perm.id))).map(perm => (
              <div key={perm.id} className="flex items-center py-3 px-4 gap-2 hover:bg-slate-100 transition">
                <div className="flex-1">
                  <div className="font-medium text-slate-800 text-sm">{perm.name}</div>
                  <div className="text-slate-400 text-xs mt-1">{perm.description}</div>
                </div>
                <Button
                  type="button"
                  onClick={() => onToggle(perm.id, true)}
                  variant="outline"
                  size="sm"
                  className="h-8 px-3 border-red-500 text-red-600 hover:bg-red-50 hover:border-red-600"
                  disabled={loading}
                >Remove</Button>
              </div>
            ))}
          </div>
        </div>
        <div className="flex justify-end px-8 pb-7 pt-4">
          <Button
            type="button"
            onClick={onClose}
            variant="outline"
            size="sm"
            className="h-8 px-3"
          >
            Close
          </Button>
        </div>
      </div>
    </div>
  );
}

export function RoleManagement() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const canManage = hasPermission(user, 'role.manage.all');

  // Check if user has permission to access this page
  useEffect(() => {
    if (!canManage) {
      navigate('/dashboard');
    }
  }, [user, navigate, canManage]);

  const [roles, setRoles] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [selectedRole, setSelectedRole] = useState<any | null>(null);
  const [roleForm, setRoleForm] = useState({ name: '', description: '' });
  const [refresh, setRefresh] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  // Permission assignment state
  const [showPermModal, setShowPermModal] = useState(false);
  const [allPermissions, setAllPermissions] = useState<any[]>([]);
  const [assignedPermissions, setAssignedPermissions] = useState<Set<string>>(new Set());
  const [permLoading, setPermLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    apiRequest('/roles')
      .then(setRoles)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, [refresh]);

  // Fetch all permissions once
  useEffect(() => {
    apiRequest('/permissions')
      .then(setAllPermissions)
      .catch(() => {});
  }, []);
  // Open permission modal for a role
  const handleOpenPermModal = async (role: any) => {
    setSelectedRole(role);
    setShowPermModal(true);
    setPermLoading(true);
    // Fetch assigned permissions for this role (from role.rolePermissions or via API if needed)
    let assigned: Set<string> = new Set();
    let roleData = role;
    if (!role.rolePermissions || !Array.isArray(role.rolePermissions)) {
      try {
        roleData = await apiRequest(`/roles/${role.id}`);
      } catch (e) {
        console.error('Failed to fetch role details:', e);
      }
    }
    // Support both .rolePermissions (old) and .permissions (new, from RoleResponseDTO)
    if (Array.isArray(roleData.permissions) || roleData.permissions instanceof Set) {
      assigned = new Set(
        Array.from(roleData.permissions)
          .filter((p: any) => p && p.id)
          .map((p: any) => String(p.id))
      );
    } else if (roleData.rolePermissions && Array.isArray(roleData.rolePermissions)) {
      assigned = new Set(
        roleData.rolePermissions
          .filter((rp: any) => rp.permission && rp.permission.id)
          .map((rp: any) => String(rp.permission.id))
      );
    }
    setAssignedPermissions(assigned);
    setPermLoading(false);
  };


  // Toggle permission assignment
  const handleTogglePermission = async (permId: string, assigned: boolean) => {
    if (!selectedRole) return;
    setPermLoading(true);
    try {
      if (assigned) {
        // Remove
        await apiRequest(`/roles/${selectedRole.id}/permissions/${permId}`, { method: 'DELETE' });
      } else {
        // Add
        await apiRequest(`/roles/${selectedRole.id}/permissions/${permId}`, { method: 'POST' });
      }
      // Always re-fetch assigned permissions after change
      try {
        const fresh = await apiRequest(`/roles/${selectedRole.id}`);
        let assignedSet: Set<string> = new Set();
        if (fresh.rolePermissions && Array.isArray(fresh.rolePermissions)) {
          assignedSet = new Set(
            fresh.rolePermissions
              .map((rp: any) => String(rp.permission?.id || rp.permissionId))
          );
        }
        setAssignedPermissions(assignedSet);
      } catch {}
      setRefresh(r => r + 1); // Refresh roles to reflect changes
    } catch (e: any) {
      setError(e.message);
    } finally {
      setPermLoading(false);
    }
  };


  const handleCreateOrEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editMode && selectedRole) {
        await apiRequest(`/roles/${selectedRole.id}`, {
          method: 'PUT',
          body: JSON.stringify(roleForm)
        });
      } else {
        await apiRequest('/roles', {
          method: 'POST',
          body: JSON.stringify(roleForm)
        });
      }
      setRoleForm({ name: '', description: '' });
      setShowModal(false);
      setEditMode(false);
      setSelectedRole(null);
      setRefresh(r => r + 1);
    } catch (e: any) {
      setError(e.message);
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Delete this role?')) return;
    try {
      await apiRequest(`/roles/${id}`, { method: 'DELETE' });
      setRefresh(r => r + 1);
    } catch (e: any) {
      setError(e.message);
    }
  };

  const filteredRoles = roles.filter(role =>
    role.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    role.description?.toLowerCase().includes(searchQuery.toLowerCase())
  );


  return (
    <div className="w-full px-0">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-bold">Role Management</h2>
        <Button onClick={() => {
          setShowModal(true);
          setEditMode(false);
          setRoleForm({ name: '', description: '' });
          setSelectedRole(null);
        }} className="bg-primary text-white">Add Role</Button>
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
      <div className="bg-white rounded-xl shadow border border-slate-200 overflow-x-auto">
        <table className="min-w-full text-sm">
          <thead>
            <tr className="bg-slate-50">
              <th className="px-6 py-3 text-left font-semibold text-slate-500">Name</th>
              <th className="px-6 py-3 text-right font-semibold text-slate-500">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={2} className="text-center py-8">Loading roles...</td></tr>
            ) : filteredRoles.length === 0 ? (
              <tr><td colSpan={2} className="text-center py-8 text-slate-400">No roles found.</td></tr>
            ) : filteredRoles.map((role: any) => (
              <tr key={role.id} className="border-t border-slate-100 hover:bg-slate-50 transition">
                <td className="px-6 py-3 font-medium">{role.name}</td>
                <td className="px-6 py-3 text-right flex gap-2 justify-end">
                  <Button
                    onClick={() => handleOpenPermModal(role)}
                    variant="outline"
                    size="sm"
                    className="h-8 px-3 mr-2"
                  >
                    Manage Permissions
                  </Button>
                  <Button
                    onClick={() => {
                      setShowModal(true);
                      setEditMode(true);
                      setSelectedRole(role);
                      setRoleForm({ name: role.name || '', description: role.description || '' });
                    }}
                    variant="outline"
                    size="sm"
                    className="h-8 px-3 mr-2"
                  >
                    Edit
                  </Button>
                  <Button
                    onClick={() => handleDelete(role.id)}
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

      {/* Modal for add/edit role */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30">
          <div className="bg-white rounded-xl shadow-lg p-8 w-full max-w-md relative">
            <button onClick={() => {
              setShowModal(false);
              setEditMode(false);
              setSelectedRole(null);
            }} className="absolute top-3 right-3 text-slate-400 hover:text-slate-700">&times;</button>
            <h3 className="text-lg font-bold mb-4">{editMode ? 'Edit Role' : 'Add New Role'}</h3>
            <form onSubmit={handleCreateOrEdit} className="space-y-4">
              <Input value={roleForm.name} onChange={e => setRoleForm({ ...roleForm, name: e.target.value })} placeholder="Role Name" required />
              <div className="flex justify-end gap-2">
                <Button type="button" onClick={() => {
                  setShowModal(false);
                  setEditMode(false);
                  setSelectedRole(null);
                }} className="bg-slate-200 text-slate-700">Cancel</Button>
                <Button type="submit" className="bg-primary text-white">{editMode ? 'Save Changes' : 'Add Role'}</Button>
              </div>
            </form>
          </div>
        </div>
      )}
      {/* Permission assignment modal */}
      <PermissionAssignmentModal
        open={showPermModal}
        onClose={() => setShowPermModal(false)}
        role={selectedRole}
        allPermissions={allPermissions}
        assignedPermissions={assignedPermissions}
        loading={permLoading}
        onToggle={handleTogglePermission}
      />
    </div>
  );
}
