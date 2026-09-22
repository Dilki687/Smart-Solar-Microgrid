/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: Authentication Types
 * Author: Dilki
 * Description: Defines TypeScript types used by the authentication module.
 */

export interface LoginRequest {
  identifier: string;
  password: string;
}

export interface LoginResponse {
  message: string;
  token: string;
  expiresAt: string;
  user: {
    userId: string;
    nic: string;
    name: string;
    email: string;
    role: string;
    status: string;
  };
}

export interface AuthUser {
  userId: string;
  nic: string;
  name: string;
  email: string;
  role: string;
  status: string;
}

export interface AuthContextType {
  user: AuthUser | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (identifier: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}
