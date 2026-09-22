import { Outlet, useNavigate } from "react-router";

import { useAuth } from "../context/AuthContext";

import Sidebar from "../components/Sidebar";

/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: Dashboard Layout
 * Author: Dilki
 * Description: Provides the shared application layout including
 *              sidebar navigation, top navigation and page content.
 */

const DashboardLayout = () => {
  const { user, logout } = useAuth();

  const navigate = useNavigate();

  const handleLogout = async () => {
    // Log out through the authentication context.
    await logout();

    // Redirect the user to the login page.
    navigate("/login");
  };

  return (
    <div className="app-shell">
      <Sidebar />

      <div className="content-area">
        <header className="topbar">
          <div className="topbar-title">Smart Solar Microgrid</div>

          <div className="topbar-user">
            <div className="topbar-user-details">
              <strong>{user?.name}</strong>

              <span>{user?.role}</span>
            </div>

            <div className="topbar-avatar">
              {user?.name?.charAt(0).toUpperCase()}
            </div>

            <button className="logout-button" onClick={handleLogout}>
              Logout
            </button>
          </div>
        </header>

        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default DashboardLayout;
