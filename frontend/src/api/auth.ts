import { api } from "./client";
import type { User } from "./types";

export interface RegisterInput {
  username: string;
  password: string;
  firstName: string;
  lastName: string;
  email: string;
}

export const authApi = {
  register: (input: RegisterInput) => api.post<User>("/api/auth/register", input),
  login: (username: string, password: string) => api.post<User>("/api/auth/login", { username, password }),
  logout: () => api.post<void>("/api/auth/logout"),
  me: () => api.get<User>("/api/auth/me"),
};
