import { Link } from "react-router";

function HomePage() {
  return (
    <div className="home-page">
      <div className="home-overlay">
        <div className="home-content">

          <div className="home-icon">
            ☀
          </div>

          <p className="home-system-label">
            SMART ENERGY MANAGEMENT PLATFORM
          </p>

          <h1>
            Smart Solar
            <br />
            Microgrid Trading System
          </h1>

          <p className="home-description">
            Manage solar energy generation, reservations, microgrid stations,
            and energy transactions through one integrated platform.
          </p>

          <div className="home-actions">
            <Link to="/login" className="home-button home-button-primary">
              Login
            </Link>

            <Link
              to="/register/prosumer"
              className="home-button home-button-secondary"
            >
              Register
            </Link>
          </div>

          <div className="home-features">
            <div className="home-feature">
              <span>☀</span>
              <div>
                <strong>Solar Energy</strong>
                <small>Smart energy management</small>
              </div>
            </div>

            <div className="home-feature">
              <span>⚡</span>
              <div>
                <strong>Energy Trading</strong>
                <small>Reserve and manage energy</small>
              </div>
            </div>

            <div className="home-feature">
              <span>⌖</span>
              <div>
                <strong>Microgrid Stations</strong>
                <small>Find nearby stations</small>
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
}

export default HomePage;