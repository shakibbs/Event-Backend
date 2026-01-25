import React from 'react';
import { motion } from 'framer-motion';
import { Calendar, MapPin, Users, ArrowRight } from 'lucide-react';
import { Event } from '../types';
import { Button } from './ui/Button';
interface PublicEventCardProps {
  event: Event;
  index: number;
}
export function PublicEventCard({ event, index }: PublicEventCardProps) {
  return (
    <motion.div
      initial={{
        opacity: 0,
        y: 20
      }}
      whileInView={{
        opacity: 1,
        y: 0
      }}
      viewport={{
        once: true
      }}
      transition={{
        duration: 0.5,
        delay: index * 0.1
      }}
      whileHover={{
        y: -8
      }}
      className="group bg-white rounded-2xl overflow-hidden shadow-sm hover:shadow-xl transition-all duration-300 border border-stone-100">

      {/* Image Container */}
      <div className="relative h-48 sm:h-64 overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent z-10" />
        <img
          src={
          event.image ||
          `https://source.unsplash.com/random/800x600?event&sig=${event.id}`
          }
          alt={event.title}
          className="w-full h-full object-cover transform group-hover:scale-105 transition-transform duration-700 ease-out" />


        {/* Date Badge */}
        <div className="absolute top-4 left-4 z-20 bg-white/90 backdrop-blur-sm rounded-lg p-2 text-center min-w-[60px] shadow-lg">
          <span className="block text-xs font-bold text-coral uppercase tracking-wider">
            {new Date(event.date).toLocaleString('default', {
              month: 'short'
            })}
          </span>
          <span className="block text-xl font-display font-bold text-gray-900">
            {new Date(event.date).getDate()}
          </span>
        </div>

        {/* Category Badge */}
        <div className="absolute top-4 right-4 z-20">
          <span className="px-3 py-1 bg-coral text-white text-xs font-medium rounded-full shadow-lg">
            {event.category || 'Conference'}
          </span>
        </div>
      </div>

      {/* Content */}
      <div className="p-6">
        <h3 className="font-display text-2xl font-bold text-gray-900 mb-2 group-hover:text-coral transition-colors line-clamp-2">
          {event.title}
        </h3>

        <div className="space-y-3 mb-6">
          <div className="flex items-center text-gray-500 text-sm">
            <Calendar className="h-4 w-4 mr-2 text-coral" />
            <span>{event.time}</span>
          </div>
          <div className="flex items-center text-gray-500 text-sm">
            <MapPin className="h-4 w-4 mr-2 text-coral" />
            <span className="truncate">{event.location}</span>
          </div>
          <div className="flex items-center text-gray-500 text-sm">
            <Users className="h-4 w-4 mr-2 text-coral" />
            <span>{event.attendees} attending</span>
          </div>
        </div>

        <div className="flex items-center justify-between pt-4 border-t border-gray-100">
          <span className="text-lg font-bold text-gray-900">
            {event.price || 'Free'}
          </span>
          <button className="text-coral font-medium text-sm flex items-center group/btn hover:underline">
            View Details
            <ArrowRight className="h-4 w-4 ml-1 transform group-hover/btn:translate-x-1 transition-transform" />
          </button>
        </div>
      </div>
    </motion.div>);

}