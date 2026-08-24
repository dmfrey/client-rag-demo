import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";
import "./index.css";
import App from "./App.tsx";
import { AuthProvider } from "./auth/AuthContext.tsx";
import { BannerGate } from "./banner/BannerGate.tsx";

const queryClient = new QueryClient();

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <BannerGate>
          <AuthProvider>
            <App />
          </AuthProvider>
        </BannerGate>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
);
