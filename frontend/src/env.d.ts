interface ImportMetaEnv {
  readonly PUBLIC_API_URL: string;
  readonly PUBLIC_SITE_URL: string;
  readonly PUBLIC_PLAUSIBLE_ENABLED?: string;
  readonly PUBLIC_PLAUSIBLE_DOMAIN?: string;
  readonly PUBLIC_PLAUSIBLE_SCRIPT_URL?: string;
  readonly ADSENSE_ENABLED?: string;
  readonly PUBLIC_ADSENSE_ENABLED?: string;
  readonly PUBLIC_ADSENSE_CLIENT_ID?: string;
  readonly PUBLIC_ADSENSE_SIDEBAR_SLOT?: string;
  readonly PUBLIC_ADSENSE_INLINE_SLOT?: string;
  readonly PUBLIC_ADSENSE_FOOTER_SLOT?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
