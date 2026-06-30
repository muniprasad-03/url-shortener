import React, { useState, useEffect } from 'react';
import { 
  Link2, 
  Copy, 
  Check, 
  Trash2, 
  BarChart3, 
  QrCode, 
  Calendar, 
  Plus, 
  ExternalLink, 
  Info,
  ChevronLeft,
  ChevronRight,
  Loader2,
  X,
  Sparkles,
  HelpCircle,
  AlertTriangle
} from 'lucide-react';
import './App.css';

const API_BASE = import.meta.env.VITE_API_BASE || (window.location.hostname === 'localhost' ? 'http://localhost:8080' : window.location.origin);

function App() {
  // Form State
  const [originalUrl, setOriginalUrl] = useState('');
  const [customAlias, setCustomAlias] = useState('');
  const [expiresAt, setExpiresAt] = useState('');
  
  // App UX States
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [history, setHistory] = useState([]);
  const [copiedUrl, setCopiedUrl] = useState(null);
  
  // Pagination State
  const [page, setPage] = useState(0);
  const [size] = useState(6);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  
  // Analytics Modal State
  const [selectedAnalytics, setSelectedAnalytics] = useState(null);
  const [analyticsLoading, setAnalyticsLoading] = useState(false);
  
  // Toasts State
  const [toasts, setToasts] = useState([]);

  // Fetch History on mount & page change
  useEffect(() => {
    fetchHistory();
  }, [page]);

  // Toast Helper
  const addToast = (type, message) => {
    const id = Date.now();
    setToasts(prev => [...prev, { id, type, message }]);
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
    }, 4000);
  };

  const removeToast = (id) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  };

  // API Call: Get History List
  const fetchHistory = async () => {
    try {
      const res = await fetch(`${API_BASE}/api/v1/urls?page=${page}&size=${size}`);
      const body = await res.json();
      if (body.success) {
        setHistory(body.data.content);
        setTotalPages(body.data.totalPages);
        setTotalElements(body.data.totalElements);
      }
    } catch (err) {
      console.error('Error fetching history:', err);
    }
  };

  // API Call: Shorten URL
  const handleShorten = async (e) => {
    e.preventDefault();
    if (!originalUrl) return;

    setIsLoading(true);
    setResult(null);

    const payload = {
      originalUrl,
      customAlias: customAlias.trim() || null,
      expiresAt: expiresAt ? new Date(expiresAt).toISOString() : null
    };

    try {
      const res = await fetch(`${API_BASE}/api/v1/urls`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
      });

      const body = await res.json();

      if (res.status === 201 && body.success) {
        setResult(body.data);
        addToast('success', 'URL shortened successfully!');
        setOriginalUrl('');
        setCustomAlias('');
        setExpiresAt('');
        fetchHistory(); // Refresh history
      } else if (res.status === 429) {
        addToast('error', 'Rate limit exceeded. Try again in a minute.');
      } else {
        addToast('error', body.message || 'Failed to shorten URL');
      }
    } catch (err) {
      addToast('error', 'Unable to connect to the server.');
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  // API Call: Delete URL
  const handleDelete = async (shortCode) => {
    if (!window.confirm(`Are you sure you want to delete /${shortCode}?`)) return;

    try {
      const res = await fetch(`${API_BASE}/api/v1/urls/${shortCode}`, {
        method: 'DELETE'
      });
      const body = await res.json();
      if (body.success) {
        addToast('success', `Short URL /${shortCode} deleted.`);
        // If current page becomes empty, jump back
        if (history.length === 1 && page > 0) {
          setPage(page - 1);
        } else {
          fetchHistory();
        }
      } else {
        addToast('error', body.message || 'Failed to delete URL');
      }
    } catch (err) {
      addToast('error', 'Failed to contact server.');
      console.error(err);
    }
  };

  // API Call: Fetch Analytics Details
  const handleViewAnalytics = async (shortCode) => {
    setAnalyticsLoading(true);
    try {
      const res = await fetch(`${API_BASE}/api/v1/urls/${shortCode}/analytics`);
      const body = await res.json();
      if (body.success) {
        setSelectedAnalytics(body.data);
      } else {
        addToast('error', body.message || 'Failed to load analytics');
      }
    } catch (err) {
      addToast('error', 'Server error loading analytics');
      console.error(err);
    } finally {
      setAnalyticsLoading(false);
    }
  };

  // Clipboard Copier
  const copyToClipboard = (url) => {
    navigator.clipboard.writeText(url);
    setCopiedUrl(url);
    addToast('success', 'Copied short link to clipboard!');
    setTimeout(() => setCopiedUrl(null), 2000);
  };

  // Date Formatting Helper
  const formatDateTime = (dateString) => {
    if (!dateString) return 'Never';
    const date = new Date(dateString);
    return date.toLocaleString(undefined, {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const isExpired = (expiresAt) => {
    if (!expiresAt) return false;
    return new Date(expiresAt) < new Date();
  };

  return (
    <div className="app-container">
      {/* Toast Notification Container */}
      <div className="toast-container">
        {toasts.map(toast => (
          <div key={toast.id} className={`toast toast-${toast.type} animate-fade-in`}>
            {toast.type === 'error' ? (
              <AlertTriangle className="toast-icon-error" size={20} />
            ) : (
              <Check className="toast-icon-success" size={20} />
            )}
            <div className="toast-message">{toast.message}</div>
            <button className="toast-close" onClick={() => removeToast(toast.id)}>
              <X size={16} />
            </button>
          </div>
        ))}
      </div>

      {/* Header */}
      <header className="header">
        <div className="brand">
          <Link2 className="brand-icon" size={28} />
          <h1 className="brand-title">URL Shortener</h1>
        </div>
        <a 
          href={`${API_BASE}/swagger-ui/index.html`} 
          target="_blank" 
          rel="noopener noreferrer" 
          className="api-docs-link"
        >
          <Sparkles size={16} />
          <span>API docs</span>
        </a>
      </header>

      {/* Hero Intro */}
      <section className="hero">
        <h1>Shorten. Share. <span>Track.</span></h1>
        <p>A production-ready, ultra-fast URL shortening service. Set expiration timelines, custom aliases, and generate downloadable QR codes instantly.</p>
      </section>

      {/* Main Dashboard Workspace */}
      <div className="dashboard-grid">
        
        {/* Left Side: Creation Form & History list */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2.5rem' }}>
          
          {/* URL Creation Form */}
          <div className="panel animate-fade-in">
            <h2 className="panel-title">
              <Plus size={20} className="brand-icon" />
              <span>Create Short Link</span>
            </h2>
            
            <form onSubmit={handleShorten} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              <div className="form-group">
                <label htmlFor="long-url">Destination URL</label>
                <div className="input-container">
                  <Link2 className="input-icon" size={18} />
                  <input 
                    id="long-url"
                    className="form-input"
                    type="url" 
                    placeholder="https://example.com/very-long-link-destination" 
                    value={originalUrl}
                    onChange={(e) => setOriginalUrl(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="custom-alias">Custom Alias (Optional)</label>
                  <div className="input-container">
                    <span className="input-icon" style={{ fontSize: '0.9rem', fontWeight: 600 }}>/</span>
                    <input 
                      id="custom-alias"
                      className="form-input"
                      type="text" 
                      placeholder="my-alias" 
                      value={customAlias}
                      onChange={(e) => setCustomAlias(e.target.value)}
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label htmlFor="expiration">Expiration Date (Optional)</label>
                  <div className="input-container">
                    <Calendar className="input-icon" size={18} />
                    <input 
                      id="expiration"
                      className="form-input"
                      type="datetime-local" 
                      value={expiresAt}
                      onChange={(e) => setExpiresAt(e.target.value)}
                    />
                  </div>
                </div>
              </div>

              <button type="submit" className="btn-primary" disabled={isLoading}>
                {isLoading ? (
                  <>
                    <Loader2 size={18} className="spinner" />
                    <span>Processing...</span>
                  </>
                ) : (
                  <>
                    <span>Generate Short Link</span>
                  </>
                )}
              </button>
            </form>
          </div>

          {/* History / Recent Links */}
          <div className="panel animate-fade-in" style={{ animationDelay: '0.1s' }}>
            <div className="history-section">
              <h2 className="panel-title">
                <Info size={20} className="brand-icon" />
                <span>Recent Mappings</span>
              </h2>

              {history.length === 0 ? (
                <div className="empty-state">
                  <HelpCircle size={40} className="empty-state-icon" />
                  <p>No shortened URLs yet. Shorten a URL to see it here.</p>
                </div>
              ) : (
                <>
                  <div className="table-wrapper">
                    <table className="history-table">
                      <thead>
                        <tr>
                          <th>Short Code</th>
                          <th>Original Destination</th>
                          <th>Clicks</th>
                          <th>Status</th>
                          <th>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {history.map((url) => {
                          const expired = isExpired(url.expiresAt);
                          return (
                            <tr key={url.shortCode}>
                              <td>
                                <a 
                                  href={`${API_BASE}/${url.shortCode}`} 
                                  target="_blank" 
                                  rel="noopener noreferrer" 
                                  className="short-url-cell"
                                >
                                  /{url.shortCode}
                                  <ExternalLink size={12} />
                                </a>
                              </td>
                              <td>
                                <div className="long-url-cell" title={url.originalUrl}>
                                  {url.originalUrl}
                                </div>
                              </td>
                              <td>
                                <span className="badge badge-clicks">
                                  {url.clickCount} clicks
                                </span>
                              </td>
                              <td>
                                {expired ? (
                                  <span className="badge badge-expired">Expired</span>
                                ) : (
                                  <span className="badge badge-badge-active badge-active">Active</span>
                                )}
                              </td>
                              <td>
                                <div className="actions-cell">
                                  <button 
                                    className="btn-secondary" 
                                    style={{ padding: '0.35rem 0.6rem' }}
                                    title="Copy"
                                    onClick={() => copyToClipboard(`${API_BASE}/${url.shortCode}`)}
                                  >
                                    <Copy size={14} />
                                  </button>
                                  <button 
                                    className="btn-secondary" 
                                    style={{ padding: '0.35rem 0.6rem' }}
                                    title="Analytics"
                                    onClick={() => handleViewAnalytics(url.shortCode)}
                                  >
                                    <BarChart3 size={14} />
                                  </button>
                                  <button 
                                    className="btn-danger" 
                                    title="Delete"
                                    onClick={() => handleDelete(url.shortCode)}
                                  >
                                    <Trash2 size={14} />
                                  </button>
                                </div>
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>

                  {/* Pagination Footer */}
                  {totalPages > 1 && (
                    <div className="pagination">
                      <div className="pagination-info">
                        Showing page {page + 1} of {totalPages} ({totalElements} links total)
                      </div>
                      <div className="pagination-controls">
                        <button 
                          className="pagination-btn"
                          disabled={page === 0} 
                          onClick={() => setPage(page - 1)}
                        >
                          <ChevronLeft size={16} />
                        </button>
                        <button 
                          className="pagination-btn"
                          disabled={page + 1 >= totalPages} 
                          onClick={() => setPage(page + 1)}
                        >
                          <ChevronRight size={16} />
                        </button>
                      </div>
                    </div>
                  )}
                </>
              )}
            </div>
          </div>

        </div>

        {/* Right Side: QR Code Generator & Result View */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2.5rem' }}>
          
          <div className="panel animate-fade-in" style={{ animationDelay: '0.15s' }}>
            <h2 className="panel-title">
              <QrCode className="brand-icon" size={20} />
              <span>Interactive Output</span>
            </h2>

            {result ? (
              <div className="result-card animate-fade-in">
                <div className="result-header">
                  <Check size={18} />
                  <span>Success! Your short URL is ready:</span>
                </div>
                
                <div className="result-url-display">
                  <span className="short-url-text">
                    {result.shortUrl}
                  </span>
                  <button 
                    className={`copy-btn ${copiedUrl === result.shortUrl ? 'copied' : ''}`}
                    onClick={() => copyToClipboard(result.shortUrl)}
                  >
                    {copiedUrl === result.shortUrl ? (
                      <>
                        <Check size={14} />
                        <span>Copied</span>
                      </>
                    ) : (
                      <>
                        <Copy size={14} />
                        <span>Copy</span>
                      </>
                    )}
                  </button>
                </div>

                <div className="result-details">
                  <div className="result-detail-item">
                    <span>Original URL:</span>
                    <span className="long-url-cell" title={result.originalUrl} style={{ maxWidth: '180px' }}>
                      {result.originalUrl}
                    </span>
                  </div>
                  <div className="result-detail-item">
                    <span>Expiration Date:</span>
                    <span>{formatDateTime(result.expiresAt)}</span>
                  </div>
                </div>

                {result.qrCodeBase64 && (
                  <div className="qr-code-section">
                    <div className="qr-image-container">
                      <img src={result.qrCodeBase64} alt="Short URL QR Code" />
                    </div>
                    <a 
                      href={result.qrCodeBase64} 
                      download={`qrcode-${result.shortCode}.png`}
                      className="btn-secondary"
                    >
                      <QrCode size={14} />
                      Download QR Code (PNG)
                    </a>
                  </div>
                )}
              </div>
            ) : (
              <div className="empty-state" style={{ padding: '6rem 1rem' }}>
                <Link2 size={36} className="empty-state-icon" style={{ color: 'var(--accent-cyan)' }} />
                <p style={{ maxWidth: '280px' }}>Once you create a short URL, the QR Code and output configurations will be populated here.</p>
              </div>
            )}
          </div>

        </div>

      </div>

      {/* Analytics Modal Dialog */}
      {selectedAnalytics && (
        <div className="modal-overlay" onClick={() => setSelectedAnalytics(null)}>
          <div className="modal-content animate-fade-in" onClick={(e) => e.stopPropagation()}>
            <button className="modal-close" onClick={() => setSelectedAnalytics(null)}>
              <X size={18} />
            </button>
            <h3 className="modal-title">Link Analytics: /{selectedAnalytics.shortCode}</h3>
            
            <div className="modal-body">
              <div className="stats-grid">
                <div className="stat-item">
                  <div className="stat-label">Total Clicks</div>
                  <div className="stat-val stat-val-highlight">{selectedAnalytics.clickCount}</div>
                </div>
                <div className="stat-item" style={{ flexDirection: 'column', alignItems: 'flex-start', gap: '0.35rem' }}>
                  <div className="stat-label">Destination URL</div>
                  <div className="stat-val" style={{ wordBreak: 'break-all', fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                    {selectedAnalytics.originalUrl}
                  </div>
                </div>
                <div className="stat-item">
                  <div className="stat-label">Created Date</div>
                  <div className="stat-val">{formatDateTime(selectedAnalytics.createdAt)}</div>
                </div>
                <div className="stat-item">
                  <div className="stat-label">Last Accessed</div>
                  <div className="stat-val">{formatDateTime(selectedAnalytics.lastAccessedAt)}</div>
                </div>
                <div className="stat-item">
                  <div className="stat-label">Expiration Date</div>
                  <div className="stat-val">{formatDateTime(selectedAnalytics.expiresAt)}</div>
                </div>
              </div>
            </div>
            
            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <button className="btn-secondary" onClick={() => setSelectedAnalytics(null)}>Close</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;
