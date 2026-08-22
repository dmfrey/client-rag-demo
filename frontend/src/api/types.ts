export interface User {
  id: number;
  username: string;
  firstName: string | null;
  lastName: string | null;
  email: string | null;
}

export type DocumentContentType = "PDF" | "DOCX" | "TXT";
export type DocumentStatus = "PROCESSING" | "READY" | "FAILED";

export interface Document {
  id: number;
  filename: string;
  contentType: DocumentContentType;
  status: DocumentStatus;
  errorMessage: string | null;
  chunkCount: number | null;
  uploadedBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface ChatSession {
  id: number;
  title: string | null;
  archived: boolean;
  createdAt: string;
  updatedAt: string;
}

export type ChatRole = "USER" | "ASSISTANT";

export interface Citation {
  documentId: number | null;
  filename: string;
}

export interface ChatMessage {
  role: ChatRole;
  content: string;
  citations: Citation[];
}
