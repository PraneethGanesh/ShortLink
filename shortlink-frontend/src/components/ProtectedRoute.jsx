import { Navigate, useLocation } from "react-router-dom";
import { isAuthenticated } from "../api/authApi";

/**
 * Wraps a route element and redirects to /login when no JWT is stored.
 * Usage: <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
 */
export default function ProtectedRoute({ children }) {
  const location = useLocation();

  if (!isAuthenticated()) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return children;
}
