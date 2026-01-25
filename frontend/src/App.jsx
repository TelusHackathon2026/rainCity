import React, { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { motion, AnimatePresence } from 'framer-motion';
import { Activity, Wifi, WifiOff, UserX, Flame, TreeDeciduous, Users } from 'lucide-react';
import Header from './Header';

const MONITORED_INTERSECTIONS = [
  'Main St and E Hastings St',
  'McLean Drive and Powell St"',
  'Seymour St and Nelson St',
  'Knight St and SE Marine Drive'
];

export default function TrafficDashboard() {
  const [alerts, setAlerts] = useState([]);
  const [connected, setConnected] = useState(false);
  const [lastUpdated, setLastUpdated] = useState(null);
  const stompClient = useRef(null);

  // --- DEBUGGING TRANSFORMER ---
  const transformAlert = (backend) => {
    console.log("🔍 DEBUG: Processing backend data:", backend);

    const tags = backend.detectedObjects || {};
    const loc = backend.location || "Unknown Location";

    // Logic Check: Is it a spike?
    const severity = backend.spike ? (backend.delta > 50 ? 'CRITICAL' : 'HIGH') : 'NORMAL';
    console.log(`📊 DEBUG: Calculated Severity for ${loc}: ${severity}`);

    return {
      id: loc,
      location: loc,
      currentScore: Math.round(backend.score || 0),
      baseline: Math.round(backend.average || 0),
      delta: Math.round(backend.delta || 0),
      severity: severity,
      detectedObjects: {
        person_laying: tags.person_laying || false,
        accident: tags.accident || false,
        tree: tags.tree || false,
        debris: tags.debris || 0,
        people: tags.people || 0
      },
      timestamp: new Date().toLocaleTimeString(),
      img: backend.img,
      description: backend.description || "System analyzing live feed..."
    };
  };

  useEffect(() => {
    console.log("🚀 Initializing WebSocket...");

    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws/hazards'),
      reconnectDelay: 5000,
      onConnect: () => {
        console.log("✅ WEBSOCKET CONNECTED");
        setConnected(true);

        client.subscribe('/topic/traffic-alerts', (msg) => {
          const rawData = JSON.parse(msg.body);
          console.log("📥 MESSAGE RECEIVED FROM JAVA:", rawData);

          const transformed = transformAlert(rawData);

          setAlerts(currentAlerts => {
            const existingIndex = currentAlerts.findIndex(a => a.location === transformed.location);

            let newState;
            if (existingIndex > -1) {
              console.log(`🔄 DEBUG: Updating existing card for ${transformed.location}`);
              newState = [...currentAlerts];
              newState[existingIndex] = transformed;
            } else {
              console.log(`🆕 DEBUG: Adding new card for ${transformed.location}`);
              newState = [transformed, ...currentAlerts];
            }

            console.log("📋 DEBUG: Current State Count:", newState.length);
            return newState;
          });

          setLastUpdated(new Date().toLocaleTimeString());
        });

        client.publish({
          destination: '/app/monitor-intersections',
          body: JSON.stringify({ intersections: MONITORED_INTERSECTIONS })
        });
      },
      onDisconnect: () => {
        console.warn("🔌 WEBSOCKET DISCONNECTED");
        setConnected(false);
      }
    });

    client.activate();
    stompClient.current = client;
    return () => client.deactivate();
  }, []);

  // --- CRITICAL DEBUG CHANGE ---
  // We are commenting out the filter. If data exists in 'alerts', it WILL show on screen.
  const activeAlerts = alerts;
  console.log("🎨 DEBUG: Rendering component with alert count:", activeAlerts.length);

  return (
    <div className="min-h-screen bg-slate-950 text-white font-['Outfit']">
      <div className="fixed top-4 right-4 z-50 flex items-center gap-2 bg-black/50 p-2 rounded-lg border border-slate-800 text-[10px]">
        {connected ? <Wifi size={12} className="text-green-500" /> : <WifiOff size={12} className="text-red-500" />}
        <span>{connected ? "WS ACTIVE" : "WS DEAD"}</span>
      </div>

      <Header lastUpdated={lastUpdated} activeAlerts={activeAlerts.filter(a => a.severity !== 'NORMAL').length} />

      <main className="max-w-7xl mx-auto px-6 py-8">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
          <StatCard label="Total Monitored" value={alerts.length} color="blue" />
          <StatCard label="Spikes Detected" value={alerts.filter(a => a.severity !== 'NORMAL').length} color="red" />
          <StatCard label="Last Signal" value={lastUpdated || "N/A"} color="orange" />
        </div>

        <div className="space-y-4">
          {activeAlerts.length === 0 && (
            <div className="text-center py-20 border border-dashed border-slate-800 rounded-2xl">
              <Activity className="mx-auto text-slate-800 mb-4 animate-pulse" size={48} />
              <p className="text-slate-600">Waiting for data from Java Backend...</p>
            </div>
          )}

          <AnimatePresence mode="popLayout">
            {activeAlerts.map((alert) => (
              <AlertCard key={alert.id} alert={alert} />
            ))}
          </AnimatePresence>
        </div>
      </main>
    </div>
  );
}

