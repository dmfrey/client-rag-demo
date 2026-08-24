import { api } from "./client";

export interface Banner {
  text: string;
  acknowledged: boolean;
}

export const bannerApi = {
  get: () => api.get<Banner>("/api/banner"),
  acknowledge: () => api.post<void>("/api/banner/ack"),
};
