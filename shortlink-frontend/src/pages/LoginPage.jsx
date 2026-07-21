import { useEffect, useState } from "react";
import {
    Link,
    useLocation,
    useNavigate,
    useSearchParams,
} from "react-router-dom";
import { login, storeToken } from "../api/authApi";
import "./RequestRegistrationPage.css";

export default function LoginPage() {
    const navigate = useNavigate();
    const location = useLocation();
    const [searchParams] = useSearchParams();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [totpCode, setTotpCode] = useState("");
    const [twoFactorRequired, setTwoFactorRequired] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const successMessage = location.state?.message;
    const previousLocation =
        location.state?.from?.pathname || "/dashboard";

    useEffect(() => {
        const oauthToken = searchParams.get("token");
        if (oauthToken) {
            storeToken(oauthToken);
            navigate("/dashboard", { replace: true });
        }
    }, [navigate, searchParams]);

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError("");

        if (!email.trim() || !password) {
            setError("Email and password are required.");
            return;
        }

        if (twoFactorRequired && !totpCode.trim()) {
            setError("Enter your two-factor authentication code.");
            return;
        }

        setLoading(true);

        try {
            const response = await login({
                email: email.trim(),
                password,
                totpCode: totpCode.trim() || null,
            });

            if (response.twoFactorRequired) {
                setTwoFactorRequired(true);
                setTotpCode("");
                return;
            }

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
                            disabled={loading || twoFactorRequired}
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
                            disabled={loading || twoFactorRequired}
                            autoComplete="current-password"
                        />
                    </div>

                    {twoFactorRequired && (
                        <div className="auth-field">
                            <label
                                className="auth-label"
                                htmlFor="totpCode"
                            >
                                2FA code
                            </label>

                            <input
                                id="totpCode"
                                type="text"
                                inputMode="numeric"
                                className="auth-input"
                                value={totpCode}
                                onChange={(event) =>
                                    setTotpCode(event.target.value)
                                }
                                disabled={loading}
                                autoComplete="one-time-code"
                                maxLength={6}
                            />
                        </div>
                    )}

                    <button
                        type="submit"
                        className="auth-submit"
                        disabled={loading}
                    >
                        {loading ? "Signing in..." : twoFactorRequired ? "Verify code" : "Sign in"}
                    </button>
                </form>

                <a
                    className="auth-submit github-login-button"
                    href="http://localhost:8080/oauth2/authorization/github"
                >
                    Sign in with GitHub
                </a>

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