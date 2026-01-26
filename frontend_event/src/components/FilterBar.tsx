import React from 'react';
import { SearchInput } from './ui/Input';
import { Select } from './ui/Select';
import { Button } from './ui/Button';
import { Filter, Download } from 'lucide-react';
import { FilterState, EventStatus } from '../types';
interface FilterBarProps {
  filters: FilterState;
  onFilterChange: (key: keyof FilterState, value: any) => void;
  onExport?: () => void;
  onInviteClick?: () => void;
}
export function FilterBar({
  filters,
  onFilterChange,
  onExport,
  onInviteClick
}: FilterBarProps) {
  return (
    <div className="flex flex-col sm:flex-row gap-4 items-center justify-between bg-white p-4 rounded-xl border border-surface-border shadow-sm mb-6">
      <div className="flex flex-1 w-full sm:w-auto gap-4 items-center">
        <div className="w-full sm:w-64">
          <SearchInput
            placeholder="Search events, locations..."
            value={filters.search}
            onChange={(e) => onFilterChange('search', e.target.value)} />

        </div>

        <div className="hidden sm:flex items-center gap-2 border-l border-surface-border pl-4">
          <Select
            options={[
              { value: 'all', label: 'All Statuses' },
              { value: 'upcoming', label: 'Upcoming' },
              { value: 'ongoing', label: 'Ongoing' },
              { value: 'active', label: 'Active' },
              { value: 'completed', label: 'Completed' },
              { value: 'cancelled', label: 'Cancelled' }
            ]}
            value={filters.status}
            onChange={(e) =>
              onFilterChange('status', e.target.value as EventStatus | 'all')
            }
            className="w-40" />


          <Select
            options={[
            {
              value: 'all',
              label: 'All Dates'
            },
            {
              value: 'upcoming',
              label: 'Upcoming Dates'
            },
            {
              value: 'past',
              label: 'Past Dates'
            }]
            }
            value={filters.dateRange}
            onChange={(e) => onFilterChange('dateRange', e.target.value)}
            className="w-40" />

        </div>
      </div>

      <div className="flex gap-2 w-full sm:w-auto">
        <Button
          variant="outline"
          leftIcon={<Filter className="h-4 w-4" />}
          className="sm:hidden w-full">
          Filters
        </Button>
        {onExport &&
          <Button
            variant="secondary"
            leftIcon={<Download className="h-4 w-4" />}
            onClick={onExport}
            className="whitespace-nowrap">
            Export
          </Button>
        }
        {onInviteClick &&
          <Button
            variant="primary"
            onClick={onInviteClick}
            className="whitespace-nowrap">
            Invite
          </Button>
        }
      </div>
    </div>);

}