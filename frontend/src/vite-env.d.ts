/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** SECURITY-09: development-only mock transport switch. Never set in a production build. */
  readonly VITE_USE_MOCK?: string;
  /** Base URL of the U1 planning API. Defaults to the loopback dev backend. */
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
