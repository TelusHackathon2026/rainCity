import React from 'react';
import { MapContainer, TileLayer, Marker, Popup, Circle } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';

// Fix for default marker icons
import icon from 'leaflet/dist/images/marker-icon.png';
import iconShadow from 'leaflet/dist/images/marker-shadow.png';
let DefaultIcon = L.icon({ iconUrl: icon, shadowUrl: iconShadow, iconSize: [25, 41], iconAnchor: [12, 41] });
L.Marker.prototype.options.icon = DefaultIcon;

export default function Map({ alerts = [] }) {
  const VANCOUVER_CENTER = [49.2827, -123.1207];

  return (
    <div className="h-[75vh] w-full rounded-xl overflow-hidden border border-slate-800">
      <MapContainer center={VANCOUVER_CENTER} zoom={13} className="h-full w-full">
        <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />

        {alerts.filter(a => a.coords).map((alert) => (
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
