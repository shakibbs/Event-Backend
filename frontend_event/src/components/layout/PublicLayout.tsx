import React, { useEffect, useState } from 'react';
import { motion, useScroll } from 'framer-motion';
import { Hexagon, Menu, X } from 'lucide-react';
import { Button } from '../ui/Button';
import { PublicViewType } from '../../types';
interface PublicLayoutProps {
  children: React.ReactNode;
  currentView: PublicViewType;
  onViewChange: (view: PublicViewType) => void;
}
export function PublicLayout({
  children,
  currentView,
  onViewChange
}: PublicLayoutProps) {
  const [isScrolled, setIsScrolled] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const { scrollY } = useScroll();
  useEffect(() => {
    return scrollY.onChange((latest) => {
      setIsScrolled(latest > 50);
    });
  }, [scrollY]);
  return (
    <div className="min-h-screen bg-warm-gray font-sans text-warm-text">
      {/* Navigation Bar */}
      <motion.nav
        className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${isScrolled ? 'bg-white/90 backdrop-blur-md shadow-sm py-3' : 'bg-transparent py-6'}`}>

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between">
            {/* Logo */}
            <div
              className="flex items-center gap-2 cursor-pointer"
              onClick={() => onViewChange('landing')}>

              <Hexagon
                className="h-8 w-8 text-coral fill-coral/10"
                strokeWidth={1.5} />

              <span
                className={`font-display font-bold text-xl tracking-tight ${isScrolled ? 'text-gray-900' : 'text-gray-900'}`}>

                EventFlow
              </span>
            </div>

            {/* Desktop Nav */}
            <div className="hidden md:flex items-center gap-8">
              <button
                onClick={() => onViewChange('landing')}
                className="text-sm font-medium text-gray-600 hover:text-coral transition-colors">
                Events
              </button>
              <button
                onClick={() => onViewChange && onViewChange('about')}
                className="text-sm font-medium text-gray-600 hover:text-coral transition-colors">
                About
              </button>
              <div className="h-4 w-px bg-gray-300 mx-2" />
              <button
                onClick={() => onViewChange('login')}
                className="text-sm font-medium text-gray-900 hover:text-coral transition-colors">
                Sign In
              </button>
              <Button
                onClick={() => onViewChange('register')}
                className="bg-coral hover:bg-coral-dark text-white border-none shadow-lg shadow-coral/30 rounded-full px-6">

                Get Started
              </Button>
            </div>

            {/* Mobile Menu Button */}
            <div className="md:hidden">
              <button
                onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
                className="text-gray-900 p-2">

                {isMobileMenuOpen ? <X /> : <Menu />}
              </button>
            </div>
          </div>
        </div>

        {/* Mobile Menu */}
        {isMobileMenuOpen &&
        <motion.div
          initial={{
            opacity: 0,
            height: 0
          }}
          animate={{
            opacity: 1,
            height: 'auto'
          }}
          className="md:hidden bg-white border-t border-gray-100">

            <div className="px-4 py-6 space-y-4 flex flex-col">
              <button
              onClick={() => {
                onViewChange('landing');
                setIsMobileMenuOpen(false);
              }}
              className="text-left font-medium text-gray-900 py-2">

                Events
              </button>
              <button className="text-left font-medium text-gray-900 py-2">
                About
              </button>
              <button
                onClick={() => {
                  onViewChange && onViewChange('about');
                  setIsMobileMenuOpen(false);
                }}
                className="text-left font-medium text-gray-900 py-2">
                About
              </button>
              <button
              onClick={() => {
                onViewChange('login');
                setIsMobileMenuOpen(false);
              }}
              className="text-left font-medium text-gray-900 py-2">

                Sign In
              </button>
              <Button
              onClick={() => {
                onViewChange('register');
                setIsMobileMenuOpen(false);
              }}
              className="w-full bg-coral hover:bg-coral-dark text-white border-none">

                Get Started
              </Button>
            </div>
          </motion.div>
        }
      </motion.nav>

      {/* Main Content */}
      <main className="pt-20">{children}</main>

      {/* Footer */}
      <footer className="bg-white border-t border-gray-100 py-12 mt-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-8 lg:px-12">
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-y-10 gap-x-16">
            {/* Brand & Social */}
            <div className="col-span-1 flex flex-col justify-between">
              <div>
                <div className="flex items-center gap-2 mb-4">
                  <Hexagon className="h-6 w-6 text-coral fill-coral/10" strokeWidth={1.5} />
                  <span className="font-display font-bold text-lg text-gray-900">EventFlow</span>
                </div>
                <p className="text-sm text-gray-500 leading-relaxed mb-4">
                  EventFlow is a modern event management platform designed to simplify event discovery, organization, and participation. We help organizers create, manage, and promote events seamlessly while providing attendees with a smooth and engaging experience.
                </p>
              </div>
              <div className="flex space-x-4 mt-4">
                <a href="#" aria-label="Twitter" className="hover:text-blue-400 transition-colors"><svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M24 4.557a9.93 9.93 0 0 1-2.828.775A4.932 4.932 0 0 0 23.337 3.1a9.864 9.864 0 0 1-3.127 1.195A4.916 4.916 0 0 0 16.616 2c-2.73 0-4.942 2.21-4.942 4.932 0 .386.045.762.127 1.124C7.728 7.89 4.1 6.13 1.671 3.149c-.423.722-.666 1.561-.666 2.475 0 1.708.87 3.216 2.188 4.099a4.904 4.904 0 0 1-2.237-.616c-.054 2.281 1.581 4.415 3.949 4.89a4.936 4.936 0 0 1-2.224.084c.627 1.956 2.444 3.377 4.6 3.417A9.867 9.867 0 0 1 0 21.543a13.94 13.94 0 0 0 7.548 2.209c9.057 0 14.009-7.496 14.009-13.986 0-.213-.005-.425-.014-.636A9.936 9.936 0 0 0 24 4.557z"/></svg></a>
                <a href="#" aria-label="LinkedIn" className="hover:text-blue-600 transition-colors"><svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M19 0h-14c-2.76 0-5 2.24-5 5v14c0 2.76 2.24 5 5 5h14c2.76 0 5-2.24 5-5v-14c0-2.76-2.24-5-5-5zm-11 19h-3v-10h3v10zm-1.5-11.28c-.966 0-1.75-.79-1.75-1.75s.784-1.75 1.75-1.75 1.75.79 1.75 1.75-.784 1.75-1.75 1.75zm13.5 11.28h-3v-5.604c0-1.337-.025-3.063-1.868-3.063-1.868 0-2.154 1.459-2.154 2.967v5.7h-3v-10h2.881v1.367h.041c.401-.761 1.379-1.563 2.841-1.563 3.039 0 3.6 2.001 3.6 4.601v5.595z"/></svg></a>
                <a href="#" aria-label="Facebook" className="hover:text-blue-500 transition-colors"><svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M22.675 0h-21.35c-.733 0-1.325.592-1.325 1.326v21.348c0 .733.592 1.326 1.325 1.326h11.495v-9.294h-3.124v-3.622h3.124v-2.671c0-3.1 1.893-4.788 4.659-4.788 1.325 0 2.463.099 2.797.143v3.24l-1.918.001c-1.504 0-1.797.715-1.797 1.763v2.312h3.587l-.467 3.622h-3.12v9.294h6.116c.733 0 1.325-.593 1.325-1.326v-21.349c0-.734-.592-1.326-1.325-1.326z"/></svg></a>
              </div>
            </div>
            <div>
              <h4 className="font-bold text-gray-900 mb-4">Platform</h4>
              <ul className="space-y-2 text-sm text-gray-500 pl-1">
                <li><a href="#" className="hover:text-coral transition-colors">Browse Events – Discover upcoming public and private events</a></li>
                <li><a href="#" className="hover:text-coral transition-colors">Organizers – Tools for managing and hosting successful events</a></li>
                <li><a href="#" className="hover:text-coral transition-colors">Pricing – Flexible plans for individuals and organizations</a></li>
              </ul>
            </div>

            {/* Company */}
            <div>
              <h4 className="font-bold text-gray-900 mb-4">Company</h4>
              <ul className="space-y-2 text-sm text-gray-500 pl-1">
                <li><a href="#" className="hover:text-coral transition-colors">About Us – Learn more about EventFlow and our mission</a></li>
                <li><a href="#" className="hover:text-coral transition-colors">Careers – Join our growing team (Coming Soon)</a></li>
                <li className="flex items-center gap-2"><span className="text-coral text-base">📧</span><span className="font-semibold">Email:</span><span className="font-mono">bseventmanagement23@gmail.com</span></li>
                <li className="flex items-center gap-2"><span className="text-coral text-base">📞</span><span className="font-semibold">Phone:</span><span className="font-mono">+880 1850-765614</span></li>
                <li className="flex items-center gap-2"><span className="text-coral text-base">📍</span><span className="font-semibold">Address:</span><span className="font-mono">Mohakhali, Dhaka, Bangladesh</span></li>
              </ul>
            </div>

            {/* Legal */}
            <div>
              <h4 className="font-bold text-gray-900 mb-4">Legal</h4>
              <ul className="space-y-2 text-sm text-gray-500 pl-1">
                <li><span className="hover:text-coral font-semibold cursor-pointer transition-colors">Privacy Policy</span><div className="text-xs text-gray-400 mt-1">We respect your privacy. User data is securely stored and never shared with third parties without consent.</div></li>
                <li><span className="hover:text-coral font-semibold cursor-pointer transition-colors">Terms & Conditions</span><div className="text-xs text-gray-400 mt-1">By using EventFlow, you agree to follow our platform rules and policies. Misuse of the system may result in account suspension.</div></li>
              </ul>
            </div>

          </div>
          <div className="border-t border-gray-100 mt-12 pt-8 text-center text-sm text-gray-400">
            © {new Date().getFullYear()} EventFlow. All rights reserved.
          </div>
        </div>
      </footer>
    </div>);

}