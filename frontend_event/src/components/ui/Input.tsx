// ...existing code...
export function SearchInput(props: InputProps) {
  return (
    <Input
      icon={<Search className="h-4 w-4" />} 
      placeholder="Search..."
      {...props}
    />
  );
}
import React, { forwardRef } from 'react';
import { cn } from '../../lib/utils';
import { Search } from 'lucide-react';
interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  icon?: React.ReactNode;
  fullWidth?: boolean;
  leftIcon?: React.ReactNode; // for compatibility with usage
}
export const Input = forwardRef<HTMLInputElement, InputProps>(
  (
    { className, label, error, icon, leftIcon, fullWidth, type = 'text', ...props },
    ref
  ) => {
    // Prefer icon, but fallback to leftIcon for compatibility
    const renderIcon = icon || leftIcon;
    return (
      <div className={cn('flex flex-col gap-1.5', fullWidth ? 'w-full' : '')}>
        {label && (
          <label className="text-sm font-medium text-text-primary">{label}</label>
        )}
        <div className="relative">
          {renderIcon && (
            <div className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted">
              {renderIcon}
            </div>
          )}
          <input
            ref={ref}
            type={type}
            className={cn(
              'flex h-10 w-full rounded-lg border border-surface-border bg-white px-3 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-accent/20 focus:border-accent transition-all duration-200',
              renderIcon ? 'pl-10' : '',
              error ? 'border-red-500 focus:border-red-500 focus:ring-red-200' : '',
              className
            )}
            {...props}
          />
        </div>
        {error && <span className="text-xs text-red-500">{error}</span>}
      </div>
    );
  }
);