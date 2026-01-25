import React, { useState } from 'react';
import { AlertTriangle, TrendingUp, Clock, MapPin, Car, UserX, Construction, Flame } from 'lucide-react';

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
        return 'from-red-500 to-red-600';
      case 'HIGH':
        return 'from-orange-500 to-orange-600';
      case 'MEDIUM':
        return 'from-yellow-500 to-yellow-600';
      default:
        return 'from-slate-500 to-slate-600';
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
    if (severity === 'CRITICAL') return '🔴';
    if (severity === 'HIGH') return '🟠';
    return '🟡';
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 font-['Outfit'] text-white">
      {/* Animated Background Grid */}
      <div className="fixed inset-0 opacity-10">
        <div className="absolute inset-0" style={{
          backgroundImage: `
            linear-gradient(rgba(59, 130, 246, 0.1) 1px, transparent 1px),
            linear-gradient(90deg, rgba(59, 130, 246, 0.1) 1px, transparent 1px)
          `,
          backgroundSize: '60px 60px',
          animation: 'gridMove 20s linear infinite'
        }}></div>
      </div>

      <style>{`
        @keyframes gridMove {
          0% { transform: translate(0, 0); }
          100% { transform: translate(60px, 60px); }
        }
        @keyframes slideUp {
          from {
            opacity: 0;
            transform: translateY(20px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }
        .animate-slide-up {
          animation: slideUp 0.5s ease-out forwards;
        }
      `}</style>

      {/* Header */}
      <div className="relative z-10 border-b border-slate-800 bg-slate-950/80 backdrop-blur-xl">
        <div className="max-w-7xl mx-auto px-6 py-6">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-4xl font-bold tracking-tight mb-2">
                <span className="bg-gradient-to-r from-blue-400 to-cyan-400 bg-clip-text text-transparent">
                  Traffic Monitor
                </span>
              </h1>
              <p className="text-slate-400 text-sm font-['JetBrains_Mono']">
                Real-time anomaly detection • Vancouver, BC
              </p>
            </div>
            
            <div className="flex items-center space-x-6">
              <div className="text-right">
                <div className="text-sm text-slate-400">Last Updated</div>
                <div className="text-lg font-semibold font-['JetBrains_Mono']">3:00 PM</div>
              </div>
              <div className="text-right">
                <div className="text-sm text-slate-400">Active Alerts</div>
                <div className="text-lg font-semibold font-['JetBrains_Mono'] text-red-400">
                  {MOCK_ALERTS.length}
                </div>
              </div>
              <div className="w-3 h-3 bg-green-400 rounded-full animate-pulse"></div>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="relative z-10 max-w-7xl mx-auto px-6 py-8">
        {/* Stats Bar */}
        <div className="grid grid-cols-3 gap-4 mb-8">
          <StatCard
            label="Critical Alerts"
            value={MOCK_ALERTS.filter(a => a.severity === 'CRITICAL').length}
            color="red"
            delay="0ms"
          />
          <StatCard
            label="High Priority"
            value={MOCK_ALERTS.filter(a => a.severity === 'HIGH').length}
            color="orange"
            delay="100ms"
          />
          <StatCard
            label="Medium Priority"
            value={MOCK_ALERTS.filter(a => a.severity === 'MEDIUM').length}
            color="yellow"
            delay="200ms"
          />
        </div>

        {/* Alert List Header */}
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-2xl font-bold flex items-center space-x-3">
            <AlertTriangle className="w-6 h-6 text-red-400" />
            <span>Active Alerts</span>
          </h2>
          <div className="text-sm text-slate-400 font-['JetBrains_Mono']">
            Sorted by Score Delta (Highest First)
          </div>
        </div>

        {/* Alert Cards */}
        <div className="space-y-4">
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
        </div>
      </div>
    </div>
  );
}

