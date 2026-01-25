import React from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Map } from 'lucide-react';

/**
 * Header Component
 * Reusable header for all pages with consistent styling
 * 
 * @param {Object} props
 * @param {string} props.title - Page title
 * @param {string} props.subtitle - Subtitle/description
 * @param {string} props.lastUpdated - Last update timestamp
 * @param {number} props.activeAlerts - Number of active alerts
 * @param {boolean} props.showMapButton - Whether to show Map button
 * @param {boolean} props.showStats - Whether to show stats (Last Updated, Active Alerts)
 */
export default function Header({ 
  title = "Traffic Monitor",
  subtitle = "Real-time anomaly detection • Vancouver, BC",
  lastUpdated = "3:00 PM",
  activeAlerts = 0,
  showMapButton = true,
  showStats = true
}) {
  return (
    <motion.div
      className="relative z-10 border-b border-slate-800 bg-slate-900/80 backdrop-blur-xl"
      initial={{ y: -100, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      transition={{ duration: 0.6, ease: "easeOut" }}
    >
      <div className="max-w-7xl mx-auto px-6 py-6">
        <div className="flex items-center justify-between">
          {/* Title Section */}
          <div>
            <h1 className="text-4xl font-bold tracking-tight mb-2 text-cyan-400">
              <Link to="/">
                {title}
              </Link>
            </h1>
            <p className="text-slate-400 text-sm font-['JetBrains_Mono']">
              {subtitle}
            </p>
          </div>
          
          {/* Right Section - Stats & Map Button */}
          <div className="flex items-center space-x-6">
            {/* Map Button */}
            {showMapButton && (
              <Link to="/map">
                <button className="flex items-center space-x-2 px-4 py-2 bg-cyan-500/10 hover:bg-cyan-500/20 border border-cyan-500/30 hover:border-cyan-500/50 text-cyan-400 rounded-lg transition-all duration-300">
                  <Map className="w-4 h-4" />
                  <span className="font-semibold">Map</span>
                </button>
              </Link>
            )}

            {/* Stats Section */}
            {showStats && (
              <>
                <div className="text-right">
                  <div className="text-sm text-slate-400">Last Updated</div>
                  <div className="text-lg font-semibold font-['JetBrains_Mono']">
                    {lastUpdated}
                  </div>
                </div>
                <div className="text-right">
                  <div className="text-sm text-slate-400">Active Alerts</div>
                  <div className="text-lg font-semibold font-['JetBrains_Mono'] text-red-400">
                    {activeAlerts}
                  </div>
                </div>
              </>
            )}

            {/* Live Indicator */}
            <motion.div
              className="w-3 h-3 bg-green-400 rounded-full"
              animate={{ scale: [1, 1.2, 1], opacity: [1, 0.7, 1] }}
              transition={{ duration: 2, repeat: Infinity }}
            />
          </div>
        </div>
      </div>
    </motion.div>
  );
}