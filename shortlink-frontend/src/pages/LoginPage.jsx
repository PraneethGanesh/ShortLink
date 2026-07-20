import { useState } from "react";
import {
    Link,
    useLocation,
    useNavigate,
} from "react-router-dom";
import { login } from "../api/authApi";
import "./RequestRegistrationPage.css";

export default function LoginPage() {
    const navigate = useNavigate();
    const location = useLocation();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const successMessage = location.state?.message;
    const previousLocation =
        location.state?.from?.pathname || "/dashboard";

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError("");

        if (!email.trim() || !password) {
            setError("Email and password are required.");
            return;
        }

        setLoading(true);

        try {
            await login({
                email: email.trim(),
                password,
            });

            navigate(previousLocation, {
                replace: true,
            });
        } catch (loginError) {
            setError(loginError.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-screen">
            <div className="auth-card">
                <p className="auth-eyebrow">
                    ShortLink · Authentication
                </p>

                <h1 className="auth-title">Sign in</h1>

                <p className="auth-subtitle">
                    Enter your account credentials to continue.
                </p>

                {successMessage && (
                    <div
                        className="auth-alert auth-alert--success"
                        role="status"
                    >
                        {successMessage}
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    <div className="auth-field">
                        <label className="auth-label" htmlFor="email">
                            Email address
                        </label>

                        <input
                            id="email"
                            type="email"
                            className="auth-input"
                            value={email}
                            onChange={(event) =>
                                setEmail(event.target.value)
                            }
                            disabled={loading}
                            autoComplete="email"
                        />
                    </div>

                    <div className="auth-field">
                        <label
                            className="auth-label"
                            htmlFor="password"
                        >
                            Password
                        </label>

                        <input
                            id="password"
                            type="password"
                            className="auth-input"
                            value={password}
                            onChange={(event) =>
                                setPassword(event.target.value)
                            }
                            disabled={loading}
                            autoComplete="current-password"
                        />
                    </div>

                    <button
                        type="submit"
                        className="auth-submit"
                        disabled={loading}
                    >
                        {loading ? "Signing in…" : "Sign in"}
                    </button>
                </form>

                {error && (
                    <div
                        className="auth-alert auth-alert--error"
                        role="alert"
                    >
                        {error}
                    </div>
                )}

                <p className="auth-footer-note">
                    Need an account?{" "}
                    <Link className="auth-link" to="/register">
                        Register
                    </Link>
                </p>
            </div>
        </div>
    );
}