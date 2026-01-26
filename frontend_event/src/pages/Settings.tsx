import React, { useState, useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';
import { Button } from '../components/ui/Button';
import { apiRequest } from '../lib/api';
import { User, Mail, Phone, Lock, LogOut } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export function Settings() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<'profile' | 'password'>('profile');
  const [profileData, setProfileData] = useState({
    fullName: user?.fullName || '',
    email: user?.email || ''
  });
  const [isEditingProfile, setIsEditingProfile] = useState(false);
  const [passwordData, setPasswordData] = useState({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    if (user) {
      setProfileData({
        fullName: user.fullName || '',
        email: user.email || ''
      });
    }
  }, [user]);

  const handleProfileUpdate = async () => {
    setLoading(true);
    setMessage(null);
    
    try {
      if (!user?.id) throw new Error('User ID not found');
      
      await apiRequest(`/users/${user.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          fullName: profileData.fullName,
          email: profileData.email
        })
      });
      
      setMessage({ type: 'success', text: 'Profile updated successfully!' });
      setIsEditingProfile(false);
    } catch (err: any) {
      setMessage({ type: 'error', text: err.message || 'Failed to update profile' });
    } finally {
      setLoading(false);
    }
  };

  const handlePasswordChange = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setMessage(null);
    
    try {
      if (passwordData.newPassword !== passwordData.confirmPassword) {
        throw new Error('Passwords do not match');
      }
      
      await apiRequest('/auth/change-password', {
        method: 'POST',
        body: JSON.stringify({
          oldPassword: passwordData.oldPassword,
          newPassword: passwordData.newPassword,
          confirmPassword: passwordData.confirmPassword
        })
      });
      
      setMessage({ type: 'success', text: 'Password changed successfully!' });
      setPasswordData({ oldPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err: any) {
      setMessage({ type: 'error', text: err.message || 'Failed to change password' });
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = async () => {
    try {
      await apiRequest('/auth/logout', { method: 'POST' });
      localStorage.removeItem('eventflow_token');
      localStorage.removeItem('eventflow_refresh_token');
      navigate('/login');
    } catch (err) {
      console.error('Logout error:', err);
    }
  };

  if (!user) {
    return <div className="text-center py-12">Loading...</div>;
  }

  const roleDisplay = typeof user.role === 'string' ? user.role : (user.role as any)?.name || 'User';

  return (
    <div className="min-h-screen bg-surface p-4 md:p-8">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-primary mb-2">Settings</h1>
          <p className="text-text-secondary">Manage your profile and account settings</p>
        </div>

        {/* Settings Card */}
        <div className="bg-surface rounded-xl shadow-md border border-surface-tertiary overflow-hidden">
          {/* Tabs */}
          <div className="flex border-b border-surface-tertiary bg-surface-secondary">
            <button
              onClick={() => setActiveTab('profile')}
              className={`flex-1 px-6 py-4 text-sm font-semibold transition-colors ${
                activeTab === 'profile'
                  ? 'text-accent border-b-2 border-accent'
                  : 'text-text-secondary hover:text-primary'
              }`}
            >
              <User className="w-4 h-4 inline mr-2" />
              Profile
            </button>
            <button
              onClick={() => setActiveTab('password')}
              className={`flex-1 px-6 py-4 text-sm font-semibold transition-colors ${
                activeTab === 'password'
                  ? 'text-accent border-b-2 border-accent'
                  : 'text-text-secondary hover:text-primary'
              }`}
            >
              <Lock className="w-4 h-4 inline mr-2" />
              Security
            </button>
          </div>

          {/* Tab Content */}
          <div className="p-8">
            {message && (
              <div className={`mb-6 p-4 rounded-lg ${
                message.type === 'success'
                  ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                  : 'bg-red-50 text-red-700 border border-red-200'
              }`}>
                {message.text}
              </div>
            )}

            {/* Profile Tab */}
            {activeTab === 'profile' && (
              <div className="space-y-6 max-w-2xl">
                <div>
                  <label className="block text-sm font-semibold text-primary mb-2">
                    <User className="w-4 h-4 inline mr-2" />
                    Full Name
                  </label>
                  {!isEditingProfile ? (
                    <div className="w-full border border-surface-tertiary rounded-lg px-4 py-2 text-sm bg-surface-secondary">
                      {profileData.fullName}
                    </div>
                  ) : (
                    <input
                      type="text"
                      value={profileData.fullName}
                      onChange={(e) => setProfileData({ ...profileData, fullName: e.target.value })}
                      className="w-full border border-surface-tertiary rounded-lg px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent focus:border-transparent transition-all bg-white"
                      placeholder="Enter your full name"
                    />
                  )}
                </div>

                <div>
                  <label className="block text-sm font-semibold text-primary mb-2">
                    <Mail className="w-4 h-4 inline mr-2" />
                    Email
                  </label>
                  {!isEditingProfile ? (
                    <div className="w-full border border-surface-tertiary rounded-lg px-4 py-2 text-sm bg-surface-secondary">
                      {profileData.email}
                    </div>
                  ) : (
                    <input
                      type="email"
                      value={profileData.email}
                      onChange={(e) => setProfileData({ ...profileData, email: e.target.value })}
                      className="w-full border border-surface-tertiary rounded-lg px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent focus:border-transparent transition-all bg-white"
                      placeholder="Enter your email"
                    />
                  )}
                </div>

                <div className="flex gap-3 pt-4">
                  {!isEditingProfile ? (
                    <Button type="button" onClick={() => setIsEditingProfile(true)} className="gap-2">
                      <User className="w-4 h-4" />
                      Edit Profile
                    </Button>
                  ) : (
                    <>
                      <Button 
                        type="button" 
                        onClick={handleProfileUpdate}
                        isLoading={loading}
                        className="gap-2"
                      >
                        Save Changes
                      </Button>
                      <Button 
                        variant="secondary" 
                        type="button" 
                        onClick={() => {
                          setIsEditingProfile(false);
                          setProfileData({
                            fullName: user?.fullName || '',
                            email: user?.email || ''
                          });
                        }}
                      >
                        Cancel
                      </Button>
                    </>
                  )}
                </div>
              </div>
            )}

            {/* Password Tab */}
            {activeTab === 'password' && (
              <form onSubmit={handlePasswordChange} className="space-y-6 max-w-md">
                <div>
                  <label className="block text-sm font-semibold text-primary mb-2">
                    Current Password
                  </label>
                  <input
                    type="password"
                    value={passwordData.oldPassword}
                    onChange={(e) => setPasswordData({ ...passwordData, oldPassword: e.target.value })}
                    className="w-full border border-surface-tertiary rounded-lg px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent focus:border-transparent transition-all"
                    placeholder="Enter current password"
                    required
                  />
                </div>

                <div>
                  <label className="block text-sm font-semibold text-primary mb-2">
                    New Password
                  </label>
                  <input
                    type="password"
                    value={passwordData.newPassword}
                    onChange={(e) => setPasswordData({ ...passwordData, newPassword: e.target.value })}
                    className="w-full border border-surface-tertiary rounded-lg px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent focus:border-transparent transition-all"
                    placeholder="Enter new password"
                    required
                  />
                </div>

                <div>
                  <label className="block text-sm font-semibold text-primary mb-2">
                    Confirm Password
                  </label>
                  <input
                    type="password"
                    value={passwordData.confirmPassword}
                    onChange={(e) => setPasswordData({ ...passwordData, confirmPassword: e.target.value })}
                    className="w-full border border-surface-tertiary rounded-lg px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent focus:border-transparent transition-all"
                    placeholder="Confirm new password"
                    required
                  />
                </div>

                <div className="flex gap-3 pt-4">
                  <Button type="submit" isLoading={loading} className="gap-2">
                    <Lock className="w-4 h-4" />
                    Change Password
                  </Button>
                </div>
              </form>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}