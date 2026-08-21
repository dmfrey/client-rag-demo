/// <reference types="vite/client" />

interface ImportMetaEnv {
  // Absolute backend origin baked in at build time for deployments where the frontend and
  // backend are separate origins/routes (e.g. Cloud Foundry - see manifest.yml). Unset (the
  // local dev default), api calls stay relative (/api/...) and go through Vite's dev proxy to
  // the same origin instead.
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
