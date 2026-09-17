import type { PluginListenerHandle } from '@capacitor/core';

export interface SSORedirectEvent {
  /** Full redirect URL yang ter-intercept */
  url: string;
  /** Authorization code (belum dikonsumsi) */
  code: string;
  /** State parameter dari OAuth flow */
  state: string;
}

export interface SSOOpenOptions {
  /** URL SSO authorize endpoint */
  url: string;
  /** Host yang menandakan redirect, mis. "tdirectmbl.telkomsel.co.id" */
  redirectHost: string;
  /** Substring path redirect, mis. "mobilelogon" */
  redirectPathContains: string;
  /** Judul header WebView */
  title?: string;
}

export interface SSOWebViewPlugin {
  /**
   * Buka WebView SSO. Navigasi ke redirectHost akan di-intercept
   * dan DIBATALKAN sehingga authorization code tidak dikonsumsi.
   */
  openSSO(options: SSOOpenOptions): Promise<void>;

  /** Tutup WebView secara manual */
  close(): Promise<void>;

  /** Fired saat redirect ter-intercept — code masih valid */
  addListener(eventName: 'ssoRedirect', listenerFunc: (event: SSORedirectEvent) => void): Promise<PluginListenerHandle>;

  /** Fired saat user menutup WebView tanpa menyelesaikan login */
  addListener(eventName: 'ssoCancelled', listenerFunc: () => void): Promise<PluginListenerHandle>;

  removeAllListeners(): Promise<void>;
}
