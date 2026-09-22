import { Link } from "react-router";

/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: Backoffice Dashboard
 * Author: Dilki
 * Description: Provides the main dashboard for Backoffice users
 *              and navigation to user management functions.
 */

const BackofficeDashboard = () => {
  return (
    <div>
      <div className="dashboard-header">
        <h1>Backoffice Dashboard</h1>

        <p>
          Manage users, Prosumer accounts and account deactivation requests.
        </p>
      </div>

      <div className="dashboard-grid">
        <div className="stat-card">
          <div className="stat-card-label">User Management</div>

          <div className="stat-card-value">→</div>
        </div>

        <div className="stat-card">
          <div className="stat-card-label">Deactivation Requests</div>

          <div className="stat-card-value">→</div>
        </div>

        <div className="stat-card">
          <div className="stat-card-label">Account Administration</div>

          <div className="stat-card-value">✓</div>
        </div>
      </div>

      <h2>Quick Actions</h2>

      <div className="quick-actions">
        <Link to="/backoffice/users" className="action-card">
          <h3>Manage Users</h3>

          <p>
            Create, update and deactivate Backoffice and Grid Operator accounts.
          </p>
        </Link>

        <Link to="/backoffice/prosumers" className="action-card">
          <h3>Manage Prosumers</h3>

          <p>View Prosumer account information and account status.</p>
        </Link>

        <Link to="/backoffice/deactivation-requests" className="action-card">
          <h3>Deactivation Requests</h3>

          <p>Review Prosumer deactivation requests and approve them.</p>
        </Link>
      </div>
    </div>
  );
};

export default BackofficeDashboard;
