import { WebPlugin } from '@capacitor/core';

import type { SSOOpenOptions, SSOWebViewPlugin } from './definitions';

export class SSOWebViewWeb extends WebPlugin implements SSOWebViewPlugin {
  private popup: Window | null = null;
  private pollTimer: any = null;

  async openSSO(options: SSOOpenOptions): Promise<void> {
    this.popup = window.open(
      options.url,
      'ssoLogin',
      'width=480,height=680,toolbar=1,location=1,scrollbars=1,resizable=1',
    );

    if (!this.popup) {
      throw new Error('Popup blocked by browser');
    }

    // Web: polling terbatas oleh CORS, hanya untuk same-origin redirect
    this.pollTimer = setInterval(() => {
      try {
        if (!this.popup || this.popup.closed) {
          clearInterval(this.pollTimer);
          this.notifyListeners('ssoCancelled', {});
          return;
        }
        const href = this.popup.location.href;
        if (href.includes(options.redirectPathContains)) {
          clearInterval(this.pollTimer);
          const u = new URL(href);
          this.notifyListeners('ssoRedirect', {
            url: href,
            code: u.searchParams.get('code') ?? '',
            state: u.searchParams.get('state') ?? '',
          });
          this.popup.close();
        }
      } catch {
        // cross-origin — normal selama masih di domain SSO
      }
    }, 300);
  }

  async close(): Promise<void> {
    if (this.pollTimer) clearInterval(this.pollTimer);
    if (this.popup && !this.popup.closed) this.popup.close();
    this.popup = null;
  }
}
