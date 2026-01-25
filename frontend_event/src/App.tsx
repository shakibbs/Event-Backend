import { useState } from 'react';
import { Sidebar } from './components/layout/Sidebar';
import { PublicLayout } from './components/layout/PublicLayout';
import { Dashboard } from './pages/Dashboard';
import { EventsList } from './pages/EventsList';
import { CalendarView } from './pages/CalendarView';
import { Analytics } from './pages/Analytics';
import { Settings } from './pages/Settings';
import { UserManagement } from './pages/UserManagement';
import { RoleManagement } from './pages/RoleManagement';
import { PermissionManagement } from './pages/PermissionManagement';
import { ActivityList } from './pages/ActivityList';
import Landing from './pages/Landing';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { ViewType, PublicViewType } from './types';
import { motion, AnimatePresence } from 'framer-motion';
import { AuthProvider, useAuth } from './context/AuthContext';
import InvitationsPage from './pages/InvitationsPage';
import EventManagement from './pages/EventManagement';
import About from './pages/About';
import { Routes, Route, Navigate } from 'react-router-dom';
function AppContent() {
  const { isAuthenticated, isLoading } = useAuth();
  const [currentView, setCurrentView] = useState<ViewType>('dashboard');
  const [publicView, setPublicView] = useState<PublicViewType>('landing');
  // For InvitationsPage event selection (removed unused selectedEventId)
  // Loading state
  if (isLoading) {
    return (
      <div className="h-screen w-full flex items-center justify-center bg-surface-secondary">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary"></div>
      </div>);
  }
  // Authenticated View (Template 1 - Enterprise Dashboard)
  if (isAuthenticated) {
    return (
      <div className="flex h-screen w-full bg-surface-secondary overflow-hidden font-sans text-text-primary">
        <Sidebar currentView={currentView} onViewChange={setCurrentView} />
        <main className="flex-1 flex flex-col h-screen overflow-hidden">
          <div className="flex-1 overflow-y-auto p-4 sm:p-8 scroll-smooth custom-scrollbar">
            <div className="max-w-7xl mx-auto w-full">
              <AnimatePresence mode="wait">
                <motion.div
                  key={window.location.pathname}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -10 }}
                  transition={{ duration: 0.2 }}>
                  <Routes>
                    <Route path="/" element={<Dashboard />} />
                    <Route path="/dashboard" element={<Dashboard />} />
                    <Route path="/events" element={<EventsList />} />
                    <Route path="/event-management/:eventId" element={<EventManagement />} />
                    <Route path="/calendar" element={<CalendarView />} />
                    <Route path="/analytics" element={<Analytics />} />
                    <Route path="/settings" element={<Settings />} />
                    <Route path="/users" element={<UserManagement />} />
                    <Route path="/roles" element={<RoleManagement />} />
                    <Route path="/permissions" element={<PermissionManagement />} />
                    <Route path="/activity" element={<ActivityList />} />
                    <Route path="/invitations" element={<InvitationsPage onBack={() => setCurrentView('events')} />} />
                    <Route path="*" element={<Navigate to="/dashboard" />} />
                  </Routes>
                </motion.div>
              </AnimatePresence>
            </div>
          </div>
        </main>
      </div>
    );
  }
  // Public View (Template 4 - Event First)
  const renderPublicView = () => {
    switch (publicView) {
      case 'landing':
        return <Landing onViewChange={setPublicView} />;
      case 'login':
        return <Login onViewChange={setPublicView} />;
      case 'register':
        return <Register onViewChange={setPublicView} />;
      case 'about':
        return <About />;
      default:
        return <Landing onViewChange={setPublicView} />;
    }
  };
  return (
    <PublicLayout currentView={publicView} onViewChange={setPublicView}>
      <AnimatePresence mode="wait">
        <motion.div
          key={publicView}
          initial={{
            opacity: 0,
            y: 10
          }}
          animate={{
            opacity: 1,
            y: 0
          }}
          exit={{
            opacity: 0,
            y: -10
          }}
          transition={{
            duration: 0.3
          }}>

          {renderPublicView()}
        </motion.div>
      </AnimatePresence>
    </PublicLayout>);

}
export function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}