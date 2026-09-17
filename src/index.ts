import { registerPlugin } from '@capacitor/core';

import type { SSOWebViewPlugin } from './definitions';

const SSOWebView = registerPlugin<SSOWebViewPlugin>('SSOWebView', {
  web: () => import('./web').then((m) => new m.SSOWebViewWeb()),
});

export * from './definitions';
export { SSOWebView };
