import { Navigate, Route, Routes } from "react-router";
import LoginPage from "./pages/auth/LoginPage";
import ProtectedRoute from "./components/ProtectedRoute";
import RoleRoute from "./components/RoleRoute";
import DashboardLayout from "./layouts/DashboardLayout";
import BackofficeDashboard from "./pages/backoffice/BackofficeDashboard";
import GridOperatorDashboard from "./pages/gridoperator/GridOperatorDashboard";
import ProsumerDashboard from "./pages/prosumer/ProsumerDashboard";
import UnauthorizedPage from "./pages/UnauthorizedPage";
// import ComingSoonPage from "./pages/ComingSoonPage";
import UserManagementPage from "./pages/backoffice/UserManagementPage";
import ProsumerManagementPage from "./pages/backoffice/ProsumerManagementPage";
import DeactivationRequestsPage from "./pages/backoffice/DeactivationRequestsPage";
import ProsumerRegistrationPage from "./pages/auth/ProsumerRegistrationPage";
import ProsumerProfilePage from "./pages/prosumer/ProsumerProfilePage";

function App() {
  return (
    <Routes>
      {/* Public authentication route */}
      <Route path="/login" element={<LoginPage />} />

      {/* Protected routes */}
      <Route element={<ProtectedRoute />}>
        <Route element={<DashboardLayout />}>
          {/* Backoffice routes */}
          <Route element={<RoleRoute allowedRoles={["BACKOFFICE"]} />}>
            <Route path="/backoffice" element={<BackofficeDashboard />} />

            <Route path="/backoffice/users" element={<UserManagementPage />} />

            <Route
              path="/backoffice/prosumers"
              element={<ProsumerManagementPage />}
            />

            <Route
              path="/backoffice/deactivation-requests"
              element={<DeactivationRequestsPage />}
            />
          </Route>

          {/* Grid Operator routes */}
          <Route element={<RoleRoute allowedRoles={["GRID_OPERATOR"]} />}>
            <Route path="/grid-operator" element={<GridOperatorDashboard />} />
          </Route>

          {/* Prosumer routes */}
          <Route element={<RoleRoute allowedRoles={["PROSUMER"]} />}>
            <Route path="/prosumer" element={<ProsumerDashboard />} />

            <Route path="/prosumer/profile" element={<ProsumerProfilePage />} />
          </Route>

          {/* Unauthorized */}
          <Route path="/unauthorized" element={<UnauthorizedPage />} />
        </Route>
      </Route>

      <Route path="/register/prosumer" element={<ProsumerRegistrationPage />} />

      {/* Default route */}
      <Route path="/" element={<Navigate to="/login" replace />} />

      {/* Unknown routes */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
