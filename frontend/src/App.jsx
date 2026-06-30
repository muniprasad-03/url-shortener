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
  const [activeTab, setActiveTab] = useState('shortener'); // 'shortener' or 'qr'
  const [copiedUrl, setCopiedUrl] = useState(null);
  
  // Toasts State
  const [toasts, setToasts] = useState([]);

  // Toast Helper
  const addToast = (type, message) => {
    const id = Date.now();
    setToasts(prev => [...prev, { id, type, message }]);
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
    }, 4000);
  };

  const removeToast = (id) => {
    setToasts(prev => setToasts(prev.filter(t => t.id !== id)));
  };

  // API Call: Shorten URL
  const handleShorten = async (e) => {
    e.preventDefault();
    if (!originalUrl) return;

    setIsLoading(true);
    setResult(null);

    const payload = {
      originalUrl,
      customAlias: activeTab === 'shortener' && customAlias.trim() ? customAlias.trim() : null,
      expiresAt: activeTab === 'shortener' && expiresAt ? new Date(expiresAt).toISOString() : null
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
        addToast('success', activeTab === 'shortener' ? 'URL shortened successfully!' : 'QR Code generated successfully!');
        setOriginalUrl('');
        setCustomAlias('');
        setExpiresAt('');
      } else if (res.status === 429) {
        addToast('error', 'Rate limit exceeded. Try again in a minute.');
      } else {
        addToast('error', body.message || 'Failed to process request');
      }
    } catch (err) {
      addToast('error', 'Unable to connect to the server.');
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  // Clipboard Copier
  const copyToClipboard = (url) => {
    navigator.clipboard.writeText(url);
    setCopiedUrl(url);
    addToast('success', 'Copied short link to clipboard!');
    setTimeout(() => setCopiedUrl(null), 2000);
  };

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    setResult(null);
    setOriginalUrl('');
    setCustomAlias('');
    setExpiresAt('');
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

      {/* Main Workspace (Centered Single Column) */}
      <main className="main-content-wrapper">
        {/* Tab Switcher */}
        <div className="tab-switcher">
          <button 
            className={`tab-btn ${activeTab === 'shortener' ? 'active' : ''}`}
            onClick={() => handleTabChange('shortener')}
          >
            <Link2 size={16} />
            <span>URL Shortener</span>
          </button>
          <button 
            className={`tab-btn ${activeTab === 'qr' ? 'active' : ''}`}
            onClick={() => handleTabChange('qr')}
          >
            <QrCode size={16} />
            <span>QR Code</span>
          </button>
        </div>

        {/* Content Panels */}
        {activeTab === 'shortener' ? (
          <div className="panel animate-fade-in">
            <h2 className="panel-title">
              <Link2 size={20} className="brand-icon" />
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
                    <span>Generating...</span>
                  </>
                ) : (
                  <span>Shorten URL</span>
                )}
              </button>
            </form>

            {/* Short URL Result */}
            {result && (
              <div className="result-card animate-fade-in" style={{ marginTop: '1.5rem' }}>
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
              </div>
            )}
          </div>
        ) : (
          <div className="panel animate-fade-in">
            <h2 className="panel-title">
              <QrCode size={20} className="brand-icon" />
              <span>Generate QR Code</span>
            </h2>
            
            <form onSubmit={handleShorten} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              <div className="form-group">
                <label htmlFor="qr-url">Target URL</label>
                <div className="input-container">
                  <Link2 className="input-icon" size={18} />
                  <input 
                    id="qr-url"
                    className="form-input"
                    type="url" 
                    placeholder="https://example.com/url-to-encode" 
                    value={originalUrl}
                    onChange={(e) => setOriginalUrl(e.target.value)}
                    required
                  />
                </div>
              </div>

              <button type="submit" className="btn-primary" disabled={isLoading}>
                {isLoading ? (
                  <>
                    <Loader2 size={18} className="spinner" />
                    <span>Generating...</span>
                  </>
                ) : (
                  <span>Generate QR Code</span>
                )}
              </button>
            </form>

            {/* QR Code Result */}
            {result && result.qrCodeBase64 && (
              <div className="result-card animate-fade-in" style={{ marginTop: '1.5rem' }}>
                <div className="qr-code-section">
                  <div className="qr-image-container">
                    <img src={result.qrCodeBase64} alt="Short URL QR Code" />
                  </div>
                  <a 
                    href={result.qrCodeBase64} 
                    download={`qrcode-${result.shortCode}.png`}
                    className="btn-secondary"
                    style={{ width: '100%', justifyContent: 'center' }}
                  >
                    <QrCode size={14} />
                    Download QR Code (PNG)
                  </a>
                </div>
              </div>
            )}
          </div>
        )}
      </main>
    </div>
  );
}

export default App;