// --- SUB-COMPONENTS (Keep these exactly as is) ---

function AlertCard({ alert }) {
  const isNormal = alert.severity === 'NORMAL';
  const sColor = isNormal ? 'bg-slate-700' : (alert.severity === 'CRITICAL' ? 'bg-red-500' : 'bg-orange-500');

  return (
    <motion.div
      layout
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      className={`bg-slate-900/50 border ${isNormal ? 'border-slate-800' : 'border-red-900/50'} rounded-xl overflow-hidden`}
    >
      <div className={`h-1 ${sColor}`} />
      <div className="p-6 grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="md:col-span-2">
          <div className="flex items-center gap-3 mb-2">
            <h3 className="text-xl font-bold">{alert.location}</h3>
            <span className={`text-[10px] px-2 py-0.5 rounded ${isNormal ? 'bg-slate-800' : 'bg-red-500/20 text-red-400'}`}>
              {alert.severity}
            </span>
          </div>
          <p className="text-slate-400 text-sm mb-4">{alert.description}</p>
          <div className="flex flex-wrap gap-2">
            {alert.detectedObjects.person_laying && <Badge label="Person Down" crit />}
            {alert.detectedObjects.accident && <Badge label="Accident" crit />}
            {alert.detectedObjects.tree && <Badge label="Fallen Tree" crit />}
            <Badge label={`Score: ${alert.currentScore}`} />
          </div>
        </div>
        <div className="flex flex-col justify-center items-end">
          <div className={`text-3xl font-bold ${alert.delta > 0 ? 'text-red-400' : 'text-slate-500'}`}>
            {alert.delta > 0 ? `+${alert.delta}` : alert.delta}
          </div>
          <div className="text-slate-500 text-[10px] tracking-tighter">SCORE DELTA</div>
        </div>
        <div className="h-32 bg-black rounded-lg overflow-hidden border border-slate-800">
          {alert.img ? <img src={alert.img} className="w-full h-full object-cover" /> : <div className="flex h-full items-center justify-center text-slate-800 text-xs">NO IMAGE</div>}
        </div>
      </div>
    </motion.div>
  );
}

const Badge = ({ label, crit }) => (
  <span className={`px-2 py-1 rounded text-[10px] font-bold ${crit ? 'bg-red-500/20 text-red-400 border border-red-500/30' : 'bg-slate-800 text-slate-400'}`}>
    {label}
  </span>
);

function StatCard({ label, value, color }) {
  const colors = { red: 'text-red-400', orange: 'text-orange-400', blue: 'text-cyan-400' };
  return (
    <div className="bg-slate-900/40 border border-slate-800 p-6 rounded-xl text-center">
      <div className="text-slate-500 text-[10px] uppercase mb-1">{label}</div>
      <div className={`text-2xl font-bold ${colors[color]}`}>{value}</div>
    </div>
  );
}
