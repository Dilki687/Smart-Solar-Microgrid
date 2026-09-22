import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";

import { loginUser, logoutUser } from "../services/authService";

import type { AuthContextType, AuthUser } from "../types/auth";

/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: Authentication Context
 * Author: Dilki
 * Description: Maintains the authenticated user's session,
 *              JWT token and role information.
 */

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider = ({ children }: AuthProviderProps) => {
  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem("smartSolarToken"),
  );

  const [user, setUser] = useState<AuthUser | null>(() => {
    const storedUser = localStorage.getItem("smartSolarUser");

    if (!storedUser) {
      return null;
    }

    try {
      return JSON.parse(storedUser);
    } catch {
      return null;
    }
  });

  useEffect(() => {
    if (token) {
      localStorage.setItem("smartSolarToken", token);
    } else {
      localStorage.removeItem("smartSolarToken");
    }
  }, [token]);

  useEffect(() => {
    if (user) {
      localStorage.setItem("smartSolarUser", JSON.stringify(user));
    } else {
      localStorage.removeItem("smartSolarUser");
    }
  }, [user]);

  const login = async (identifier: string, password: string) => {
    // Send the credentials to the backend and store the returned session.
    const response = await loginUser({
      identifier,
      password,
    });

    setToken(response.token);
    setUser(response.user);
  };

  const logout = async () => {
    try {
      // Ask the backend to revoke the current JWT.
      if (token) {
        await logoutUser();
      }
    } finally {
      // Clear the local authentication state even if the API request fails.
      setToken(null);
      setUser(null);
    }
  };

  const value: AuthContextType = {
    user,
    token,
    isAuthenticated: Boolean(token && user),
    login,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }

  return context;
};
