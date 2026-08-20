import { api } from "./client";
import type { User } from "./types";

export const authApi = {
  register: (username: string, password: string) => api.post<User>("/api/auth/register", { username, password }),
  login: (username: string, password: string) => api.post<User>("/api/auth/login", { username, password }),
  logout: () => api.post<void>("/api/auth/logout"),
  me: () => api.get<User>("/api/auth/me"),
};
