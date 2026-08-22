import { useState, type KeyboardEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { NavLink, useLocation, useNavigate } from "react-router-dom";
import { chatApi } from "../api/chat";
import { ConfirmDialog } from "./ConfirmDialog";
import { DropdownMenu, DropdownMenuItem } from "./DropdownMenu";
import { UserMenu } from "./UserMenu";
import type { ChatSession } from "../api/types";

export function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();

  const sessionsQuery = useQuery({ queryKey: ["chats"], queryFn: () => chatApi.list(false) });

  const [renamingId, setRenamingId] = useState<number | null>(null);
  const [renameValue, setRenameValue] = useState("");
  const [pendingDelete, setPendingDelete] = useState<ChatSession | null>(null);

  const createChatMutation = useMutation({
    mutationFn: chatApi.create,
    onSuccess: (session) => {
      queryClient.invalidateQueries({ queryKey: ["chats"] });
      navigate(`/chat/${session.id}`);
    },
  });

  const renameMutation = useMutation({
    mutationFn: ({ id, title }: { id: number; title: string }) => chatApi.rename(id, title),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["chats"] });
      setRenamingId(null);
    },
  });

  const archiveMutation = useMutation({
    mutationFn: (id: number) => chatApi.archive(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["chats"] }),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => chatApi.remove(id),
    onSuccess: (_data, id) => {
      queryClient.invalidateQueries({ queryKey: ["chats"] });
      if (location.pathname === `/chat/${id}`) {
        navigate("/", { replace: true });
      }
    },
  });

  function startRename(session: ChatSession) {
    setRenamingId(session.id);
    setRenameValue(session.title ?? "");
  }

  function commitRename(id: number) {
    const title = renameValue.trim();
    if (!title) {
      setRenamingId(null);
      return;
    }
    renameMutation.mutate({ id, title });
  }

  function handleRenameKeyDown(event: KeyboardEvent<HTMLInputElement>, id: number) {
    if (event.key === "Enter") {
      event.preventDefault();
      commitRename(id);
    } else if (event.key === "Escape") {
      event.preventDefault();
      setRenamingId(null);
    }
  }

  const linkClasses = ({ isActive }: { isActive: boolean }) =>
    `block rounded-md px-3 py-2 text-sm truncate ${
      isActive
        ? "bg-blue-50 font-medium text-blue-700 dark:bg-blue-900/40 dark:text-blue-300"
        : "text-gray-700 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-800"
    }`;

  return (
    <aside className="flex h-full w-64 flex-none flex-col border-r border-gray-200 bg-gray-50 dark:border-gray-800 dark:bg-gray-950">
      <div className="p-3">
        <button
          type="button"
          onClick={() => createChatMutation.mutate()}
          disabled={createChatMutation.isPending}
          className="w-full rounded-md bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-60"
        >
          + New chat
        </button>
      </div>

      <nav className="flex-1 overflow-y-auto px-3">
        <ul className="flex flex-col gap-0.5">
          {sessionsQuery.data?.map((session) =>
            renamingId === session.id ? (
              <li key={session.id} className="px-3 py-2">
                <input
                  autoFocus
                  value={renameValue}
                  onChange={(event) => setRenameValue(event.target.value)}
                  onKeyDown={(event) => handleRenameKeyDown(event, session.id)}
                  onBlur={() => commitRename(session.id)}
                  onFocus={(event) => event.target.select()}
                  className="w-full rounded border border-blue-400 bg-white px-1.5 py-0.5 text-sm text-gray-900 focus:outline-none dark:bg-gray-900 dark:text-gray-100"
                />
              </li>
            ) : (
              <li key={session.id} className="group flex items-center">
                <NavLink to={`/chat/${session.id}`} className={`${linkClasses} min-w-0 flex-1`}>
                  {session.title ?? "New chat"}
                </NavLink>
                <DropdownMenu
                  trigger={({ onClick }) => (
                    <button
                      type="button"
                      onClick={onClick}
                      aria-label={`Options for ${session.title ?? "New chat"}`}
                      className="rounded px-1.5 py-1 text-gray-400 opacity-0 group-hover:opacity-100 hover:bg-gray-200 focus:opacity-100 dark:hover:bg-gray-800"
                    >
                      ⋯
                    </button>
                  )}
                >
                  <DropdownMenuItem onClick={() => startRename(session)}>Rename</DropdownMenuItem>
                  <DropdownMenuItem onClick={() => archiveMutation.mutate(session.id)}>Archive</DropdownMenuItem>
                  <DropdownMenuItem danger onClick={() => setPendingDelete(session)}>
                    Delete
                  </DropdownMenuItem>
                </DropdownMenu>
              </li>
            ),
          )}
        </ul>
      </nav>

      <div className="border-t border-gray-200 p-3 dark:border-gray-800">
        <NavLink to="/documents" className={linkClasses}>
          📄 Documents
        </NavLink>
      </div>

      <div className="border-t border-gray-200 p-2 dark:border-gray-800">
        <UserMenu />
      </div>

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
    </aside>
  );
}
