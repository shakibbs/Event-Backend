import React, { useEffect, useState, createContext, useContext } from 'react';
import { User } from '../types';
import { loginApi, registerApi } from '../lib/api';
interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  register: (name: string, email: string, password: string) => Promise<void>;
}
const AuthContext = createContext<AuthContextType | undefined>(undefined);
export function AuthProvider({ children }: {children: React.ReactNode;}) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  useEffect(() => {
    // Check for existing session
    const storedUser = localStorage.getItem('eventflow_user');
    if (storedUser) {
      try {
        setUser(JSON.parse(storedUser));
      } catch (e) {
        console.error('Failed to parse user session');
        localStorage.removeItem('eventflow_user');
      }
    }
    setIsLoading(false);
  }, []);
  const login = async (email: string, password: string) => {
    setIsLoading(true);
    try {
      const data = await loginApi(email, password);
      // Assume backend returns { user, token } or { user, accessToken }
      const user = data.user || data;
      const token = data.token || data.accessToken || data.jwt || data.access_token;
      setUser(user);
      localStorage.setItem('eventflow_user', JSON.stringify(user));
      if (token) {
        localStorage.setItem('eventflow_token', token);
      }
    } catch (error) {
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (name: string, email: string, password: string) => {
    setIsLoading(true);
    try {
      const data = await registerApi(name, email, password);
      setUser(data.user || data);
      localStorage.setItem('eventflow_user', JSON.stringify(data.user || data));
    } catch (error) {
      throw error;
    } finally {
      setIsLoading(false);
    }
  };
  const logout = () => {
    setUser(null);
    localStorage.removeItem('eventflow_user');
    localStorage.removeItem('eventflow_token');
  };
  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        logout,
        register
      }}>

      {children}
    </AuthContext.Provider>);

}
export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}