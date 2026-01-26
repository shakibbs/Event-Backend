import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Hexagon, Mail, Lock, ArrowRight } from 'lucide-react';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { useAuth } from '../hooks/useAuth';
import { PublicViewType } from '../types';
interface LoginProps {
  onViewChange: (view: PublicViewType) => void;
}
export function Login({ onViewChange }: LoginProps) {
  const { login, isLoading } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) return;
    await login(email, password);
  };
  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4 py-12">
      <motion.div
        initial={{
          opacity: 0,
          y: 20
        }}
        animate={{
          opacity: 1,
          y: 0
        }}
        className="w-full max-w-md bg-white rounded-2xl shadow-xl shadow-gray-200/50 border border-gray-100 p-8 md:p-10">

        <div className="text-center mb-10">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-xl bg-coral/10 text-coral mb-6">
            <Hexagon className="h-6 w-6 fill-coral/20" />
          </div>
          <h1 className="font-display text-3xl font-bold text-gray-900 mb-2">
            Welcome back
          </h1>
          <p className="text-gray-500">
            Enter your details to access your account
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          <Input
            label="Email address"
            type="email"
            placeholder="you@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            icon={<Mail className="h-4 w-4" />} />


          <div>
            <Input
              label="Password"
              type="password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              icon={<Lock className="h-4 w-4" />} />

            <div className="flex justify-end mt-2">
              <button
                type="button"
                className="text-sm font-medium text-coral hover:text-coral-dark">

                Forgot password?
              </button>
            </div>
          </div>

          <Button
            type="submit"
            className="w-full h-12 bg-coral hover:bg-coral-dark text-white border-none rounded-xl text-base shadow-lg shadow-coral/20"
            isLoading={isLoading}>

            Sign In
          </Button>
        </form>

        <div className="mt-8 pt-8 border-t border-gray-100 text-center">
          <p className="text-gray-500 text-sm">
            Don't have an account?{' '}
            <button
              onClick={() => onViewChange('register')}
              className="font-bold text-gray-900 hover:text-coral transition-colors">

              Sign up for free
            </button>
          </p>
        </div>
      </motion.div>
    </div>);

}