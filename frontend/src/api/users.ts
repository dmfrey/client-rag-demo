import { api } from "./client";
import type { User } from "./types";

export interface UpdateProfileInput {
  firstName: string;
  lastName: string;
  email: string;
}

export interface ChangePasswordInput {
  currentPassword: string;
  newPassword: string;
}

export const usersApi = {
  updateProfile: (input: UpdateProfileInput) => api.patch<User>("/api/users/me", input),
  changePassword: (input: ChangePasswordInput) => api.put<void>("/api/users/me/password", input),
};
