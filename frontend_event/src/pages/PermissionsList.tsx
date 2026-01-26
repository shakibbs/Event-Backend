import React, { useEffect, useState } from 'react';
import { apiRequest } from '../lib/api';

export function PermissionsList() {
  const [permissions, setPermissions] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiRequest('/permissions')
      .then(setPermissions)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div>Loading permissions...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div>
      <h2 className="text-xl font-bold mb-4">Permissions</h2>
      <ul>
        {permissions.map((perm: any) => (
          <li key={perm.id}>{perm.name} - {perm.description}</li>
        ))}
      </ul>
    </div>
  );
}
