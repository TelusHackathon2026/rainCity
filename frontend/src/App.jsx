import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { AlertTriangle, TrendingUp, Clock, MapPin, Car, UserX, Construction, Flame } from 'lucide-react';
import Header from './Header';

// Hardcoded alert data
const MOCK_ALERTS = [
  {
    id: 1,
    location: 'Main St & Broadway',
    currentScore: 144,
    baseline: 52,
    delta: 92,
    severity: 'CRITICAL',
    detectedObjects: {
      person_laying: 1,
      traffic_cones: 12,
      stopped_vehicles: 2,
      moving_vehicles: 48
    },
    timestamp: '3:00 PM',
    description: '1 person laying, 12 cones, 2 stopped vehicles'
  },
  {
    id: 2,
    location: 'Granville St & 41st Ave',
    currentScore: 87,
    baseline: 56,
    delta: 31,
    severity: 'HIGH',
    detectedObjects: {
      traffic_cones: 8,
      stopped_vehicles: 1,
      moving_vehicles: 62
    },
    timestamp: '3:00 PM',
    description: 'Heavy traffic, 8 cones detected'
  },
  {
    id: 3,
    location: 'Cambie St & 12th Ave',
    currentScore: 45,
    baseline: 31,
    delta: 14,
    severity: 'MEDIUM',
    detectedObjects: {
      traffic_cones: 5,
      stopped_vehicles: 1,
      moving_vehicles: 28
    },
    timestamp: '3:00 PM',
    description: '5 traffic cones, 1 stopped vehicle'
  },
  {
    id: 4,
    location: 'Hastings St & Commercial Dr',
    currentScore: 78,
    baseline: 64,
    delta: 14,
    severity: 'MEDIUM',
    detectedObjects: {
      emergency_vehicle: 1,
      traffic_cones: 4,
      moving_vehicles: 52
    },
    timestamp: '2:45 PM',
    description: '1 emergency vehicle, 4 cones'
  },
  {
    id: 5,
    location: 'Burrard St & Davie St',
    currentScore: 156,
    baseline: 105,
    delta: 51,
    severity: 'CRITICAL',
    detectedObjects: {
      crash_debris: 1,
      stopped_vehicles: 3,
      traffic_cones: 15,
      moving_vehicles: 71
    },
    timestamp: '2:45 PM',
    description: 'Crash debris, 3 stopped vehicles, 15 cones'
  },
  {
    id: 6,
    location: 'Knight St & 49th Ave',
    currentScore: 92,
    baseline: 68,
    delta: 24,
    severity: 'HIGH',
    detectedObjects: {
      construction_equipment: 2,
      traffic_cones: 10,
      moving_vehicles: 54
    },
    timestamp: '2:30 PM',
    description: '2 construction equipment, 10 cones'
  }
];

