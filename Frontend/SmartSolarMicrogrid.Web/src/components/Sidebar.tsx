import { NavLink } from "react-router";
import { useAuth } from "../context/AuthContext";

const Sidebar = () => {
  const { user } = useAuth();

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <div className="brand-icon">☀</div>

        <div>
          <h2>Smart Solar</h2>

          <span>Microgrid</span>
        </div>
      </div>

      <nav className="sidebar-nav">
        <div className="nav-section-title">MAIN</div>

        {user?.role === "BACKOFFICE" && (
          <>
            <NavLink
              to="/backoffice"
              end
              className={({ isActive }) =>
                isActive ? "nav-link active" : "nav-link"
              }
            >
              <span>▣</span>
              Dashboard
            </NavLink>

            <NavLink
              to="/backoffice/users"
              className={({ isActive }) =>
                isActive ? "nav-link active" : "nav-link"
              }
            >
              <span>◉</span>
              Users
            </NavLink>

            <NavLink
              to="/backoffice/prosumers"
              className={({ isActive }) =>
                isActive ? "nav-link active" : "nav-link"
              }
            >
              <span>♙</span>
              Prosumers
            </NavLink>

            <NavLink
              to="/backoffice/deactivation-requests"
              className={({ isActive }) =>
                isActive ? "nav-link active" : "nav-link"
              }
            >
              <span>⚠</span>
              Deactivation Requests
            </NavLink>
            <NavLink
  to="/backoffice/booking-slots"
  className={({ isActive }) =>
    `nav-link ${isActive ? "active" : ""}`
  }
> 
<span>🗓</span>
  Booking Slots
</NavLink>
<NavLink
  to="/backoffice/reservations"
  className={({ isActive }) =>
    `nav-link ${isActive ? "active" : ""}`
  }
>
   <span>📋</span>
  Reservations
</NavLink>
            <NavLink
  to="/backoffice/stations"
  className={({ isActive }) =>
    isActive ? "nav-link active" : "nav-link"
  }
>
  <span>📍</span>
  Stations
</NavLink>
          </>
        )}

        {user?.role === "GRID_OPERATOR" && (
  <>
    <NavLink
      to="/grid-operator"
      end
      className={({ isActive }) =>
        isActive ? "nav-link active" : "nav-link"
      }
    >
      <span>▣</span>
      Dashboard
    </NavLink>

    <NavLink
      to="/grid-operator/nearby"
      className={({ isActive }) =>
        isActive ? "nav-link active" : "nav-link"
      }
    >
      <span>📍</span>
      Nearby Stations
    </NavLink>
  </>
)}
        {user?.role === "PROSUMER" && (
          <>
            <NavLink
              to="/prosumer"
              end
              className={({ isActive }) =>
                isActive ? "nav-link active" : "nav-link"
              }
            >
              <span>▣</span>
              Dashboard
            </NavLink>
            <NavLink
  to="/prosumer/booking"
  className={({ isActive }) =>
    `nav-link ${isActive ? "active" : ""}`
  }
>
  <span>🗓</span>
  Booking
</NavLink>
<NavLink
  to="/prosumer/reservations"
  className={({ isActive }) =>
    `nav-link ${isActive ? "active" : ""}`
  }
><span>🗓</span>
  My Reservations
</NavLink>

            <NavLink
              to="/prosumer/profile"
              className={({ isActive }) =>
                isActive ? "nav-link active" : "nav-link"
              }
            >
              <span>♙</span>
              My Profile
            </NavLink>
          </>
        )}
      </nav>

      <div className="sidebar-footer">
        <div className="sidebar-user">
          <div className="user-avatar">
            {user?.name?.charAt(0).toUpperCase()}
          </div>

          <div className="sidebar-user-info">
            <strong>{user?.name}</strong>

            <span>{user?.role}</span>
          </div>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
