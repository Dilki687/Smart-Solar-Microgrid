import { Navigate, Outlet } from "react-router";

import { useAuth } from "../context/AuthContext";

/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: Protected Route
 * Author: Dilki
 * Description: Prevents unauthenticated users from accessing
 *              protected application pages.
 */

const ProtectedRoute = () => {
  const { isAuthenticated } = useAuth();

  // Redirect unauthenticated users to the login page.
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
};

export default ProtectedRoute;
