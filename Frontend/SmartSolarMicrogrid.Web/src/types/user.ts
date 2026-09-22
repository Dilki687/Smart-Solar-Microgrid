/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: User Types
 * Author: Dilki
 * Description: Defines user roles, account statuses and user-related
 *              TypeScript models.
 */

export const USER_ROLES = {
  BACKOFFICE: "BACKOFFICE",
  GRID_OPERATOR: "GRID_OPERATOR",
  PROSUMER: "PROSUMER",
} as const;

export type UserRole = (typeof USER_ROLES)[keyof typeof USER_ROLES];

export const ACCOUNT_STATUS = {
  ACTIVE: "ACTIVE",
  INACTIVE: "INACTIVE",
  PENDING_DEACTIVATION: "PENDING_DEACTIVATION",
} as const;

export type AccountStatus =
  (typeof ACCOUNT_STATUS)[keyof typeof ACCOUNT_STATUS];

export interface User {
  userId: string;
  nic: string;
  name: string;
  email: string;
  phone: string;
  address: string;
  role: UserRole;
  status: AccountStatus;
  createdAt?: string;
}

export interface CreateUserRequest {
  nic: string;
  name: string;
  email: string;
  phone: string;
  address: string;
  password: string;
  role: "BACKOFFICE" | "GRID_OPERATOR";
}

export interface UpdateUserRequest {
  name: string;
  email: string;
  phone: string;
  address: string;
  role: "BACKOFFICE" | "GRID_OPERATOR";
}

export interface Prosumer {
  nic: string;
  name: string;
  email: string;
  phone: string;
  address: string;
  status: AccountStatus;
}

export interface DeactivationRequest {
  nic: string;
  name: string;
  requestedAt: string | null;
  status: AccountStatus;
}
