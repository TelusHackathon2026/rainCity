
import React from 'react';
import { MapContainer, TileLayer, Marker, Popup, Circle } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';

import icon from 'leaflet/dist/images/marker-icon.png';
import iconShadow from 'leaflet/dist/images/marker-shadow.png';
let DefaultIcon = L.icon({ iconUrl: icon, shadowUrl: iconShadow, iconSize: [25, 41], iconAnchor: [12, 41] });
L.Marker.prototype.options.icon = DefaultIcon;

export default function Map({ alerts }) {
  const VANCOUVER_CENTER = [49.2827, -123.1207];

  const exampleAlerts = [
    { id: 1, location: "Denman St and W Georgia St", coords: { lat: 49.292594, lng: -123.133742 }, severity: "CRITICAL", currentScore: 9.2 },
    { id: 2, location: "Cambie St and W 16th Av", coords: { lat: 49.256908, lng: -123.115085 }, severity: "WARNING", currentScore: 6.5 },
    { id: 3, location: "Cassiar Connector and E Hastings St", coords: { lat: 49.281126, lng: -123.030836 }, severity: "CRITICAL", currentScore: 8.7 },
    { id: 4, location: "Main St and Terminal Av", coords: { lat: 49.272776, lng: -123.100028 }, severity: "WARNING", currentScore: 5.9 },
    { id: 5, location: "Beatty St and Dunsmuir St", coords: { lat: 49.284680, lng: -123.110954 }, severity: "CRITICAL", currentScore: 9.0 },
    { id: 6, location: "Commercial Drive and E Broadway", coords: { lat: 49.262319, lng: -123.069763 }, severity: "WARNING", currentScore: 6.2 },
    { id: 7, location: "Boundary Road and Grandview Hwy", coords: { lat: 49.250540, lng: -123.024711 }, severity: "WARNING", currentScore: 6.0 },
  ];

  const mapAlerts = alerts?.length ? alerts : exampleAlerts;

  return (
    <div className="h-[75vh] w-full rounded-xl overflow-hidden border border-slate-800">
      <MapContainer center={VANCOUVER_CENTER} zoom={13} className="h-full w-full">
        <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />

        {mapAlerts.filter(a => a.coords).map((alert) => (
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
                </div>
              </Popup>
            </Marker>
          </React.Fragment>
        ))}
      </MapContainer>
    </div>
  );
}

