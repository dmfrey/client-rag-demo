import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { DocumentsPage } from "./DocumentsPage";
import { documentsApi } from "../api/documents";
import type { Document } from "../api/types";

vi.mock("../api/documents", () => ({
  documentsApi: {
    list: vi.fn(),
    upload: vi.fn(),
    remove: vi.fn(),
    get: vi.fn(),
  },
}));

const existingDoc: Document = {
  id: 1,
  filename: "report.pdf",
  contentType: "PDF",
  status: "READY",
  errorMessage: null,
  chunkCount: 4,
  uploadedBy: "alice",
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
};

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <DocumentsPage />
    </QueryClientProvider>,
  );
}

function makeFile(name: string): File {
  return new File(["content"], name, { type: "text/plain" });
}

function fileInput(): HTMLInputElement {
  return document.querySelector('input[type="file"]') as HTMLInputElement;
}

beforeEach(() => {
  vi.mocked(documentsApi.list).mockResolvedValue([existingDoc]);
  vi.mocked(documentsApi.upload).mockResolvedValue({ ...existingDoc, status: "PROCESSING" });
  vi.mocked(documentsApi.remove).mockResolvedValue(undefined);
});

describe("DocumentsPage upload confirmation", () => {
  it("uploads a new file directly, with no confirmation, when the filename doesn't collide", async () => {
    renderPage();
    await screen.findByText("report.pdf");

    await userEvent.upload(fileInput(), makeFile("new-file.txt"));

    await waitFor(() => expect(documentsApi.upload).toHaveBeenCalledTimes(1));
    expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
  });

  it("asks for confirmation before replacing a matching filename, and only uploads on confirm", async () => {
    renderPage();
    await screen.findByText("report.pdf");

    await userEvent.upload(fileInput(), makeFile("report.pdf"));

    const dialog = await screen.findByRole("alertdialog");
    expect(dialog).toHaveTextContent('This will replace the existing "report.pdf"');
    expect(documentsApi.upload).not.toHaveBeenCalled();

    await userEvent.click(within(dialog).getByRole("button", { name: "Replace" }));

    await waitFor(() => expect(documentsApi.upload).toHaveBeenCalledTimes(1));
    expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
  });

  it("does not upload when the replace confirmation is cancelled", async () => {
    renderPage();
    await screen.findByText("report.pdf");

    await userEvent.upload(fileInput(), makeFile("report.pdf"));

    const dialog = await screen.findByRole("alertdialog");
    await userEvent.click(within(dialog).getByRole("button", { name: "Cancel" }));

    expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
    expect(documentsApi.upload).not.toHaveBeenCalled();
  });

  it("rejects an unsupported file extension before ever showing a confirmation", async () => {
    renderPage();
    await screen.findByText("report.pdf");

    // Via the file input, the browser's own accept=".pdf,.docx,.txt" filtering (which
    // userEvent.upload respects) would stop this before it ever reaches handleFiles - drag-and-drop
    // has no such restriction, so that's the real path an unsupported file can take.
    fireEvent.drop(screen.getByTestId("dropzone"), {
      dataTransfer: { files: [makeFile("malware.exe")] },
    });

    expect(await screen.findByText(/only pdf, word/i)).toBeInTheDocument();
    expect(documentsApi.upload).not.toHaveBeenCalled();
    expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
  });
});

describe("DocumentsPage delete confirmation", () => {
  it("asks for confirmation before deleting, and only deletes on confirm", async () => {
    renderPage();
    await screen.findByText("report.pdf");

    await userEvent.click(screen.getByRole("button", { name: "Delete" }));

    const dialog = await screen.findByRole("alertdialog");
    expect(dialog).toHaveTextContent('Delete "report.pdf"?');
    expect(documentsApi.remove).not.toHaveBeenCalled();

    await userEvent.click(within(dialog).getByRole("button", { name: "Delete" }));

    await waitFor(() => expect(documentsApi.remove).toHaveBeenCalledWith(existingDoc.id));
  });

  it("does not delete when the confirmation is cancelled", async () => {
    renderPage();
    await screen.findByText("report.pdf");

    await userEvent.click(screen.getByRole("button", { name: "Delete" }));
    const dialog = await screen.findByRole("alertdialog");
    await userEvent.click(within(dialog).getByRole("button", { name: "Cancel" }));

    expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
    expect(documentsApi.remove).not.toHaveBeenCalled();
  });
});
