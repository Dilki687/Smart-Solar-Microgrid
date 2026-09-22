import { Link, Outlet, useNavigate } from "react-router";

import { useAuth } from "../context/AuthContext";

/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: Dashboard Layout
 * Author: Dilki
 * Description: Provides the common navigation layout for
 *              authenticated users.
 */

const DashboardLayout = () => {
  const { user, logout } = useAuth();

  const navigate = useNavigate();

  const handleLogout = async () => {
    // Log out through the authentication context.
    await logout();

    // Return the user to the login page.
    navigate("/login");
  };

  return (
    <div className="app-layout">
      <header className="topbar">
        <div>
          <strong>Smart Solar Microgrid</strong>
        </div>

        <div className="user-section">
          <span>{user?.name}</span>

          <span className="role-badge">{user?.role}</span>

          <button onClick={handleLogout}>Logout</button>
        </div>
      </header>

      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
};

export default DashboardLayout;
