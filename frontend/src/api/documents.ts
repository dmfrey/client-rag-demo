import { api } from "./client";
import type { Document } from "./types";

export const documentsApi = {
  list: () => api.get<Document[]>("/api/documents"),
  get: (id: number) => api.get<Document>(`/api/documents/${id}`),
  upload: (file: File) => {
    const form = new FormData();
    form.append("file", file);
    return api.postForm<Document>("/api/documents", form);
  },
  remove: (id: number) => api.delete<void>(`/api/documents/${id}`),
};
