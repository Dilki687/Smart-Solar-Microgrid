import api from "./api";
import type {
  CreateStationRequest,
  NearbyStation,
  NearbyStationsResponse,
  Station,
  StationsResponse,
  UpdateStationRequest,
} from "../types/station";

export const getStations = async (
  status?: string,
): Promise<Station[]> => {
  const response = await api.get<StationsResponse>(
    "/api/stations",
    {
      params: status ? { status } : undefined,
    },
  );

  return response.data.stations;
};

export const getStation = async (
  stationId: string,
): Promise<Station> => {
  const response = await api.get<Station>(
    `/api/stations/${stationId}`,
  );

  return response.data;
};

export const createStation = async (
  request: CreateStationRequest,
): Promise<void> => {
  await api.post("/api/stations", request);
};

export const updateStation = async (
  stationId: string,
  request: UpdateStationRequest,
): Promise<void> => {
  await api.put(
    `/api/stations/${stationId}`,
    request,
  );
};

export const deactivateStation = async (
  stationId: string,
): Promise<void> => {
  await api.patch(
    `/api/stations/${stationId}/deactivate`,
  );
};

export const getNearbyStations = async (
  latitude: number,
  longitude: number,
  radiusKm: number,
): Promise<NearbyStation[]> => {
  const response =
    await api.get<NearbyStationsResponse>(
      "/api/nodes/nearby",
      {
        params: {
          latitude,
          longitude,
          radiusKm,
        },
      },
    );

  return response.data.stations;
};