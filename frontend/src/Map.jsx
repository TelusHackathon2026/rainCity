
import React from 'react';
import { MapContainer, TileLayer, Marker, Popup, Circle } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';

// Fix for default marker icons
import icon from 'leaflet/dist/images/marker-icon.png';
import iconShadow from 'leaflet/dist/images/marker-shadow.png';
let DefaultIcon = L.icon({ iconUrl: icon, shadowUrl: iconShadow, iconSize: [25, 41], iconAnchor: [12, 41] });
L.Marker.prototype.options.icon = DefaultIcon;

export default function Map({ alerts }) {
  const VANCOUVER_CENTER = [49.2827, -123.1207];

  // Use prop alerts if provided, otherwise fallback to test data
  const testAlerts = [
    {
      id: 1,
      location: 'Main St & 1st Ave',
      coords: { lat: 49.2820, lng: -123.1150 },
      severity: 'CRITICAL',
      currentScore: 95,
      description: 'Major traffic incident reported here.'
    },
    {
      id: 2,
      location: 'Granville St & Broadway',
      coords: { lat: 49.2695, lng: -123.1380 },
      severity: 'WARNING',
      currentScore: 60,
      description: 'Roadwork ongoing, expect minor delays.'
    },
    {
      id: 3,
      location: 'Cambie St & 16th Ave',
      coords: { lat: 49.2480, lng: -123.1155 },
      severity: 'CRITICAL',
      currentScore: 88,
      description: 'Blocked drain causing flooding.'
    },
    {
      id: 4,
      location: 'West Georgia St & Howe St',
      coords: { lat: 49.2835, lng: -123.1200 },
      severity: 'WARNING',
      currentScore: 45,
      description: 'Light traffic congestion.'
    }
  ];

  const displayedAlerts = alerts && alerts.length > 0 ? alerts : testAlerts;

  return (
    <div className="h-[75vh] w-full rounded-xl overflow-hidden border border-slate-800">
      <MapContainer center={VANCOUVER_CENTER} zoom={13} className="h-full w-full">
        <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />

        {displayedAlerts.filter(a => a.coords).map((alert) => (
          <React.Fragment key={alert.id}>
            <Circle
              center={[alert.coords.lat, alert.coords.lng]}
              radius={200}
              pathOptions={{
                color: alert.severity === 'CRITICAL' ? '#ef4444' : '#f97316',
                fillOpacity: 0.4
              }}
            />
            <Marker position={[alert.coords.lat, alert.coords.lng]}>
              <Popup>
                <div className="text-slate-900 p-2">
                  <h4 className="font-bold">{alert.location}</h4>
                  <p className="text-xs">Score: {alert.currentScore}</p>
                  <p className="text-[10px] mt-1 text-slate-600">{alert.description}</p>
                </div>
              </Popup>
            </Marker>
          </React.Fragment>
        ))}
      </MapContainer>
    </div>
  );
}

