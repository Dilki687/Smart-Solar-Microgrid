import { Navigate, Outlet } from "react-router";

import { useAuth } from "../context/AuthContext";

interface RoleRouteProps {
  allowedRoles: string[];
}

/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: Role Route
 * Author: Dilki
 * Description: Restricts frontend routes according to the
 *              authenticated user's role.
 */

const RoleRoute = ({ allowedRoles }: RoleRouteProps) => {
  const { user, isAuthenticated } = useAuth();

  // Redirect unauthenticated users to the login page.
  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />;
  }

  // Prevent users from accessing another role's pages.
  if (!allowedRoles.includes(user.role)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <Outlet />;
};

export default RoleRoute;
