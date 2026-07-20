import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import RequestRegistrationPage from "./pages/RequestRegistrationPage";
import CompleteRegistrationPage from "./pages/CompleteRegistrationPage";
import LoginPage from "./pages/LoginPage";
import DashboardPage from "./pages/DashboardPage";
import ProtectedRoute from "./components/ProtectedRoute";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/register" replace />} />
        <Route path="/register" element={<RequestRegistrationPage />} />
        <Route path="/register/complete" element={<CompleteRegistrationPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <DashboardPage />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<Navigate to="/register" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
