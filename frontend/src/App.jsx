import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useEffect, useRef, useState } from "react";
import { motion } from 'framer-motion';
import { AlertTriangle, TrendingUp, Clock, MapPin, UserX, Construction, Flame, TreeDeciduous, Users } from 'lucide-react';
import Header from './Header';

// List of intersections to monitor
const MONITORED_INTERSECTIONS = [
  'Hornby St and Nelson St',
  'Granville St and W 12th Ave',
  'Fir St and W 5th Ave',
  'Main St and E Hastings St',
  'Burrard St and Drake St',
  'Hornby St and Smithe St',
  'Davie St and Burrard St',
  'Robson St and Granville St',
  'Broadway and Main St',
  'Commercial Dr and Broadway'
];

export default function TrafficDashboard() {
  const [selectedAlert, setSelectedAlert] = useState(null);
  const [alerts, setAlerts] = useState([]);
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);
  const stompClient = useRef(null);

  // Calculate severity from spike and delta
  const calculateSeverity = (alert) => {
    if (!alert.spike) return 'NORMAL';

    if (alert.delta > 50) return 'CRITICAL';
    if (alert.delta > 25) return 'HIGH';
    if (alert.delta > 10) return 'MEDIUM';

    return 'NORMAL';
  };

  // Transform backend data to frontend format
  const transformAlert = (backendAlert) => ({
    id: backendAlert.id,
    location: backendAlert.location,
    currentScore: backendAlert.score,              // score → currentScore
    baseline: backendAlert.average,                 // average → baseline
    delta: backendAlert.delta,
    severity: calculateSeverity(backendAlert),      // calculate from spike
    detectedObjects: backendAlert.detectedObjects,
    timestamp: backendAlert.timestamp,
    img: backendAlert.img,
    description: backendAlert.description
  });

  useEffect(() => {
    // Create STOMP client
    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws/hazards'), // ← CHANGE THIS to your backend URL

      connectHeaders: {
        // Add any auth headers here if needed
      },

      debug: (str) => {
        console.log('STOMP:', str);
      },

      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      onConnect: (frame) => {
        console.log('✅ Connected to WebSocket');
        setConnected(true);
        setError(null);

        // Subscribe to traffic alerts topic
        client.subscribe('/topic/traffic-alerts', (message) => {
          console.log('📨 Received alert:', message.body);

          try {
            const data = JSON.parse(message.body);

            // Handle single alert or array of alerts
            if (Array.isArray(data)) {
              // Multiple alerts received (initial load or bulk update)
              const transformedAlerts = data.map(transformAlert);
              setAlerts(transformedAlerts);
              console.log(`✅ ${transformedAlerts.length} alerts loaded`);
            } else {
              // Single alert received (real-time update)
              const transformedAlert = transformAlert(data);
              setAlerts(prevAlerts => {
                // Check if alert exists (update) or is new (add)
                const existingIndex = prevAlerts.findIndex(a => a.id === transformedAlert.id);
                if (existingIndex >= 0) {
                  // Update existing alert
                  const newAlerts = [...prevAlerts];
                  newAlerts[existingIndex] = transformedAlert;
                  return newAlerts;
                } else {
                  // Add new alert
                  return [...prevAlerts, transformedAlert];
                }
              });
              console.log(`✅ Alert for ${data.location}`);
            }

            setLastUpdated(new Date().toLocaleTimeString());
          } catch (err) {
            console.error('❌ Error parsing message:', err);
          }
        });

        // Send list of intersections to monitor
        console.log('📤 Requesting monitoring for intersections:', MONITORED_INTERSECTIONS);
        client.publish({
          destination: '/app/monitor-intersections',
          body: JSON.stringify({
            intersections: MONITORED_INTERSECTIONS
          })
        });
      },

      onStompError: (frame) => {
        console.error('❌ STOMP error:', frame.headers['message']);
        setError('WebSocket connection error');
        setConnected(false);
      },

      onWebSocketClose: () => {
        console.log('🔌 WebSocket closed');
        setConnected(false);
      },

      onWebSocketError: (event) => {
        console.error('❌ WebSocket error:', event);
        setError('Failed to connect to server');
      }
    });

    client.activate();
    stompClient.current = client;

    // Cleanup on unmount
    return () => {
      console.log('🛑 Disconnecting WebSocket');
      if (client.active) {
        client.deactivate();
      }
    };
  }, []);

  // Show loading while connecting
  if (!connected && !error && alerts.length === 0) {
    return (
      <div className="min-h-screen bg-slate-950 font-['Outfit'] text-white flex items-center justify-center">
        <div className="text-center">
          <div className="w-16 h-16 border-4 border-cyan-400 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
          <p className="text-slate-400 text-lg">Connecting to server...</p>
          <p className="text-slate-500 text-sm mt-2">Establishing WebSocket connection</p>
        </div>
      </div>
    );
  }

  // Show error state
  if (error && !connected) {
    return (
      <div className="min-h-screen bg-slate-950 font-['Outfit'] text-white flex items-center justify-center">
        <div className="text-center">
          <div className="text-red-400 text-6xl mb-4">⚠️</div>
          <h2 className="text-2xl font-bold text-white mb-2">Connection Failed</h2>
          <p className="text-slate-400 mb-4">{error}</p>
          <button
            onClick={() => window.location.reload()}
            className="px-6 py-2 bg-cyan-500 hover:bg-cyan-600 text-white rounded-lg transition-colors"
          >
            Retry Connection
          </button>
        </div>
      </div>
    );
  }

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
    if (severity === 'CRITICAL') return '🔴';
    if (severity === 'HIGH') return '🟠';
    if (severity === 'MEDIUM') return '🟡';
    return '⚪';
  };

  // Filter and sort alerts
  const activeAlerts = alerts
    .filter(a => a.severity !== 'NORMAL')
    .sort((a, b) => b.delta - a.delta);

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

      {/* Connection Status */}
      {connected && (
        <div className="fixed top-4 right-4 z-50">
          <div className="flex items-center space-x-2 bg-green-500/10 border border-green-500/30 rounded-full px-4 py-2">
            <div className="w-2 h-2 bg-green-400 rounded-full animate-pulse"></div>
            <span className="text-green-400 text-sm font-semibold">Live</span>
          </div>
        </div>
      )}

      {/* Header */}
      <Header
        lastUpdated={lastUpdated || "Waiting for data..."}
        activeAlerts={activeAlerts.length}
        showMapButton={true}
        showStats={true}
      />

      {/* Main Content */}
      <div className="relative z-10 max-w-7xl mx-auto px-6 py-8">
        {/* Stats Bar */}
        <div className="grid grid-cols-3 gap-4 mb-8">
          <StatCard
            label="Critical Alerts"
            value={activeAlerts.filter(a => a.severity === 'CRITICAL').length}
            color="red"
            delay={0}
          />
          <StatCard
            label="High Priority"
            value={activeAlerts.filter(a => a.severity === 'HIGH').length}
            color="orange"
            delay={0.1}
          />
          <StatCard
            label="Medium Priority"
            value={activeAlerts.filter(a => a.severity === 'MEDIUM').length}
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
          {activeAlerts.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-slate-400 text-lg">No active alerts at this time</p>
              <p className="text-slate-500 text-sm mt-2">
                {connected ? 'All traffic conditions are normal' : 'Waiting for connection...'}
              </p>
            </div>
          ) : (
            activeAlerts.map((alert, index) => (
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
            ))
          )}
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
      className={`bg-slate-900/50 backdrop-blur-sm border border-slate-800 rounded-xl overflow-hidden transition-all duration-300 cursor-pointer hover:border-slate-700 hover:shadow-2xl ${isSelected ? 'ring-2 ring-cyan-500' : ''
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

        {/* Camera Image */}
        {alert.img && (
          <motion.div
            className="mb-4 rounded-lg overflow-hidden border border-slate-700"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.5, delay: 0.2 }}
          >
            <img
              src={alert.img}
              alt={`Traffic camera at ${alert.location}`}
              className="w-full h-48 object-cover"
              onError={(e) => e.target.style.display = 'none'}
            />
          </motion.div>
        )}

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
          {alert.detectedObjects.person_laying > 0 && (
            <ObjectBadge
              icon={<UserX className="w-3 h-3" />}
              label="Person Laying"
              count={alert.detectedObjects.person_laying}
              critical
            />
          )}
          {alert.detectedObjects.accident && (
            <ObjectBadge
              icon={<Flame className="w-3 h-3" />}
              label="Accident"
              isBool
              critical
            />
          )}
          {alert.detectedObjects.debris > 0 && (
            <ObjectBadge
              icon={<Flame className="w-3 h-3" />}
              label="Debris"
              count={alert.detectedObjects.debris}
              critical
            />
          )}
          {alert.detectedObjects.cones > 0 && (
            <ObjectBadge
              icon={<Construction className="w-3 h-3" />}
              label="Traffic Cones"
              count={alert.detectedObjects.cones}
            />
          )}
          {alert.detectedObjects.people > 0 && (
            <ObjectBadge
              icon={<Users className="w-3 h-3" />}
              label="People"
              count={alert.detectedObjects.people}
            />
          )}
          {alert.detectedObjects.tree && (
            <ObjectBadge
              icon={<TreeDeciduous className="w-3 h-3" />}
              label="Fallen Tree"
              isBool
              critical
            />
          )}
        </motion.div>
      </div>
    </motion.div>
  );
}

// Object Badge Component
function ObjectBadge({ icon, label, count, isBool = false, critical = false }) {
  return (
    <motion.div
      className={`flex items-center space-x-2 px-3 py-1.5 rounded-full text-xs font-semibold border ${critical
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
      {!isBool && count !== undefined && (
        <span className={`px-1.5 py-0.5 rounded-full font-['JetBrains_Mono'] ${critical ? 'bg-red-500 text-white' : 'bg-slate-700 text-white'
          }`}>
          {count}
        </span>
      )}
    </motion.div>
  );
}
