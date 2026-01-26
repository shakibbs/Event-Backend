import React, { useEffect, useState } from 'react';
import { apiRequest } from '../lib/api';

export function RolesList() {
  const [roles, setRoles] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiRequest('/roles')
      .then(setRoles)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div>Loading roles...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div>
      <h2 className="text-xl font-bold mb-4">Roles</h2>
      <ul>
        {roles.map((role: any) => (
          <li key={role.id}>{role.name}</li>
        ))}
      </ul>
    </div>
  );
}
