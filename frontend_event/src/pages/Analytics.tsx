import React from 'react';
import { BarChart3 } from 'lucide-react';
export function Analytics() {
  return (
    <div className="flex flex-col items-center justify-center h-[60vh] text-center space-y-4">
      <div className="p-4 bg-blue-50 rounded-full text-accent">
        <BarChart3 className="h-12 w-12" />
      </div>
      <h2 className="text-2xl font-bold text-primary">Analytics Dashboard</h2>
      <p className="text-text-secondary max-w-md">
        Advanced analytics and reporting features are currently under
        development. Check back soon for detailed insights.
      </p>
    </div>);

}