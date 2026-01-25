import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useEffect, useRef, useState } from "react";
import { motion, AnimatePresence } from 'framer-motion';
import { AlertTriangle, TrendingUp, Clock, MapPin, UserX, Construction, Flame, TreeDeciduous, Users } from 'lucide-react';

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
    currentScore: backendAlert.score,
    baseline: backendAlert.average,
    delta: backendAlert.delta,
    severity: calculateSeverity(backendAlert),
    detectedObjects: backendAlert.detectedObjects || {},
    timestamp: backendAlert.timestamp,
    img: backendAlert.img,
    description: backendAlert.description
  });

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws/hazards'),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => console.log('STOMP:', str),

      onConnect: () => {
        console.log('✅ Connected to WebSocket');
        setConnected(true);
        setError(null);

        // 1. Subscribe to topic
        client.subscribe('/topic/traffic-alerts', (message) => {
          try {
            const data = JSON.parse(message.body);

            // 2. Use functional update to avoid stale closures
            setAlerts(currentAlerts => {
              if (Array.isArray(data)) {
                return data.map(transformAlert);
              } else {
                const transformed = transformAlert(data);
                const existingIndex = currentAlerts.findIndex(a => a.id === transformed.id);

                if (existingIndex >= 0) {
                  const updated = [...currentAlerts];
                  updated[existingIndex] = transformed;
                  return updated;
                } else {
                  return [transformed, ...currentAlerts];
                }
              }
            });

            setLastUpdated(new Date().toLocaleTimeString());
          } catch (err) {
            console.error('❌ Error parsing message:', err);
          }
        });

        // 3. Initial publish
        client.publish({
          destination: '/app/monitor-intersections',
          body: JSON.stringify({ intersections: MONITORED_INTERSECTIONS })
        });
      },

      onStompError: (frame) => {
        console.error('❌ STOMP error:', frame);
        setError('WebSocket Protocol Error');
      },

      onWebSocketClose: () => {
        setConnected(false);
      }
    });

    client.activate();
    stompClient.current = client;

    return () => {
      if (stompClient.current) stompClient.current.deactivate();
    };
  }, []);

  const activeAlerts = alerts
    .filter(a => a.severity !== 'NORMAL')
    .sort((a, b) => b.delta - a.delta);

  // Helper UI functions
  const getSeverityColor = (s) => ({
    CRITICAL: 'bg-red-600', HIGH: 'bg-orange-600', MEDIUM: 'bg-yellow-600'
  }[s] || 'bg-slate-600');

  const getSeverityBadge = (s) => ({
    CRITICAL: 'bg-red-500/20 text-red-400 border-red-500/50',
    HIGH: 'bg-orange-500/20 text-orange-400 border-orange-500/50',
    MEDIUM: 'bg-yellow-500/20 text-yellow-400 border-yellow-500/50'
  }[s] || 'bg-slate-500/20 text-slate-400 border-slate-500/50');

  const getSeverityIcon = (s) => ({ CRITICAL: '🔴', HIGH: '🟠', MEDIUM: '🟡' }[s] || '⚪');

  if (!connected && alerts.length === 0) {
    return (
      <div className="min-h-screen bg-slate-950 text-white flex items-center justify-center font-sans">
        <div className="text-center">
          <div className="w-12 h-12 border-4 border-cyan-400 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
          <p className="text-slate-400">Connecting to Live Traffic Feed...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-950 text-white font-sans p-6">
      {/* Status Header */}
      <div className="max-w-7xl mx-auto flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Traffic Control Center</h1>
          <p className="text-slate-400">Last update: {lastUpdated || 'Never'}</p>
        </div>
        <div className={`px-4 py-2 rounded-full border flex items-center gap-2 ${connected ? 'border-green-500/30 bg-green-500/10' : 'border-red-500/30 bg-red-500/10'}`}>
          <div className={`w-2 h-2 rounded-full ${connected ? 'bg-green-400 animate-pulse' : 'bg-red-400'}`} />
          <span className={connected ? 'text-green-400' : 'text-red-400'}>{connected ? 'Live' : 'Disconnected'}</span>
        </div>
      </div>

      <div className="max-w-7xl mx-auto grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <StatCard label="Critical" value={activeAlerts.filter(a => a.severity === 'CRITICAL').length} color="text-red-400" />
        <StatCard label="High Priority" value={activeAlerts.filter(a => a.severity === 'HIGH').length} color="text-orange-400" />
        <StatCard label="Monitoring" value={MONITORED_INTERSECTIONS.length} color="text-cyan-400" />
      </div>

      <div className="max-w-7xl mx-auto space-y-4">
        <AnimatePresence mode='popLayout'>
          {activeAlerts.length === 0 ? (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="text-center py-20 border-2 border-dashed border-slate-800 rounded-2xl">
              <p className="text-slate-500">No active hazards detected in monitored zones.</p>
            </motion.div>
          ) : (
            activeAlerts.map((alert) => (
              <AlertCard
                key={alert.id}
                alert={alert}
                getSeverityColor={getSeverityColor}
                getSeverityBadge={getSeverityBadge}
                getSeverityIcon={getSeverityIcon}
              />
            ))
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}

function StatCard({ label, value, color }) {
  return (
    <div className="bg-slate-900 border border-slate-800 p-6 rounded-2xl">
      <p className="text-slate-500 text-sm font-medium uppercase tracking-wider">{label}</p>
      <p className={`text-4xl font-bold mt-1 ${color}`}>{value}</p>
    </div>
  );
}

function AlertCard({ alert, getSeverityColor, getSeverityBadge, getSeverityIcon }) {
  return (
    <motion.div
      layout
      initial={{ opacity: 0, scale: 0.98 }}
      animate={{ opacity: 1, scale: 1 }}
      exit={{ opacity: 0, scale: 0.98 }}
      transition={{ type: "spring", stiffness: 300, damping: 30 }}
      className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden hover:border-slate-700 transition-colors shadow-xl"
    >
      <div className={`h-1 ${getSeverityColor(alert.severity)}`} />
      <div className="p-6 flex flex-col md:flex-row gap-6">
        <div className="flex-1">
          <div className="flex items-center gap-3 mb-2">
            <span className="text-xl">{getSeverityIcon(alert.severity)}</span>
            <h3 className="text-xl font-bold">{alert.location}</h3>
            <span className={`px-2 py-0.5 rounded text-xs font-bold border ${getSeverityBadge(alert.severity)}`}>
              {alert.severity}
            </span>
          </div>
          <div className="flex items-center gap-2 text-slate-400 mb-4 text-sm">
            <MapPin size={14} />
            {alert.description}
          </div>

          <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            <div className="bg-slate-950 p-3 rounded-xl border border-slate-800">
              <p className="text-[10px] text-slate-500 uppercase">Current Score</p>
              <p className="text-lg font-mono font-bold">{alert.currentScore}</p>
            </div>
            <div className="bg-slate-950 p-3 rounded-xl border border-slate-800">
              <p className="text-[10px] text-slate-500 uppercase">Baseline</p>
              <p className="text-lg font-mono text-slate-400">{alert.baseline}</p>
            </div>
            <div className="bg-slate-950 p-3 rounded-xl border border-slate-800">
              <p className="text-[10px] text-slate-500 uppercase">Timestamp</p>
              <p className="text-lg font-mono text-cyan-400">{alert.timestamp}</p>
            </div>
          </div>
        </div>

        <div className="md:w-32 flex flex-col justify-center items-end">
          <div className="text-right">
            <div className="flex items-center gap-2 text-red-400 font-mono text-3xl font-black">
              <TrendingUp size={24} />
              +{alert.delta}
            </div>
            <p className="text-[10px] text-slate-500 uppercase font-bold">Spike Intensity</p>
          </div>
        </div>
      </div>
    </motion.div>
  );
}