export default function TrafficDashboard() {
  const [selectedAlert, setSelectedAlert] = useState(null);

  const getSeverityColor = (severity) => {
    switch (severity) {
      case 'CRITICAL':
        return 'bg-red-600';
      case 'HIGH':
        return 'bg-orange-600';
      case 'MEDIUM':
        return 'bg-yellow-600';
      default:
        return 'bg-slate-600';
    }
  };

  const getSeverityBadgeColor = (severity) => {
    switch (severity) {
      case 'CRITICAL':
        return 'bg-red-500/20 text-red-400 border-red-500/50';
      case 'HIGH':
        return 'bg-orange-500/20 text-orange-400 border-orange-500/50';
      case 'MEDIUM':
        return 'bg-yellow-500/20 text-yellow-400 border-yellow-500/50';
      default:
        return 'bg-slate-500/20 text-slate-400 border-slate-500/50';
    }
  };

  const getSeverityIcon = (severity) => {
    if (severity === 'CRITICAL') return '';
    if (severity === 'HIGH') return '';
    return '';
  };

  return (
    <div className="min-h-screen bg-slate-950 font-['Outfit'] text-white">
      {/* Animated Background Grid */}
      <motion.div
        className="fixed inset-0 opacity-10"
        initial={{ opacity: 0 }}
        animate={{ opacity: 0.1 }}
        transition={{ duration: 1 }}
      >
        <motion.div
          className="absolute inset-0"
          style={{
            backgroundImage: `
              linear-gradient(rgba(59, 130, 246, 0.1) 1px, transparent 1px),
              linear-gradient(90deg, rgba(59, 130, 246, 0.1) 1px, transparent 1px)
            `,
            backgroundSize: '60px 60px'
          }}
          animate={{
            x: [0, 60],
            y: [0, 60]
          }}
          transition={{
            duration: 20,
            repeat: Infinity,
            ease: "linear"
          }}
        />
      </motion.div>

      {/* Header */}
      <Header 
        lastUpdated="3:00 PM"
        activeAlerts={MOCK_ALERTS.length}
        showMapButton={true}
        showStats={true}
      />

      {/* Main Content */}
      <div className="relative z-10 max-w-7xl mx-auto px-6 py-8">
        {/* Stats Bar */}
        <div className="grid grid-cols-3 gap-4 mb-8">
          <StatCard
            label="Critical Alerts"
            value={MOCK_ALERTS.filter(a => a.severity === 'CRITICAL').length}
            color="red"
            delay={0}
          />
          <StatCard
            label="High Priority"
            value={MOCK_ALERTS.filter(a => a.severity === 'HIGH').length}
            color="orange"
            delay={0.1}
          />
          <StatCard
            label="Medium Priority"
            value={MOCK_ALERTS.filter(a => a.severity === 'MEDIUM').length}
            color="yellow"
            delay={0.2}
          />
        </div>

        {/* Alert List Header */}
        <motion.div
          className="flex items-center justify-between mb-6"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.3 }}
        >
          <h2 className="text-2xl font-bold flex items-center space-x-3">
            <AlertTriangle className="w-6 h-6 text-red-400" />
            <span>Active Alerts</span>
          </h2>
          <div className="text-sm text-slate-400 font-['JetBrains_Mono']">
            Sorted by Score Delta (Highest First)
          </div>
        </motion.div>

        {/* Alert Cards */}
        <motion.div
          className="space-y-4"
          initial="hidden"
          animate="visible"
          variants={{
            visible: {
              transition: {
                staggerChildren: 0.1,
                delayChildren: 0.4
              }
            }
          }}
        >
          {MOCK_ALERTS.map((alert, index) => (
            <AlertCard
              key={alert.id}
              alert={alert}
              index={index}
              getSeverityColor={getSeverityColor}
              getSeverityBadgeColor={getSeverityBadgeColor}
              getSeverityIcon={getSeverityIcon}
              isSelected={selectedAlert?.id === alert.id}
              onClick={() => setSelectedAlert(alert)}
            />
          ))}
        </motion.div>
      </div>
    </div>
  );
}

// Stat Card Component
function StatCard({ label, value, color, delay }) {
  const colorClasses = {
    red: 'bg-red-500/10 border-red-500/30',
    orange: 'bg-orange-500/10 border-orange-500/30',
    yellow: 'bg-yellow-500/10 border-yellow-500/30'
  };

  const textColors = {
    red: 'text-red-400',
    orange: 'text-orange-400',
    yellow: 'text-yellow-400'
  };

  return (
    <motion.div
      className={`${colorClasses[color]} border backdrop-blur-sm rounded-xl p-6`}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay }}
      whileHover={{ scale: 1.02, transition: { duration: 0.2 } }}
    >
      <div className="text-slate-400 text-sm mb-2">{label}</div>
      <div className={`text-4xl font-bold font-['JetBrains_Mono'] ${textColors[color]}`}>
        {value}
      </div>
    </motion.div>
  );
}

