import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ArchivePage } from "./ArchivePage";
import { chatApi } from "../api/chat";
import type { ChatSession } from "../api/types";

vi.mock("../api/chat", () => ({
  chatApi: {
    list: vi.fn(),
    unarchive: vi.fn(),
    remove: vi.fn(),
  },
}));

const archivedSession: ChatSession = {
  id: 2,
  title: "Old chat",
  archived: true,
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-02T00:00:00Z",
};

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ArchivePage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.mocked(chatApi.list).mockResolvedValue([archivedSession]);
  vi.mocked(chatApi.unarchive).mockResolvedValue({ ...archivedSession, archived: false });
  vi.mocked(chatApi.remove).mockResolvedValue(undefined);
});

describe("ArchivePage", () => {
  it("lists archived sessions via the archived filter", async () => {
    renderPage();
    await screen.findByText("Old chat");
    expect(chatApi.list).toHaveBeenCalledWith(true);
  });

  it("unarchives a session", async () => {
    renderPage();
    await screen.findByText("Old chat");

    await userEvent.click(screen.getByRole("button", { name: "Unarchive" }));

    await waitFor(() => expect(chatApi.unarchive).toHaveBeenCalledWith(2));
  });

  it("asks for confirmation before deleting, and only deletes on confirm", async () => {
    renderPage();
    await screen.findByText("Old chat");

    await userEvent.click(screen.getByRole("button", { name: "Delete" }));
    const dialog = await screen.findByRole("alertdialog");
    expect(chatApi.remove).not.toHaveBeenCalled();

    await userEvent.click(within(dialog).getByRole("button", { name: "Delete" }));

    await waitFor(() => expect(chatApi.remove).toHaveBeenCalledWith(2));
  });

  it("shows an empty state with no archived chats", async () => {
    vi.mocked(chatApi.list).mockResolvedValue([]);
    renderPage();
    expect(await screen.findByText("No archived chats.")).toBeInTheDocument();
  });
});