// Stat Card Component
function StatCard({ label, value, color, delay }) {
  const colorClasses = {
    red: 'from-red-500/10 to-red-600/10 border-red-500/30',
    orange: 'from-orange-500/10 to-orange-600/10 border-orange-500/30',
    yellow: 'from-yellow-500/10 to-yellow-600/10 border-yellow-500/30'
  };

  const textColors = {
    red: 'text-red-400',
    orange: 'text-orange-400',
    yellow: 'text-yellow-400'
  };

  return (
    <div
      className={`bg-gradient-to-br ${colorClasses[color]} border backdrop-blur-sm rounded-xl p-6 animate-slide-up`}
      style={{ animationDelay: delay }}
    >
      <div className="text-slate-400 text-sm mb-2">{label}</div>
      <div className={`text-4xl font-bold font-['JetBrains_Mono'] ${textColors[color]}`}>
        {value}
      </div>
    </div>
  );
}

// Alert Card Component
function AlertCard({ alert, index, getSeverityColor, getSeverityBadgeColor, getSeverityIcon, isSelected, onClick }) {
  return (
    <div
      className={`bg-slate-900/50 backdrop-blur-sm border border-slate-800 rounded-xl overflow-hidden transition-all duration-300 cursor-pointer hover:border-slate-700 hover:shadow-2xl animate-slide-up ${
        isSelected ? 'ring-2 ring-cyan-500' : ''
      }`}
      style={{ animationDelay: `${index * 100}ms` }}
      onClick={onClick}
    >
      {/* Severity Indicator Bar */}
      <div className={`h-1.5 bg-gradient-to-r ${getSeverityColor(alert.severity)}`}></div>

      <div className="p-6">
        <div className="flex items-start justify-between mb-4">
          <div className="flex-1">
            <div className="flex items-center space-x-3 mb-2">
              <span className="text-2xl">{getSeverityIcon(alert.severity)}</span>
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
              <span className="text-3xl font-bold font-['JetBrains_Mono'] text-red-400">
                +{alert.delta}
              </span>
            </div>
            <div className="text-xs text-slate-500">Score Delta</div>
          </div>
        </div>

        {/* Score Breakdown */}
        <div className="grid grid-cols-3 gap-4 mb-4">
          <div className="bg-slate-800/50 rounded-lg p-3">
            <div className="text-xs text-slate-500 mb-1">Current Score</div>
            <div className="text-xl font-bold font-['JetBrains_Mono'] text-white">
              {alert.currentScore}
            </div>
          </div>
          <div className="bg-slate-800/50 rounded-lg p-3">
            <div className="text-xs text-slate-500 mb-1">Baseline (30min avg)</div>
            <div className="text-xl font-bold font-['JetBrains_Mono'] text-slate-400">
              {alert.baseline}
            </div>
          </div>
          <div className="bg-slate-800/50 rounded-lg p-3">
            <div className="text-xs text-slate-500 mb-1 flex items-center space-x-1">
              <Clock className="w-3 h-3" />
              <span>Time</span>
            </div>
            <div className="text-xl font-bold font-['JetBrains_Mono'] text-cyan-400">
              {alert.timestamp}
            </div>
          </div>
        </div>

        {/* Detected Objects */}
        <div className="flex flex-wrap gap-2">
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
        </div>
      </div>
    </div>
  );
}

// Object Badge Component
function ObjectBadge({ icon, label, count, critical = false }) {
  return (
    <div
      className={`flex items-center space-x-2 px-3 py-1.5 rounded-full text-xs font-semibold border ${
        critical
          ? 'bg-red-500/10 text-red-400 border-red-500/30'
          : 'bg-slate-800/50 text-slate-300 border-slate-700'
      }`}
    >
      {icon}
      <span>{label}</span>
      <span className={`px-1.5 py-0.5 rounded-full font-['JetBrains_Mono'] ${
        critical ? 'bg-red-500 text-white' : 'bg-slate-700 text-white'
      }`}>
        {count}
      </span>
    </div>
  );
}