import { Event, MetricData } from '../types';
import { Calendar, Users, DollarSign, TrendingUp } from 'lucide-react';

export const mockEvents: Event[] = [
{
  id: '1',
  title: 'Tech Innovation Summit 2024',
  startTime: '2024-03-15T09:00:00',
  location: 'Moscone Center, SF',
  attendees: 2450,
  capacity: 3000,
  eventStatus: 'UPCOMING',
  revenue: 1225000,
  organizer: 'Sarah Chen'
},
{
  id: '2',
  title: 'Global Leadership Conference',
  startTime: '2024-02-28T08:30:00',
  location: 'ExCeL London',
  attendees: 1800,
  capacity: 2000,
  eventStatus: 'ACTIVE',
  revenue: 900000,
  organizer: 'James Wilson'
},
{
  id: '3',
  title: 'Product Design Workshop',
  startTime: '2024-02-10T10:00:00',
  location: 'Virtual',
  attendees: 450,
  capacity: 500,
  eventStatus: 'COMPLETED',
  revenue: 45000,
  organizer: 'Elena Rodriguez'
},
{
  id: '4',
  title: 'Q1 Marketing Strategy',
  startTime: '2024-01-15T13:00:00',
  location: 'HQ - Room 404',
  attendees: 45,
  capacity: 50,
  eventStatus: 'COMPLETED',
  revenue: 0,
  organizer: 'Mike Ross'
},
{
  id: '5',
  title: 'Developer Days 2024',
  startTime: '2024-04-20T09:00:00',
  location: 'Austin Convention Center',
  attendees: 1200,
  capacity: 1500,
  eventStatus: 'UPCOMING',
  revenue: 360000,
  organizer: 'David Kim'
},
{
  id: '6',
  name: 'Sales Kickoff',
  date: '2024-01-05T09:00:00',
  location: 'Grand Hyatt NYC',
  attendees: 800,
  capacity: 800,
  status: 'completed',
  revenue: 0,
  organizer: 'Sarah Chen'
},
{
  id: '7',
  name: 'Customer Success Summit',
  date: '2024-05-10T08:00:00',
  location: 'Chicago, IL',
  attendees: 600,
  capacity: 800,
  status: 'cancelled',
  revenue: 0,
  organizer: 'James Wilson'
},
{
  id: '8',
  name: 'AI in Enterprise',
  date: '2024-03-22T14:00:00',
  location: 'Virtual',
  attendees: 3200,
  capacity: 5000,
  status: 'upcoming',
  revenue: 160000,
  organizer: 'Elena Rodriguez'
},
{
  id: '9',
  name: 'HR Policy Update',
  date: '2024-02-15T11:00:00',
  location: 'HQ - Main Hall',
  attendees: 150,
  capacity: 200,
  status: 'completed',
  revenue: 0,
  organizer: 'Mike Ross'
},
{
  id: '10',
  name: 'Summer Gala',
  date: '2024-06-15T19:00:00',
  location: 'The Ritz',
  attendees: 500,
  capacity: 500,
  status: 'upcoming',
  revenue: 125000,
  organizer: 'Sarah Chen'
},
{
  id: '11',
  name: 'Agile Transformation Workshop',
  date: '2024-03-05T09:00:00',
  location: 'Seattle, WA',
  attendees: 120,
  capacity: 150,
  status: 'upcoming',
  revenue: 60000,
  organizer: 'David Kim'
},
{
  id: '12',
  name: 'Cybersecurity Forum',
  date: '2024-04-12T08:30:00',
  location: 'Washington DC',
  attendees: 850,
  capacity: 1000,
  status: 'upcoming',
  revenue: 425000,
  organizer: 'James Wilson'
}];


export const mockMetrics: MetricData[] = [
{
  label: 'Total Revenue',
  value: '$2.4M',
  trend: 12.5,
  trendLabel: 'vs last month',
  icon: DollarSign
},
{
  label: 'Total Attendees',
  value: '12,450',
  trend: 8.2,
  trendLabel: 'vs last month',
  icon: Users
},
{
  label: 'Events Hosted',
  value: '45',
  trend: -2.4,
  trendLabel: 'vs last month',
  icon: Calendar
},
{
  label: 'Avg. Satisfaction',
  value: '4.8/5',
  trend: 4.1,
  trendLabel: 'vs last month',
  icon: TrendingUp
}];