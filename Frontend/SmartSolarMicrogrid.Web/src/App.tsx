import { Navigate, Route, Routes } from "react-router";

import LoginPage from "./pages/auth/LoginPage";

import ProtectedRoute from "./components/ProtectedRoute";

import RoleRoute from "./components/RoleRoute";

import DashboardLayout from "./layouts/DashboardLayout";

import BackofficeDashboard from "./pages/backoffice/BackofficeDashboard";

import GridOperatorDashboard from "./pages/gridoperator/GridOperatorDashboard";

import ProsumerDashboard from "./pages/prosumer/ProsumerDashboard";

import UnauthorizedPage from "./pages/UnauthorizedPage";

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
          </Route>

          {/* Grid Operator routes */}
          <Route element={<RoleRoute allowedRoles={["GRID_OPERATOR"]} />}>
            <Route path="/grid-operator" element={<GridOperatorDashboard />} />
          </Route>

          {/* Prosumer routes */}
          <Route element={<RoleRoute allowedRoles={["PROSUMER"]} />}>
            <Route path="/prosumer" element={<ProsumerDashboard />} />
          </Route>

          {/* Unauthorized */}
          <Route path="/unauthorized" element={<UnauthorizedPage />} />
        </Route>
      </Route>

      {/* Default route */}
      <Route path="/" element={<Navigate to="/login" replace />} />

      {/* Unknown routes */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
