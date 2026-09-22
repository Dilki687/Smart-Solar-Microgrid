import { Link } from "react-router";

/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: Backoffice Dashboard
 * Author: Dilki
 * Description: Main dashboard for Backoffice users.
 */

const BackofficeDashboard = () => {
  return (
    <div>
      <h1>Backoffice Dashboard</h1>

      <p>Welcome to the Smart Solar Microgrid Backoffice system.</p>

      <div className="dashboard-links">
        <Link to="/backoffice/users">Manage Users</Link>

        <Link to="/backoffice/deactivation-requests">
          Prosumer Deactivation Requests
        </Link>
      </div>
    </div>
  );
};

export default BackofficeDashboard;
