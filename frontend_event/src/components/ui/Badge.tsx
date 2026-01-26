import React from 'react';
import { cn } from '../../lib/utils';
import { EventStatus } from '../../types';
interface BadgeProps {
  status?: EventStatus | string;
  variant?: 'default' | 'outline';
  className?: string;
  children?: React.ReactNode;
}
export function Badge({
  status,
  variant = 'default',
  className,
  children
}: BadgeProps) {
  const getStatusStyles = (s?: string) => {
    switch (s) {
      case 'upcoming':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'active':
        return 'bg-green-50 text-green-700 border-green-200';
      case 'completed':
        return 'bg-slate-100 text-slate-700 border-slate-200';
      case 'cancelled':
        return 'bg-red-50 text-red-700 border-red-200';
      default:
        return 'bg-slate-100 text-slate-700 border-slate-200';
    }
  };
  const styles = status ?
  getStatusStyles(status) :
  'bg-slate-100 text-slate-700 border-slate-200';
  return (
    <span
      className={cn(
        'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border',
        styles,
        className
      )}>

      {children || (
      status ? status.charAt(0).toUpperCase() + status.slice(1) : '')}
    </span>);

}