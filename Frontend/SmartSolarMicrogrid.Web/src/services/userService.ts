import api from "./api";
import type {
  CreateUserRequest,
  UpdateUserRequest,
  User,
  UsersResponse,
} from "../types/user";

/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: User Service
 * Author: Dilki
 * Description: Provides API functions for Backoffice user management.
 */

/**
 * Get all Backoffice and Grid Operator users.
 */
export const getUsers = async (): Promise<User[]> => {
  const response = await api.get<UsersResponse>("/api/users");

  // Convert the backend response format into the
  // frontend User format.
  return response.data.users.map((user) => ({
    userId: user.id,
    nic: user.nic,
    name: user.name,
    email: user.email,
    phone: user.phone ?? "",
    address: user.address ?? "",
    role: user.role,
    status: user.status,
    createdAt: user.createdAt,
  }));
};

/**
 * Create a new Backoffice or Grid Operator user.
 */
export const createUser = async (request: CreateUserRequest): Promise<void> => {
  await api.post("/api/users", request);
};

/**
 * Update an existing Backoffice or Grid Operator user.
 */
export const updateUser = async (
  userId: string,
  request: UpdateUserRequest,
): Promise<void> => {
  await api.put(`/api/users/${userId}`, request);
};

/**
 * Deactivate an existing Backoffice or Grid Operator user.
 */
export const deactivateUser = async (userId: string): Promise<void> => {
  await api.patch(`/api/users/${userId}/deactivate`);
};
