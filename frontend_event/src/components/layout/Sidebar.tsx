import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { cn } from '../../lib/utils';
import { ViewType, Event } from '../../types';
import { fetchEventsApi } from '../../lib/api';
import { useAuth } from '../../hooks/useAuth';
import { hasPermission, hasAnyPermission } from '../../utils/rolePermissions';
import {
  LayoutDashboard,
  CalendarDays,
  ListTodo,
  BarChart3,
  Settings,
  ChevronLeft,
  ChevronRight,
  LogOut,
  Hexagon } from
'lucide-react';
interface SidebarProps {
  currentView: ViewType;
  onViewChange: (view: ViewType) => void;
}
export function Sidebar({ currentView, onViewChange }: SidebarProps) {
    const navigate = useNavigate();
  const [isCollapsed, setIsCollapsed] = useState(false);
  const { user, logout } = useAuth();

  // Event preview state
  const [sidebarEvents, setSidebarEvents] = useState<Event[]>([]);
  const [eventLoading, setEventLoading] = useState(false);
  const [eventError, setEventError] = useState<string | null>(null);

  useEffect(() => {
    setEventLoading(true);
    fetchEventsApi(0, 3)
      .then((arr) => {
        setSidebarEvents(arr);
        setEventError(null);
      })
      .catch((err) => {
        setEventError(err && err.message ? err.message : 'Failed to load events');
      })
      .finally(() => setEventLoading(false));
  }, []);
  // Role-based menu items
  let menuItems = [
    {
      category: 'Management',
      items: [
        { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
        { id: 'events', label: 'Events', icon: ListTodo },
        { id: 'calendar', label: 'Calendar', icon: CalendarDays },
      ]
    },
    {
      category: 'Insights',
      items: [
        { id: 'activity', label: 'Activity', icon: BarChart3 },
      ]
    },
    {
      category: 'System',
      items: [
        { id: 'settings', label: 'Settings', icon: Settings },
      ]
    }
  ];

  // Only SuperAdmin can manage users, roles, permissions
  // Only Admin can view users
  const baseItems = menuItems[0].items;
  
  if (hasAnyPermission(user, ['user.view.all', 'user.manage.all'])) {
    baseItems.push({ id: 'users', label: 'Users', icon: Hexagon });
  }
  if (hasPermission(user, 'role.manage.all')) {
    baseItems.push({ id: 'roles', label: 'Roles', icon: Hexagon });
  }
  if (hasPermission(user, 'role.manage.all')) {
    baseItems.push({ id: 'permissions', label: 'Permissions', icon: Hexagon });
  }

  return (
    <motion.div
      initial={false}
      animate={{
        width: isCollapsed ? 80 : 280
      }}
      transition={{
        duration: 0.3,
        ease: 'easeInOut'
      }}
      className="h-screen bg-slate-800 text-white flex flex-col border-r border-slate-700 relative z-20 shrink-0">

      {/* Logo Area */}
      <div className="h-16 flex items-center px-6 border-b border-slate-700/50">
        <div className="flex items-center gap-3 text-accent-light">
          <Hexagon className="h-8 w-8 fill-accent-light/20" strokeWidth={1.5} />
          <AnimatePresence>
            {!isCollapsed &&
            <motion.span
              initial={{
                opacity: 0,
                x: -10
              }}
              animate={{
                opacity: 1,
                x: 0
              }}
              exit={{
                opacity: 0,
                x: -10
              }}
              className="font-bold text-xl tracking-tight text-white whitespace-nowrap">

                EventFlow
              </motion.span>
            }
          </AnimatePresence>
        </div>
      </div>

      {/* Navigation */}
      <div className="flex-1 overflow-y-auto py-6 px-3 space-y-8 custom-scrollbar">
        {menuItems.map((section, idx) =>
          <div key={idx}>
            {!isCollapsed &&
              <h3 className="px-4 text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
                {section.category}
              </h3>
            }
            <div className="space-y-1">
              {section.items.map((item) => {
                // Map view id to route
                const routeMap: Record<string, string> = {
                  dashboard: '/dashboard',
                  events: '/events',
                  calendar: '/calendar',
                  analytics: '/analytics',
                  activity: '/activity',
                  settings: '/settings',
                  users: '/users',
                  roles: '/roles',
                  permissions: '/permissions',
                  invitations: '/invitations'
                };
                const isActive = window.location.pathname.startsWith(routeMap[item.id] || '/');
                return (
                  <button
                    key={item.id}
                    onClick={() => navigate(routeMap[item.id] || '/')}
                    className={cn(
                      'w-full flex items-center gap-3 px-3 py-2.5 rounded-lg transition-all duration-200 group relative',
                      isActive ?
                        'bg-blue-500 text-white shadow-lg shadow-blue-500/20' :
                        'text-slate-400 hover:text-white hover:bg-slate-700/50'
                    )}
                    title={isCollapsed ? item.label : undefined}>

                    <item.icon
                      className={cn(
                        'h-5 w-5 shrink-0',
                        isActive ?
                          'text-white' :
                          'text-slate-400 group-hover:text-white'
                      )} />

                    {!isCollapsed &&
                      <span className="font-medium text-sm whitespace-nowrap">
                        {item.label}
                      </span>
                    }
                    {isActive && !isCollapsed &&
                      <motion.div
                        layoutId="activeIndicator"
                        className="absolute right-2 w-1.5 h-1.5 rounded-full bg-white" />

                    }
                  </button>
                );
              })}
            </div>
          </div>
        )}


      </div>

      {/* User Profile / Footer */}
      <div className="p-4 border-t border-slate-700/50">
        <div
          className={cn(
            'flex items-center gap-3',
            isCollapsed ? 'justify-center' : ''
          )}>

          <div className="h-9 w-9 rounded-full bg-gradient-to-tr from-accent to-purple-500 flex items-center justify-center text-white font-bold text-sm shrink-0 overflow-hidden">
            {user?.avatar ?
            <img
              src={user.avatar}
              alt={user.name}
              className="h-full w-full object-cover" /> :


            user?.name?.charAt(0) || 'U'
            }
          </div>
          {!isCollapsed &&
          <div className="flex-1 overflow-hidden">
              <p className="text-sm font-medium text-white truncate">
                {user?.name || 'User'}
              </p>
              <p className="text-xs text-slate-400 truncate capitalize">
                {typeof user?.role === 'string' ? user.role : (user?.role?.name || 'Guest')}
              </p>
            </div>
          }
          {!isCollapsed &&
          <button
            onClick={logout}
            className="text-slate-400 hover:text-white transition-colors"
            title="Sign out">

              <LogOut className="h-4 w-4" />
            </button>
          }
        </div>
      </div>

      {/* Collapse Toggle */}
      <button
        onClick={() => setIsCollapsed(!isCollapsed)}
        className="absolute -right-3 top-20 bg-slate-800 border border-slate-700 text-slate-400 hover:text-white rounded-full p-1 shadow-lg transition-colors z-50">

        {isCollapsed ?
        <ChevronRight className="h-3 w-3" /> :

        <ChevronLeft className="h-3 w-3" />
        }
      </button>
    </motion.div>);

}