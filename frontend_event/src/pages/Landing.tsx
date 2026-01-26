import React, { memo, useState } from 'react';
import { motion } from 'framer-motion';
import {
  Search,
  Calendar,
  MapPin,
  ArrowRight,
  Users,
  Hexagon } from
'lucide-react';
import { Button } from '../components/ui/Button';
import { PublicEventCard } from '../components/PublicEventCard';
import { PublicViewType } from '../types';
import { fetchPublicEventsApi } from '../lib/api';
import { useEffect } from 'react';
import type { Event } from '../lib/api';

interface LandingProps {
  onViewChange: (view: PublicViewType) => void;
}

const Landing: React.FC<LandingProps> = ({ onViewChange }) => {


  const [events, setEvents] = useState<Event[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showContact, setShowContact] = useState(false);

  useEffect(() => {
    setIsLoading(true);
    fetchPublicEventsApi()
      .then((arr) => {
        setEvents(arr);
        setError(null);
      })
      .catch((err) => {
        setError(err && err.message ? err.message : 'Failed to load events');
      })
      .finally(() => setIsLoading(false));
  }, []);

  return (
    <div className="w-full">
      {/* Hero Section */}
      <section className="relative pt-16 pb-32 overflow-hidden">
        <div className="absolute top-0 right-0 -mr-20 -mt-20 w-[600px] h-[600px] bg-cream rounded-full blur-3xl opacity-50 -z-10" />
        <div className="absolute bottom-0 left-0 -ml-20 -mb-20 w-[500px] h-[500px] bg-coral/10 rounded-full blur-3xl opacity-50 -z-10" />

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-3xl mx-auto">
            <motion.div
              initial={{
                opacity: 0,
                y: 20
              }}
              animate={{
                opacity: 1,
                y: 0
              }}
              transition={{
                duration: 0.6
              }}>

              <span className="inline-block py-1 px-3 rounded-full bg-coral/10 text-coral text-sm font-semibold mb-6">
                Discover Amazing Experiences
              </span>
              <h1 className="font-display text-5xl md:text-7xl font-bold text-gray-900 leading-tight mb-6">
                Find your next{' '}
                <span className="text-coral italic">unforgettable</span> event.
              </h1>
              <p className="text-xl text-gray-500 mb-10 leading-relaxed">
                Connect with communities, learn new skills, and create memories
                at thousands of events happening near you.
              </p>

              <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
                <Button
                  onClick={() => onViewChange('register')}
                  className="w-full sm:w-auto h-12 px-8 bg-coral hover:bg-coral-dark text-white border-none rounded-full text-lg shadow-xl shadow-coral/20">

                  Start for Free
                </Button>
                <Button
                  variant="outline"
                  className="w-full sm:w-auto h-12 px-8 rounded-full text-lg border-gray-300 hover:border-coral hover:text-coral bg-white">

                  Browse Events
                </Button>
              </div>
            </motion.div>
          </div>

          {/* Search Bar */}
          <motion.div
            initial={{
              opacity: 0,
              y: 40
            }}
            animate={{
              opacity: 1,
              y: 0
            }}
            transition={{
              duration: 0.6,
              delay: 0.2
            }}
            className="mt-16 bg-white p-4 rounded-2xl shadow-xl shadow-gray-200/50 max-w-4xl mx-auto border border-gray-100 flex flex-col md:flex-row gap-4">

            <div className="flex-1 flex items-center px-4 border-b md:border-b-0 md:border-r border-gray-100 pb-4 md:pb-0">
              <Search className="h-5 w-5 text-gray-400 mr-3" />
              <input
                type="text"
                placeholder="Search events, concerts, workshops..."
                className="w-full bg-transparent border-none focus:ring-0 text-gray-900 placeholder-gray-400" />

            </div>
            <div className="flex-1 flex items-center px-4 border-b md:border-b-0 md:border-r border-gray-100 pb-4 md:pb-0">
              <MapPin className="h-5 w-5 text-gray-400 mr-3" />
              <input
                type="text"
                placeholder="Location (e.g. San Francisco)"
                className="w-full bg-transparent border-none focus:ring-0 text-gray-900 placeholder-gray-400" />

            </div>
            <div className="flex-1 flex items-center px-4 pb-4 md:pb-0">
              <Calendar className="h-5 w-5 text-gray-400 mr-3" />
              <input
                type="text"
                placeholder="Any date"
                className="w-full bg-transparent border-none focus:ring-0 text-gray-900 placeholder-gray-400" />

            </div>
            <Button className="bg-gray-900 hover:bg-gray-800 text-white rounded-xl h-12 px-8">
              Search
            </Button>
          </motion.div>
        </div>
      </section>


      {/* Featured Events */}
      <section className="py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-end justify-between mb-12">
            <div>
              <h2 className="font-display text-4xl font-bold text-gray-900 mb-4">
                Upcoming Events
              </h2>
              <p className="text-gray-500 text-lg">
                Curated experiences just for you
              </p>
            </div>
            <button className="hidden sm:flex items-center text-coral font-medium hover:underline">
              View all events <ArrowRight className="h-4 w-4 ml-2" />
            </button>
          </div>

          {error && (
            <div className="text-red-500 text-sm mb-4">{error}</div>
          )}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {isLoading ? (
              <div className="col-span-full text-center text-gray-500">Loading events...</div>
            ) : (
              events.length === 0 ? (
                <div className="col-span-full text-center text-gray-500">No public upcoming events found.</div>
              ) : (
                events.map((event, index) => (
                  <div key={event.id} className="bg-white rounded-xl shadow p-6 flex flex-col justify-between">
                    <h3 className="font-display text-2xl font-bold text-gray-900 mb-2">{event.title}</h3>
                    <p className="text-gray-600">{event.description}</p>
                  </div>
                ))
              )
            )}
          </div>

          <div className="mt-12 text-center sm:hidden">
            <Button variant="outline" className="w-full">
              View all events
            </Button>
          </div>
        </div>
      </section>

      {/* Categories Section */}
      <section className="py-20 bg-cream/30">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="font-display text-3xl font-bold text-gray-900 mb-12 text-center">
            Explore by Category
          </h2>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
            {[
            'Music',
            'Business',
            'Food & Drink',
            'Arts',
            'Sports',
            'Tech'].
            map((category, i) =>
            <motion.div
              key={category}
              whileHover={{
                y: -5
              }}
              className="bg-white p-6 rounded-xl shadow-sm hover:shadow-md transition-all cursor-pointer text-center border border-gray-100">

                <div className="w-12 h-12 bg-coral/10 rounded-full flex items-center justify-center mx-auto mb-4 text-coral">
                  {
                [
                <Calendar />,
                <Users />,
                <MapPin />,
                <Search />,
                <ArrowRight />,
                <Hexagon />][
                i % 6]
                }
                </div>
                <h3 className="font-medium text-gray-900">{category}</h3>
              </motion.div>
            )}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-24 bg-gray-900 text-white overflow-hidden relative">
        <div className="absolute top-0 right-0 w-[600px] h-[600px] bg-coral rounded-full blur-[120px] opacity-20 -translate-y-1/2 translate-x-1/2" />

        <div className="max-w-4xl mx-auto px-4 text-center relative z-10">
          <h2 className="font-display text-4xl md:text-5xl font-bold mb-6">
            Ready to host your own event?
          </h2>
          <p className="text-xl text-gray-400 mb-10 max-w-2xl mx-auto">
            Join thousands of organizers who use EventFlow to create, manage,
            and grow their events.
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <Button
              onClick={() => onViewChange('register')}
              className="w-full sm:w-auto h-14 px-10 bg-coral hover:bg-coral-dark text-white border-none rounded-full text-lg font-medium">
              Get Started for Free
            </Button>
            <Button
              variant="outline"
              onClick={() => setShowContact(true)}
              className="w-full sm:w-auto h-14 px-10 rounded-full text-lg border-gray-700 hover:bg-gray-800 text-white">
              Contact Sales
            </Button>
          </div>
          {showContact && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
              <div className="bg-white rounded-xl shadow-2xl p-8 max-w-xs w-full flex flex-col items-center">
                <h3 className="text-xl font-bold mb-2 text-coral">Contact Sales</h3>
                <div className="mb-2 text-gray-800 text-center">
                  <div className="font-semibold">Email:</div>
                  <a href="mailto:bseventmanagement23@gmail.com" className="text-coral underline">bseventmanagement23@gmail.com</a>
                </div>
                <div className="mb-4 text-gray-800 text-center">
                  <div className="font-semibold">Phone:</div>
                  <a href="tel:+8801850765614" className="text-coral underline">+880 1850-765614</a>
                </div>
                <Button onClick={() => setShowContact(false)} className="mt-2 w-full bg-coral text-white">Close</Button>
              </div>
            </div>
          )}
        </div>
      </section>
    </div>
  );
};

export default Landing;