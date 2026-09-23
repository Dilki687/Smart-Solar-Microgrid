import {
  APIProvider,
  Map,
  AdvancedMarker,
  InfoWindow,
} from "@vis.gl/react-google-maps";
import { useState } from "react";
import type { Station } from "../types/station";

interface StationMapProps {
  stations: Station[];
}

const StationMap = ({ stations }: StationMapProps) => {
  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;

  const [selectedStation, setSelectedStation] = useState<Station | null>(
    null,
  );

  const activeStations = stations.filter(
    (station) => station.status === "ACTIVE",
  );

  const center =
    activeStations.length > 0
      ? {
          lat: activeStations[0].latitude,
          lng: activeStations[0].longitude,
        }
      : {
          lat: 6.9271,
          lng: 79.8612,
        };

  if (!apiKey) {
    return (
      <div className="map-placeholder">
        <div className="map-placeholder-icon">📍</div>

        <h3>Google Maps API Key Required</h3>

        <p>
          Add VITE_GOOGLE_MAPS_API_KEY to the frontend .env file
          to display the station map.
        </p>
      </div>
    );
  }

  return (
    <div className="station-map">
      <APIProvider apiKey={apiKey}>
        <Map
          defaultCenter={center}
          defaultZoom={12}
          mapId="SMART_SOLAR_MICROGRID_MAP"
          gestureHandling="greedy"
          disableDefaultUI={false}
        >
          {activeStations.map((station) => (
            <AdvancedMarker
              key={station.stationId}
              position={{
                lat: station.latitude,
                lng: station.longitude,
              }}
              title={station.name}
              onClick={() => setSelectedStation(station)}
            />
          ))}

          {selectedStation && (
            <InfoWindow
              position={{
                lat: selectedStation.latitude,
                lng: selectedStation.longitude,
              }}
              onCloseClick={() => setSelectedStation(null)}
            >
              <div className="station-map-info">
                <h3>{selectedStation.name}</h3>

                <p>
                  <strong>Station ID:</strong>{" "}
                  {selectedStation.stationId}
                </p>

                <p>
                  <strong>Status:</strong>{" "}
                  {selectedStation.status}
                </p>

                <p>
                  <strong>Capacity:</strong>{" "}
                  {selectedStation.capacityKw} kW
                </p>

                <p>
                  <strong>Location:</strong>{" "}
                  {selectedStation.latitude.toFixed(4)},{" "}
                  {selectedStation.longitude.toFixed(4)}
                </p>
              </div>
            </InfoWindow>
          )}
        </Map>
      </APIProvider>
    </div>
  );
};

export default StationMap;