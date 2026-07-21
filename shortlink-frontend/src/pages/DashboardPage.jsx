import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
    clearToken,
    createShortUrl,
    deleteShortUrl,
    disableTotp,
    enableTotp,
    getCurrentUser,
    listShortUrls,
    setShortUrlEnabled,
    setupTotp,
} from "../api/authApi";
import "./RequestRegistrationPage.css";

export default function DashboardPage() {
    const navigate = useNavigate();
    const [user, setUser] = useState(null);
    const [links, setLinks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const [longUrl, setLongUrl] = useState("");
    const [customAlias, setCustomAlias] = useState("");
    const [expiresAt, setExpiresAt] = useState("");
    const [totpSetup, setTotpSetup] = useState(null);
    const [totpCode, setTotpCode] = useState("");

    useEffect(() => {
        let cancelled = false;
        async function loadDashboard() {
            try {
                const [currentUser, urlPage] = await Promise.all([
                    getCurrentUser(),
                    listShortUrls(),
                ]);
                if (!cancelled) {
                    setUser(currentUser);
                    setLinks(urlPage.content || []);
                }
            } catch (requestError) {
                if (!cancelled) {
                    clearToken();
                    setError(requestError.message);
                }
            } finally {
                if (!cancelled) setLoading(false);
            }
        }
        loadDashboard();
        return () => {
            cancelled = true;
        };
    }, []);

    const resetMessages = () => {
        setError("");
        setSuccess("");
    };

    const handleLogout = () => {
        clearToken();
        navigate("/login", { replace: true });
    };

    const reloadLinks = async () => {
        const urlPage = await listShortUrls();
        setLinks(urlPage.content || []);
    };

    const handleCreate = async (event) => {
        event.preventDefault();
        resetMessages();
        if (!longUrl.trim()) {
            setError("Long URL is required.");
            return;
        }
        setSaving(true);
        try {
            const created = await createShortUrl({
                longUrl: longUrl.trim(),
                customAlias: customAlias.trim(),
                expiresAt: expiresAt ? new Date(expiresAt).toISOString() : null,
            });
            setLinks((currentLinks) => [created, ...currentLinks]);
            setLongUrl("");
            setCustomAlias("");
            setExpiresAt("");
            setSuccess("Short URL created.");
        } catch (createError) {
            setError(createError.message);
        } finally {
            setSaving(false);
        }
    };

    const handleToggleEnabled = async (link) => {
        resetMessages();
        try {
            const updated = await setShortUrlEnabled(link.id, !link.enabled);
            setLinks((currentLinks) =>
                currentLinks.map((currentLink) => currentLink.id === updated.id ? updated : currentLink)
            );
        } catch (toggleError) {
            setError(toggleError.message);
        }
    };

    const handleDelete = async (id) => {
        resetMessages();
        try {
            await deleteShortUrl(id);
            setLinks((currentLinks) => currentLinks.filter((link) => link.id !== id));
            setSuccess("Short URL deleted.");
        } catch (deleteError) {
            setError(deleteError.message);
        }
    };

    const handleCopy = async (shortUrl) => {
        try {
            await navigator.clipboard.writeText(shortUrl);
            setSuccess("Short URL copied.");
        } catch {
            setError("Could not copy the short URL.");
        }
    };

    const handleSetupTotp = async () => {
        resetMessages();
        try {
            const setup = await setupTotp();
            setTotpSetup(setup);
            setSuccess("Scan the 2FA secret, then enter the code to enable it.");
        } catch (setupError) {
            setError(setupError.message);
        }
    };

    const handleEnableTotp = async () => {
        resetMessages();
        try {
            const status = await enableTotp(totpCode.trim());
            setUser((currentUser) => ({ ...currentUser, totpEnabled: status.enabled }));
            setTotpCode("");
            setTotpSetup(null);
            setSuccess(status.message);
        } catch (enableError) {
            setError(enableError.message);
        }
    };

    const handleDisableTotp = async () => {
        resetMessages();
        try {
            const status = await disableTotp(totpCode.trim());
            setUser((currentUser) => ({ ...currentUser, totpEnabled: status.enabled }));
            setTotpCode("");
            setTotpSetup(null);
            setSuccess(status.message);
        } catch (disableError) {
            setError(disableError.message);
        }
    };

    if (loading) {
        return <div className="auth-screen"><div className="auth-card"><h1 className="auth-title">Loading dashboard</h1><p className="auth-status-line">Fetching your account...</p></div></div>;
    }

    if (error && !user) {
        return <div className="auth-screen"><div className="auth-card"><h1 className="auth-title">Session unavailable</h1><div className="auth-alert auth-alert--error" role="alert">{error}</div><button className="auth-submit" onClick={() => navigate("/login", { replace: true })}>Return to login</button></div></div>;
    }

    return (
        <div className="auth-screen dashboard-screen">
            <main className="dashboard-shell">
                <header className="dashboard-header">
                    <div>
                        <p className="auth-eyebrow">ShortLink dashboard</p>
                        <h1 className="auth-title">Welcome, {user?.name}</h1>
                        <p className="auth-subtitle dashboard-subtitle">{user?.email} · {user?.role} · {user?.status}</p>
                    </div>
                    <button type="button" className="dashboard-secondary-button" onClick={handleLogout}>Sign out</button>
                </header>

                <section className="dashboard-grid">
                    <div>
                        <form className="url-panel" onSubmit={handleCreate}>
                            <h2 className="dashboard-section-title">Create short URL</h2>
                            <div className="auth-field"><label className="auth-label" htmlFor="longUrl">Long URL</label><input id="longUrl" type="url" className="auth-input" value={longUrl} onChange={(event) => setLongUrl(event.target.value)} placeholder="https://example.com/very/long/link" disabled={saving} /></div>
                            <div className="dashboard-form-row">
                                <div className="auth-field"><label className="auth-label" htmlFor="customAlias">Custom alias</label><input id="customAlias" type="text" className="auth-input" value={customAlias} onChange={(event) => setCustomAlias(event.target.value)} placeholder="launch" disabled={saving} /></div>
                                <div className="auth-field"><label className="auth-label" htmlFor="expiresAt">Expiry</label><input id="expiresAt" type="datetime-local" className="auth-input" value={expiresAt} onChange={(event) => setExpiresAt(event.target.value)} disabled={saving} /></div>
                            </div>
                            <button type="submit" className="auth-submit dashboard-submit" disabled={saving}>{saving ? "Creating..." : "Create link"}</button>
                        </form>

                        <section className="url-panel security-panel">
                            <h2 className="dashboard-section-title">Two-factor authentication</h2>
                            <p className="url-item__meta">{user?.totpEnabled ? "Enabled" : "Disabled"}</p>
                            {totpSetup && (
                                <div className="totp-secret-box">
                                    <img
                                        className="totp-qr-code"
                                        src={totpSetup.qrCodeDataUri}
                                        alt="Two-factor authentication QR code"
                                    />
                                    <p className="url-item__long">Secret: {totpSetup.secret}</p>
                                </div>
                            )}
                            <div className="auth-field"><label className="auth-label" htmlFor="dashboardTotpCode">2FA code</label><input id="dashboardTotpCode" type="text" inputMode="numeric" className="auth-input" value={totpCode} onChange={(event) => setTotpCode(event.target.value)} maxLength={6} /></div>
                            <div className="totp-actions">
                                {!user?.totpEnabled && <button type="button" className="dashboard-secondary-button" onClick={handleSetupTotp}>Set up</button>}
                                {!user?.totpEnabled && totpSetup && <button type="button" className="dashboard-secondary-button" onClick={handleEnableTotp}>Enable</button>}
                                {user?.totpEnabled && <button type="button" className="dashboard-danger-button" onClick={handleDisableTotp}>Disable</button>}
                            </div>
                        </section>

                        {error && <div className="auth-alert auth-alert--error" role="alert">{error}</div>}
                        {success && <div className="auth-alert auth-alert--success" role="status">{success}</div>}
                    </div>

                    <section className="url-panel url-panel--list">
                        <div className="url-list-header"><h2 className="dashboard-section-title">Your links</h2><button type="button" className="dashboard-secondary-button" onClick={reloadLinks}>Refresh</button></div>
                        {links.length === 0 ? <p className="dashboard-empty">No short URLs yet.</p> : <div className="url-list">{links.map((link) => <article className="url-item" key={link.id}><div className="url-item__main"><a className="url-item__short" href={link.shortUrl} target="_blank" rel="noreferrer">{link.shortUrl}</a><p className="url-item__long">{link.longUrl}</p><p className="url-item__meta">{link.enabled ? "Enabled" : "Disabled"}{link.expiresAt ? ` · Expires ${new Date(link.expiresAt).toLocaleString()}` : " · No expiry"}</p></div><div className="url-item__actions"><button type="button" className="dashboard-secondary-button" onClick={() => handleCopy(link.shortUrl)}>Copy</button><button type="button" className="dashboard-secondary-button" onClick={() => handleToggleEnabled(link)}>{link.enabled ? "Disable" : "Enable"}</button><button type="button" className="dashboard-danger-button" onClick={() => handleDelete(link.id)}>Delete</button></div></article>)}</div>}
                    </section>
                </section>
            </main>
        </div>
    );
}