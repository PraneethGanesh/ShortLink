import { useState } from "react";
import { requestRegistration } from "../api/authApi";
import "./RequestRegistrationPage.css";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const STEPS = [
  { label: "Request" },
  { label: "Verify" },
  { label: "Set password" },
  { label: "Sign in" },
];

export default function RequestRegistrationPage() {
  const [email, setEmail] = useState("");
  const [fieldError, setFieldError] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const handleSubmit = async (event) => {
    event.preventDefault();

    const trimmedEmail = email.trim();

    if (!trimmedEmail) {
      setFieldError("Enter your email to continue.");
      return;
    }
    if (!EMAIL_PATTERN.test(trimmedEmail)) {
      setFieldError("Enter a valid email address.");
      return;
    }

    setFieldError("");
    setError("");
    setMessage("");
    setLoading(true);

    try {
      const backendMessage = await requestRegistration(trimmedEmail);
      setMessage(backendMessage || "Check your inbox for a link to finish setting up your account.");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-screen">
      <div className="auth-card">
        <p className="auth-eyebrow">Identity Service · Access request</p>
        <h1 className="auth-title">Request access</h1>
        <p className="auth-subtitle">
          Enter your work email and we'll send you a link to set up your account.
        </p>

        <div className="step-tracker" aria-hidden="true">
          {STEPS.map((step, index) => (
            <div key={step.label} style={{ display: "contents" }}>
              <div className={`step-tracker__item${index === 0 ? " is-active" : ""}`}>
                <span className="step-tracker__dot">{index + 1}</span>
                <span className="step-tracker__label">{step.label}</span>
              </div>
              {index < STEPS.length - 1 && <div className="step-tracker__line" />}
            </div>
          ))}
        </div>

        <form onSubmit={handleSubmit} noValidate>
          <div className="auth-field">
            <label className="auth-label" htmlFor="email">
              Email address
            </label>
            <input
              id="email"
              name="email"
              type="email"
              className="auth-input"
              placeholder="you@company.com"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              aria-invalid={Boolean(fieldError)}
              aria-describedby={fieldError ? "email-error" : undefined}
              disabled={loading}
              autoComplete="email"
            />
            {fieldError && (
              <p className="auth-field-error" id="email-error" role="alert">
                {fieldError}
              </p>
            )}
          </div>

          <button type="submit" className="auth-submit" disabled={loading}>
            {loading && <span className="auth-spinner" aria-hidden="true" />}
            {loading ? "Sending request…" : "Request registration link"}
          </button>
        </form>

        {error && (
          <div className="auth-alert auth-alert--error" role="alert">
            {error}
          </div>
        )}

        {message && !error && (
          <div className="auth-alert auth-alert--success" role="status">
            {message}
          </div>
        )}

        <p className="auth-status-line">
          <span className="auth-status-line__prompt">›</span>
          {loading
            ? "contacting identity-service…"
            : message
              ? "request accepted, awaiting verification"
              : "waiting for input"}
        </p>

        <p className="auth-footer-note">
          Already have an account? <a className="auth-link" href="/login">Sign in</a>
        </p>
      </div>
    </div>
  );
}