// Alert Card Component
function AlertCard({ alert, index, getSeverityColor, getSeverityBadgeColor, getSeverityIcon, isSelected, onClick }) {
  return (
    <motion.div
      className={`bg-slate-900/50 backdrop-blur-sm border border-slate-800 rounded-xl overflow-hidden transition-all duration-300 cursor-pointer hover:border-slate-700 hover:shadow-2xl ${
        isSelected ? 'ring-2 ring-cyan-500' : ''
      }`}
      variants={{
        hidden: { opacity: 0, y: 20 },
        visible: { opacity: 1, y: 0 }
      }}
      transition={{ duration: 0.5 }}
      whileHover={{ scale: 1.01, transition: { duration: 0.2 } }}
      whileTap={{ scale: 0.99 }}
      onClick={onClick}
      layout
    >
      {/* Severity Indicator Bar */}
      <motion.div
        className={`h-1.5 ${getSeverityColor(alert.severity)}`}
        initial={{ scaleX: 0 }}
        animate={{ scaleX: 1 }}
        transition={{ duration: 0.5, delay: 0.1 }}
        style={{ transformOrigin: "left" }}
      />

      <div className="p-6">
        <div className="flex items-start justify-between mb-4">
          <div className="flex-1">
            <div className="flex items-center space-x-3 mb-2">
              <motion.span
                className="text-2xl"
                initial={{ scale: 0, rotate: -180 }}
                animate={{ scale: 1, rotate: 0 }}
                transition={{ duration: 0.5, type: "spring" }}
              >
                {getSeverityIcon(alert.severity)}
              </motion.span>
              <h3 className="text-xl font-bold">{alert.location}</h3>
              <span className={`px-3 py-1 rounded-full text-xs font-semibold border ${getSeverityBadgeColor(alert.severity)}`}>
                {alert.severity}
              </span>
            </div>
            <p className="text-slate-400 text-sm flex items-center space-x-2">
              <MapPin className="w-4 h-4" />
              <span>{alert.description}</span>
            </p>
          </div>

          {/* Delta Score */}
          <div className="text-right ml-6">
            <div className="flex items-center space-x-2 mb-1">
              <TrendingUp className="w-5 h-5 text-red-400" />
              <motion.span
                className="text-3xl font-bold font-['JetBrains_Mono'] text-red-400"
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ duration: 0.5, type: "spring", stiffness: 200 }}
              >
                +{alert.delta}
              </motion.span>
            </div>
            <div className="text-xs text-slate-500">Score Delta</div>
          </div>
        </div>

        {/* Score Breakdown */}
        <div className="grid grid-cols-3 gap-4 mb-4">
          <motion.div
            className="bg-slate-800/50 rounded-lg p-3"
            whileHover={{ backgroundColor: "rgba(30, 41, 59, 0.7)" }}
          >
            <div className="text-xs text-slate-500 mb-1">Current Score</div>
            <div className="text-xl font-bold font-['JetBrains_Mono'] text-white">
              {alert.currentScore}
            </div>
          </motion.div>
          <motion.div
            className="bg-slate-800/50 rounded-lg p-3"
            whileHover={{ backgroundColor: "rgba(30, 41, 59, 0.7)" }}
          >
            <div className="text-xs text-slate-500 mb-1">Baseline (30min avg)</div>
            <div className="text-xl font-bold font-['JetBrains_Mono'] text-slate-400">
              {alert.baseline}
            </div>
          </motion.div>
          <motion.div
            className="bg-slate-800/50 rounded-lg p-3"
            whileHover={{ backgroundColor: "rgba(30, 41, 59, 0.7)" }}
          >
            <div className="text-xs text-slate-500 mb-1 flex items-center space-x-1">
              <Clock className="w-3 h-3" />
              <span>Time</span>
            </div>
            <div className="text-xl font-bold font-['JetBrains_Mono'] text-cyan-400">
              {alert.timestamp}
            </div>
          </motion.div>
        </div>

        {/* Detected Objects */}
        <motion.div
          className="flex flex-wrap gap-2"
          initial="hidden"
          animate="visible"
          variants={{
            visible: {
              transition: {
                staggerChildren: 0.05
              }
            }
          }}
        >
          {alert.detectedObjects.person_laying && (
            <ObjectBadge
              icon={<UserX className="w-3 h-3" />}
              label="Person Laying"
              count={alert.detectedObjects.person_laying}
              critical
            />
          )}
          {alert.detectedObjects.crash_debris && (
            <ObjectBadge
              icon={<Flame className="w-3 h-3" />}
              label="Crash Debris"
              count={alert.detectedObjects.crash_debris}
              critical
            />
          )}
          {alert.detectedObjects.emergency_vehicle && (
            <ObjectBadge
              icon={<Car className="w-3 h-3" />}
              label="Emergency Vehicle"
              count={alert.detectedObjects.emergency_vehicle}
            />
          )}
          {alert.detectedObjects.stopped_vehicles && (
            <ObjectBadge
              icon={<Car className="w-3 h-3" />}
              label="Stopped Vehicles"
              count={alert.detectedObjects.stopped_vehicles}
            />
          )}
          {alert.detectedObjects.traffic_cones && (
            <ObjectBadge
              icon={<Construction className="w-3 h-3" />}
              label="Traffic Cones"
              count={alert.detectedObjects.traffic_cones}
            />
          )}
          {alert.detectedObjects.construction_equipment && (
            <ObjectBadge
              icon={<Construction className="w-3 h-3" />}
              label="Construction Equipment"
              count={alert.detectedObjects.construction_equipment}
            />
          )}
        </motion.div>
      </div>
    </motion.div>
  );
}

// Object Badge Component
function ObjectBadge({ icon, label, count, critical = false }) {
  return (
    <motion.div
      className={`flex items-center space-x-2 px-3 py-1.5 rounded-full text-xs font-semibold border ${
        critical
          ? 'bg-red-500/10 text-red-400 border-red-500/30'
          : 'bg-slate-800/50 text-slate-300 border-slate-700'
      }`}
      variants={{
        hidden: { opacity: 0, scale: 0 },
        visible: { opacity: 1, scale: 1 }
      }}
      whileHover={{ scale: 1.05 }}
      transition={{ duration: 0.2 }}
    >
      {icon}
      <span>{label}</span>
      <span className={`px-1.5 py-0.5 rounded-full font-['JetBrains_Mono'] ${
        critical ? 'bg-red-500 text-white' : 'bg-slate-700 text-white'
      }`}>
        {count}
      </span>
    </motion.div>
  );
}