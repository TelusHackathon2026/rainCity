import React from 'react';
import { motion } from 'framer-motion';
import { MapContainer, TileLayer, Marker, Popup, Circle } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import Header from './Header';
import L from 'leaflet';

// Fix Leaflet default marker icon issue
import icon from 'leaflet/dist/images/marker-icon.png';
import iconShadow from 'leaflet/dist/images/marker-shadow.png';

let DefaultIcon = L.icon({
  iconUrl: icon,
  shadowUrl: iconShadow,
  iconSize: [25, 41],
  iconAnchor: [12, 41]
});

L.Marker.prototype.options.icon = DefaultIcon;

// Vancouver coordinates
const VANCOUVER_CENTER = [49.2827, -123.1207];

// Mock alert locations (replace with your actual data)
const ALERT_LOCATIONS = [
  { 
    id: 1, 
    position: [49.2827, -123.1207], 
    location: 'Main St & Broadway',
    severity: 'CRITICAL',
    delta: 92
  },
  { 
    id: 2, 
    position: [49.2488, -123.1165], 
    location: 'Granville St & 41st Ave',
    severity: 'HIGH',
    delta: 31
  },
  { 
    id: 3, 
    position: [49.2634, -123.1139], 
    location: 'Cambie St & 12th Ave',
    severity: 'MEDIUM',
    delta: 14
  }
];

// Severity colors
const getSeverityColor = (severity) => {
  switch (severity) {
    case 'CRITICAL': return '#dc2626';  // red-600
    case 'HIGH': return '#ea580c';       // orange-600
    case 'MEDIUM': return '#ca8a04';     // yellow-600
    default: return '#64748b';           // slate-500
  }
};

export default function Map() {
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
        title="Traffic Monitor"
        showMapButton={false}
        showStats={true}
      />

      {/* Map Content */}
      <div className="relative z-10 max-w-7xl mx-auto px-6 py-8">
        <motion.div
          className="bg-slate-900/50 backdrop-blur-sm border border-slate-800 rounded-xl overflow-hidden"
          style={{ height: '75vh' }}
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.5 }}
        >
          {/* Leaflet Map */}
          <MapContainer 
            center={VANCOUVER_CENTER} 
            zoom={12} 
            style={{ height: '100%', width: '100%' }}
            className="z-0"
          >
            {/* Map Tiles - Using OpenStreetMap (free, no API key) */}
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />

            {/* Alert Markers */}
            {ALERT_LOCATIONS.map((alert) => (
              <React.Fragment key={alert.id}>
                {/* Circle to show alert area */}
                <Circle
                  center={alert.position}
                  radius={500}
                  pathOptions={{
                    color: getSeverityColor(alert.severity),
                    fillColor: getSeverityColor(alert.severity),
                    fillOpacity: 0.3,
                    weight: 2
                  }}
                />
                
                {/* Marker with popup */}
                <Marker position={alert.position}>
                  <Popup>
                    <div className="text-slate-900">
                      <h3 className="font-bold text-lg mb-1">{alert.location}</h3>
                      <p className="text-sm mb-1">
                        <span className="font-semibold">Severity:</span> {alert.severity}
                      </p>
                      <p className="text-sm">
                        <span className="font-semibold">Delta:</span> +{alert.delta}
                      </p>
                    </div>
                  </Popup>
                </Marker>
              </React.Fragment>
            ))}
          </MapContainer>
        </motion.div>

        {/* Map Legend */}
        <motion.div
          className="mt-4 bg-slate-900/50 backdrop-blur-sm border border-slate-800 rounded-xl p-4"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.3 }}
        >
          <h3 className="text-white font-bold mb-3">Alert Severity</h3>
          <div className="flex items-center space-x-6">
            <div className="flex items-center space-x-2">
              <div className="w-4 h-4 rounded-full bg-red-600"></div>
              <span className="text-slate-300 text-sm">Critical</span>
            </div>
            <div className="flex items-center space-x-2">
              <div className="w-4 h-4 rounded-full bg-orange-600"></div>
              <span className="text-slate-300 text-sm">High</span>
            </div>
            <div className="flex items-center space-x-2">
              <div className="w-4 h-4 rounded-full bg-yellow-600"></div>
              <span className="text-slate-300 text-sm">Medium</span>
            </div>
          </div>
        </motion.div>
      </div>
    </div>
  );
}