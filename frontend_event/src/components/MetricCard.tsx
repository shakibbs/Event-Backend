import React from 'react';
import { motion } from 'framer-motion';
import { MetricData } from '../types';
import { cn } from '../lib/utils';
interface MetricCardProps {
  data: MetricData;
  index: number;
}
export function MetricCard({ data, index }: MetricCardProps) {
  return (
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
        duration: 0.4,
        delay: index * 0.1
      }}
      className="bg-white rounded-lg p-4 border border-surface-border shadow-sm hover:shadow-md transition-shadow duration-300 min-w-0">

      <div className="flex justify-between items-start mb-3">
        <span className="text-xs font-medium text-text-secondary">
          {data.label}
        </span>
        {data.icon &&
        <div className="p-1.5 bg-surface-secondary rounded-lg text-primary">
            <data.icon className="h-4 w-4" />
          </div>
        }
      </div>

      <div className="flex items-end justify-between">
        <h3 className="text-2xl font-bold text-primary tabular-nums tracking-tight">
          {data.value}
        </h3>
      </div>
      <p className="text-xs text-text-muted mt-1">{data.trendLabel}</p>
    </motion.div>);

}