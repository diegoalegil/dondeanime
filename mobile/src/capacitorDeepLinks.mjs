import { App } from '@capacitor/app';

import { createDeepLinkHandler } from './deepLinks.mjs';

export function registerCapacitorDeepLinks({ navigate }) {
  return App.addListener('appUrlOpen', createDeepLinkHandler({ navigate }));
}
