import { Link } from "react-router";

/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: Backoffice Dashboard
 * Author: Dilki
 * Description: Main dashboard for Backoffice users.
 *              Provides an overview and quick navigation
 *              to user and Prosumer administration features.
 */

const BackofficeDashboard = () => {
  return (
    <div className="backoffice-dashboard">
      {/* =========================================
          Dashboard Header
         ========================================= */}

      {/* <section className="backoffice-hero">
        <div className="backoffice-hero-content">
          <div>
            <div className="dashboard-eyebrow">
              <span className="eyebrow-dot"></span>
              SYSTEM ADMINISTRATION
            </div>

            <h1>Backoffice Dashboard</h1>

            <p>
              Manage users, Prosumer accounts and account administration from
              one central workspace.
            </p>
          </div>

          <div className="hero-energy-icon">
            <span>☀</span>
          </div>
        </div>
      </section> */}

      {/* =========================================
          Overview Cards
         ========================================= */}

      <section className="dashboard-overview">
        <div className="overview-card overview-card-blue">
          <div className="overview-icon">👥</div>

          <div className="overview-content">
            <span className="overview-label">User Administration</span>

            <strong>Manage Users</strong>

            <p>Backoffice & Grid Operator accounts</p>
          </div>

          <Link to="/backoffice/users" className="overview-arrow">
            →
          </Link>
        </div>

        <div className="overview-card overview-card-yellow">
          <div className="overview-icon">⚡</div>

          <div className="overview-content">
            <span className="overview-label">Prosumer Management</span>

            <strong>Manage Prosumers</strong>

            <p>View accounts and account status</p>
          </div>

          <Link to="/backoffice/prosumers" className="overview-arrow">
            →
          </Link>
        </div>

        <div className="overview-card overview-card-red">
          <div className="overview-icon">🔔</div>

          <div className="overview-content">
            <span className="overview-label">Requires Attention</span>

            <strong>Deactivation Requests</strong>

            <p>Review pending Prosumer requests</p>
          </div>

          <Link
            to="/backoffice/deactivation-requests"
            className="overview-arrow"
          >
            →
          </Link>
        </div>
      </section>

      {/* =========================================
          Main Dashboard Grid
         ========================================= */}

      <section className="backoffice-dashboard-grid">
        {/* Quick Actions */}

        <div className="dashboard-panel quick-actions-panel">
          <div className="dashboard-panel-header">
            <div>
              <span className="panel-eyebrow">ADMINISTRATION</span>

              <h2>Quick Actions</h2>

              <p>Frequently used management functions</p>
            </div>

            <div className="panel-icon">⚙</div>
          </div>

          <div className="advanced-action-grid">
            <Link
              to="/backoffice/users"
              className="advanced-action-card action-blue"
            >
              <div className="advanced-action-icon">👤</div>

              <div className="advanced-action-content">
                <h3>Manage Users</h3>

                <p>
                  Create, update and manage Backoffice and Grid Operator
                  accounts.
                </p>
              </div>

              <span className="action-arrow">→</span>
            </Link>

            <Link
              to="/backoffice/prosumers"
              className="advanced-action-card action-yellow"
            >
              <div className="advanced-action-icon">☀</div>

              <div className="advanced-action-content">
                <h3>Manage Prosumers</h3>

                <p>
                  Search and review Prosumer accounts and their current status.
                </p>
              </div>

              <span className="action-arrow">→</span>
            </Link>

            <Link
              to="/backoffice/deactivation-requests"
              className="advanced-action-card action-red"
            >
              <div className="advanced-action-icon">🔔</div>

              <div className="advanced-action-content">
                <h3>Deactivation Requests</h3>

                <p>
                  Review Prosumer requests and perform account administration.
                </p>
              </div>

              <span className="action-arrow">→</span>
            </Link>
          </div>
        </div>

        {/* System Overview */}

        <div className="dashboard-panel system-panel">
          <div className="dashboard-panel-header">
            <div>
              <span className="panel-eyebrow">SYSTEM</span>

              <h2>System Overview</h2>

              <p>Smart Solar Microgrid administration</p>
            </div>

            <div className="system-status">
              <span className="system-status-dot"></span>
              Operational
            </div>
          </div>

          <div className="system-overview-list">
            <div className="system-overview-item">
              <div className="system-item-icon blue">👥</div>

              <div>
                <strong>User Management</strong>

                <span>Account administration</span>
              </div>

              <span className="system-item-status">Available</span>
            </div>

            <div className="system-overview-item">
              <div className="system-item-icon yellow">☀</div>

              <div>
                <strong>Prosumer Management</strong>

                <span>Prosumer account administration</span>
              </div>

              <span className="system-item-status">Available</span>
            </div>

            <div className="system-overview-item">
              <div className="system-item-icon red">!</div>

              <div>
                <strong>Account Requests</strong>

                <span>Deactivation request workflow</span>
              </div>

              <span className="system-item-status attention">Review</span>
            </div>
          </div>
        </div>
      </section>

      {/* =========================================
          Information Banner
         ========================================= */}

      <section className="dashboard-info-banner">
        <div className="info-banner-icon">🔐</div>

        <div>
          <strong>Backoffice Administration</strong>

          <p>
            Administrative actions are protected by role-based authorization and
            are processed through the central Smart Solar Microgrid API.
          </p>
        </div>
      </section>
    </div>
  );
};

export default BackofficeDashboard;
