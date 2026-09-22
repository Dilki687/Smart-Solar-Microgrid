import api from "./api";
import type { LoginRequest, LoginResponse } from "../types/auth";

/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: Authentication Service
 * Author: Dilki
 * Description: Handles communication with authentication endpoints
 *              provided by the ASP.NET Core backend.
 */

export const loginUser = async (
  request: LoginRequest,
): Promise<LoginResponse> => {
  // Send the login credentials to the backend authentication API.
  const response = await api.post<LoginResponse>("/api/auth/login", request);

  return response.data;
};

export const logoutUser = async (): Promise<void> => {
  // Request the backend to revoke the current JWT token.
  await api.post("/api/auth/logout");
};
