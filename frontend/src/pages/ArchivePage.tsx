import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { chatApi } from "../api/chat";
import { ConfirmDialog } from "../components/ConfirmDialog";
import type { ChatSession } from "../api/types";

export function ArchivePage() {
  const queryClient = useQueryClient();
  const [pendingDelete, setPendingDelete] = useState<ChatSession | null>(null);

  const sessionsQuery = useQuery({ queryKey: ["chats", "archived"], queryFn: () => chatApi.list(true) });

  const unarchiveMutation = useMutation({
    mutationFn: (id: number) => chatApi.unarchive(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["chats"] });
      queryClient.invalidateQueries({ queryKey: ["chats", "archived"] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => chatApi.remove(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["chats", "archived"] }),
  });

  const sessions = sessionsQuery.data ?? [];

  return (
    <div className="mx-auto flex h-full max-w-3xl flex-col gap-6 overflow-y-auto p-6">
      <h1 className="text-xl font-semibold text-gray-900 dark:text-gray-100">Archived chats</h1>

      <ul className="flex flex-col divide-y divide-gray-100 dark:divide-gray-900">
        {sessions.map((session) => (
          <li key={session.id} className="flex items-center justify-between gap-4 py-3">
            <div className="min-w-0">
              <Link
                to={`/chat/${session.id}`}
                className="block truncate text-sm font-medium text-gray-900 hover:underline dark:text-gray-100"
              >
                {session.title ?? "New chat"}
              </Link>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                Updated {new Date(session.updatedAt).toLocaleString()}
              </p>
            </div>
            <div className="flex flex-none items-center gap-3">
              <button
                type="button"
                onClick={() => unarchiveMutation.mutate(session.id)}
                className="text-sm font-medium text-blue-600 hover:underline dark:text-blue-400"
              >
                Unarchive
              </button>
              <button
                type="button"
                onClick={() => setPendingDelete(session)}
                className="text-sm font-medium text-red-600 hover:underline dark:text-red-400"
              >
                Delete
              </button>
            </div>
          </li>
        ))}
        {sessions.length === 0 && (
          <li className="py-8 text-center text-sm text-gray-500 dark:text-gray-400">No archived chats.</li>
        )}
      </ul>

      {pendingDelete && (
        <ConfirmDialog
          title="Delete chat?"
          message={`Delete "${pendingDelete.title ?? "New chat"}"? This can't be undone.`}
          confirmLabel="Delete"
          onConfirm={() => {
            deleteMutation.mutate(pendingDelete.id);
            setPendingDelete(null);
          }}
          onCancel={() => setPendingDelete(null)}
        />
      )}
    </div>
  );
}
