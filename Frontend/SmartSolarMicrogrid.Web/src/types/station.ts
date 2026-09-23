export interface Station {
  stationId: string;
  name: string;
  address: string;
  latitude: number;
  longitude: number;
  capacityKw: number;
  status: string;
  operatorUserId: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface StationsResponse {
  stations: Station[];
}

export interface CreateStationRequest {
  name: string;
  address: string;
  latitude: number;
  longitude: number;
  capacityKw: number;
  operatorUserId: string;
}

export type UpdateStationRequest = CreateStationRequest;

export interface NearbyStation {
  nodeId: string;
  name: string;
  latitude: number;
  longitude: number;
  distanceKm: number;
  availableSlots: number;
  status: string;
}

export interface NearbyStationsResponse {
  stations: NearbyStation[];
}