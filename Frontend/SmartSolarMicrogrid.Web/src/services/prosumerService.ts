import api from "./api";
import type {
  CreateProsumerRequest,
  DeactivationRequest,
  Prosumer,
  UpdateProsumerRequest,
} from "../types/user";

/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: Prosumer Service
 * Author: Dilki
 * Description: Provides API functions for prosumer
 *              registration, retrieval, updating and
 *              account status management.
 */

/**
 * Get a prosumer using their NIC.
 */
export const getProsumerByNic = async (nic: string): Promise<Prosumer> => {
  const response = await api.get(`/api/prosumers/${encodeURIComponent(nic)}`);

  return response.data;
};

/**
 * Register a new prosumer.
 */
export const createProsumer = async (
  request: CreateProsumerRequest,
): Promise<void> => {
  await api.post("/api/prosumers", request);
};

/**
 * Update an existing prosumer.
 */
export const updateProsumer = async (
  nic: string,
  request: UpdateProsumerRequest,
): Promise<void> => {
  await api.put(`/api/prosumers/${encodeURIComponent(nic)}`, request);
};

/**
 * Request deactivation of a prosumer account.
 */
export const requestProsumerDeactivation = async (
  nic: string,
): Promise<void> => {
  await api.patch(
    `/api/prosumers/${encodeURIComponent(nic)}/deactivation-request`,
  );
};

/**
 * Get all pending prosumer deactivation requests.
 */
export const getDeactivationRequests = async (): Promise<
  DeactivationRequest[]
> => {
  const response = await api.get("/api/prosumers/deactivation-requests");

  return response.data;
};

/**
 * Deactivate a prosumer after Backoffice approval.
 */
export const deactivateProsumer = async (nic: string): Promise<void> => {
  await api.patch(`/api/prosumers/${encodeURIComponent(nic)}/deactivate`);
};

/**
 * Reactivate an inactive prosumer.
 */
export const reactivateProsumer = async (nic: string): Promise<void> => {
  await api.patch(`/api/prosumers/${encodeURIComponent(nic)}/reactivate`);
};
