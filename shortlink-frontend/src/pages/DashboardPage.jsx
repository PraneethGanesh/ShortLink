import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
    clearToken,
    getCurrentUser,
} from "../api/authApi";
import "./RequestRegistrationPage.css";

export default function DashboardPage() {
    const navigate = useNavigate();

    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        let cancelled = false;

        async function loadCurrentUser() {
            try {
                const currentUser = await getCurrentUser();

                if (!cancelled) {
                    setUser(currentUser);
                }
            } catch (requestError) {
                if (!cancelled) {
                    clearToken();
                    setError(requestError.message);
                }
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        loadCurrentUser();

        return () => {
            cancelled = true;
        };
    }, []);

    const handleLogout = () => {
        clearToken();
        navigate("/login", { replace: true });
    };

    if (loading) {
        return (
            <div className="auth-screen">
                <div className="auth-card">
                    <h1 className="auth-title">
                        Loading dashboard
                    </h1>

                    <p className="auth-status-line">
                        Fetching your account…
                    </p>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="auth-screen">
                <div className="auth-card">
                    <h1 className="auth-title">
                        Session unavailable
                    </h1>

                    <div
                        className="auth-alert auth-alert--error"
                        role="alert"
                    >
                        {error}
                    </div>

                    <button
                        className="auth-submit"
                        onClick={() =>
                            navigate("/login", { replace: true })
                        }
                    >
                        Return to login
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="auth-screen">
            <div className="auth-card">
                <p className="auth-eyebrow">
                    ShortLink · User dashboard
                </p>

                <h1 className="auth-title">
                    Welcome, {user?.name}
                </h1>

                <p className="auth-subtitle">
                    You are authenticated using a JWT access token.
                </p>

                <div className="auth-alert auth-alert--success">
                    <p>
                        <strong>Email:</strong> {user?.email}
                    </p>

                    <p>
                        <strong>Role:</strong> {user?.role}
                    </p>

                    <p>
                        <strong>Status:</strong> {user?.status}
                    </p>
                </div>

                <button
                    type="button"
                    className="auth-submit"
                    onClick={handleLogout}
                >
                    Sign out
                </button>
            </div>
        </div>
    );
}