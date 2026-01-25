import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Event } from '../types';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from './ui/Button';
import { cn } from '../lib/utils';
interface CalendarProps {
  events: Event[];
}
export function Calendar({ events }: CalendarProps) {
  const days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  const [currentDate, setCurrentDate] = useState(new Date());
  const currentMonth = currentDate.toLocaleString('default', {
    month: 'long'
  });
  const currentYear = currentDate.getFullYear();
  
  // Handle month navigation
  const handlePreviousMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 1));
  };
  
  const handleNextMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 1));
  };
  
  const handleToday = () => {
    setCurrentDate(new Date());
  };

  // Generate calendar days for the current month
  const generateDays = () => {
    const daysArr = [];
    const firstDay = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1);
    const lastDay = new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 0);
    const startDate = new Date(firstDay);
    startDate.setDate(startDate.getDate() - firstDay.getDay()); // Start from Sunday of first week
    
    const endDate = new Date(lastDay);
    endDate.setDate(endDate.getDate() + (6 - lastDay.getDay())); // End on Saturday of last week
    
    let current = new Date(startDate);
    
    while (current <= endDate) {
      daysArr.push({
        day: current.getDate(),
        date: new Date(current),
        isPadding: current.getMonth() !== currentDate.getMonth()
      });
      current.setDate(current.getDate() + 1);
    }
    
    return daysArr;
  };
  const calendarDays = generateDays();
  const getEventsForDay = (day: number, isPadding: boolean) => {
    if (isPadding) return [];
    
    return events.filter((e) => {
      // Use startTime if available, otherwise fall back to date
      const dateStr = e.startTime || e.date;
      if (!dateStr) return false;
      
      const eventDate = new Date(dateStr);
      return (
        eventDate.getDate() === day &&
        eventDate.getMonth() === currentDate.getMonth() &&
        eventDate.getFullYear() === currentDate.getFullYear()
      );
    });
  };
  return (
    <div className="bg-white rounded-xl border border-surface-border shadow-sm overflow-hidden flex flex-col h-[600px]">
      {/* Header with Month and Navigation */}
      <div className="flex items-center justify-between p-4 border-b border-surface-border">
        <h2 className="text-lg font-semibold text-primary">
          {currentMonth} {currentYear}
        </h2>
        <div className="flex gap-2">
          <Button 
            variant="outline" 
            size="sm" 
            className="h-8 px-2 text-xs"
            onClick={handleToday}
          >
            Today
          </Button>
          <Button 
            variant="outline" 
            size="sm" 
            className="h-8 w-8 p-0"
            onClick={handlePreviousMonth}
          >
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <Button 
            variant="outline" 
            size="sm" 
            className="h-8 w-8 p-0"
            onClick={handleNextMonth}
          >
            <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {/* Status Legend */}
      <div className="px-4 py-3 border-b border-surface-border bg-slate-50/50">
        <div className="flex items-center gap-4 text-xs">
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded bg-blue-100 border border-blue-300"></div>
            <span className="font-medium text-text-secondary">Upcoming</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded bg-green-100 border border-green-300"></div>
            <span className="font-medium text-text-secondary">Active</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded bg-slate-100 border border-slate-300"></div>
            <span className="font-medium text-text-secondary">Completed</span>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-7 border-b border-surface-border bg-slate-50">
        {days.map((day) =>
        <div
          key={day}
          className="py-2 text-center text-xs font-semibold text-text-secondary uppercase tracking-wider">

            {day}
          </div>
        )}
      </div>

      <div className="grid grid-cols-7 flex-1 auto-rows-fr">
        {calendarDays.map((date, index) => {
          const dayEvents = getEventsForDay(date.day, date.isPadding);
          return (
            <div
              key={index}
              className={cn(
                'border-b border-r border-surface-border p-2 min-h-[80px] relative group transition-colors hover:bg-slate-50',
                date.isPadding ? 'bg-slate-50/50 text-text-muted' : 'bg-white'
              )}>

              <span
                className={cn(
                  'text-sm font-medium block mb-1',
                  date.date.toDateString() === new Date().toDateString() && !date.isPadding ?
                  'bg-accent text-white w-6 h-6 rounded-full flex items-center justify-center' :
                  ''
                )}>

                {date.day}
              </span>

              <div className="space-y-1">
                {dayEvents.slice(0, 3).map((event) => {
                  // Map backend eventStatus to display status
                  const statusStr = (event.eventStatus || event.status || 'UPCOMING').toString().toUpperCase();
                  let displayStatus = 'upcoming';
                  if (statusStr === 'ONGOING' || statusStr === 'ACTIVE') {
                    displayStatus = 'active';
                  } else if (statusStr === 'COMPLETED' || statusStr === 'CANCELLED') {
                    displayStatus = 'completed';
                  }
                  
                  const statusLabel = displayStatus === 'upcoming' ? 'Upcoming' : 
                                     displayStatus === 'active' ? 'Active' : 'Completed';
                  const eventTitle = event.name || event.title || 'Untitled Event';
                  return (
                    <motion.div
                      key={event.id}
                      whileHover={{
                        scale: 1.02
                      }}
                      className={cn(
                        'text-[10px] px-2 py-1 rounded truncate cursor-pointer font-medium transition-all',
                        displayStatus === 'upcoming' ?
                        'bg-blue-100 text-blue-700 hover:bg-blue-200' :
                        displayStatus === 'active' ?
                        'bg-green-100 text-green-700 hover:bg-green-200' :
                        'bg-slate-100 text-slate-700 hover:bg-slate-200'
                      )}
                      title={`${eventTitle} (${statusLabel})`}
                    >
                      <span className="inline-block max-w-[70px] truncate">{eventTitle}</span>
                      <span className="ml-1 font-semibold">•</span>
                    </motion.div>
                  );
                })}
                {dayEvents.length > 3 &&
                <div className="text-[10px] text-text-muted pl-1">
                    +{dayEvents.length - 3} more
                  </div>
                }
              </div>
            </div>);

        })}
      </div>
    </div>);

}