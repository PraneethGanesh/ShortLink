import { useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import {
    completeRegistration,
    validateRegistrationToken,
} from "../api/authApi";
import "./RequestRegistrationPage.css";

export default function CompleteRegistrationPage() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();

    const token = searchParams.get("token") || "";

    const [validating, setValidating] = useState(true);
    const [tokenValid, setTokenValid] = useState(false);
    const [tokenMessage, setTokenMessage] = useState("");

    const [name, setName] = useState("");
    const [password, setPassword] = useState("");
    const [phoneNumber, setPhoneNumber] = useState("");

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        let cancelled = false;

        async function validateToken() {
            if (!token) {
                setTokenMessage("Registration token is missing.");
                setValidating(false);
                return;
            }

            try {
                const response =
                    await validateRegistrationToken(token);

                if (!cancelled) {
                    setTokenValid(response.valid);
                    setTokenMessage(response.message);
                }
            } catch (validationError) {
                if (!cancelled) {
                    setTokenMessage(validationError.message);
                }
            } finally {
                if (!cancelled) {
                    setValidating(false);
                }
            }
        }

        validateToken();

        return () => {
            cancelled = true;
        };
    }, [token]);

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError("");

        if (!name.trim()) {
            setError("Name is required.");
            return;
        }

        if (password.length < 8) {
            setError(
                "Password must contain at least 8 characters."
            );
            return;
        }

        setLoading(true);

        try {
            await completeRegistration({
                token,
                name: name.trim(),
                password,
                phoneNumber: phoneNumber.trim(),
            });

            navigate("/login", {
                replace: true,
                state: {
                    message:
                        "Registration completed. You can now sign in.",
                },
            });
        } catch (registrationError) {
            setError(registrationError.message);
        } finally {
            setLoading(false);
        }
    };

    if (validating) {
        return (
            <div className="auth-screen">
                <div className="auth-card">
                    <h1 className="auth-title">
                        Validating registration link
                    </h1>

                    <p className="auth-status-line">
                        Please wait while we validate your link…
                    </p>
                </div>
            </div>
        );
    }

    if (!tokenValid) {
        return (
            <div className="auth-screen">
                <div className="auth-card">
                    <h1 className="auth-title">
                        Invalid registration link
                    </h1>

                    <div
                        className="auth-alert auth-alert--error"
                        role="alert"
                    >
                        {tokenMessage}
                    </div>

                    <p className="auth-footer-note">
                        <Link className="auth-link" to="/register">
                            Request a new registration link
                        </Link>
                    </p>
                </div>
            </div>
        );
    }

    return (
        <div className="auth-screen">
            <div className="auth-card">
                <p className="auth-eyebrow">
                    Identity Service · Account setup
                </p>

                <h1 className="auth-title">
                    Complete registration
                </h1>

                <p className="auth-subtitle">
                    Enter your details to create your ShortLink account.
                </p>

                <form onSubmit={handleSubmit}>
                    <div className="auth-field">
                        <label className="auth-label" htmlFor="name">
                            Full name
                        </label>

                        <input
                            id="name"
                            type="text"
                            className="auth-input"
                            value={name}
                            onChange={(event) =>
                                setName(event.target.value)
                            }
                            disabled={loading}
                            autoComplete="name"
                        />
                    </div>

                    <div className="auth-field">
                        <label
                            className="auth-label"
                            htmlFor="phoneNumber"
                        >
                            Phone number
                        </label>

                        <input
                            id="phoneNumber"
                            type="tel"
                            className="auth-input"
                            value={phoneNumber}
                            onChange={(event) =>
                                setPhoneNumber(event.target.value)
                            }
                            disabled={loading}
                            autoComplete="tel"
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
                            minLength={8}
                            autoComplete="new-password"
                        />
                    </div>

                    <button
                        type="submit"
                        className="auth-submit"
                        disabled={loading}
                    >
                        {loading
                            ? "Creating account…"
                            : "Create account"}
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
            </div>
        </div>
    );
}