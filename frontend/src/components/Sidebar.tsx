import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { NavLink, useNavigate } from "react-router-dom";
import { chatApi } from "../api/chat";
import { useAuth } from "../auth/AuthContext";

export function Sidebar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const sessionsQuery = useQuery({ queryKey: ["chats"], queryFn: chatApi.list });

  const createChatMutation = useMutation({
    mutationFn: chatApi.create,
    onSuccess: (session) => {
      queryClient.invalidateQueries({ queryKey: ["chats"] });
      navigate(`/chat/${session.id}`);
    },
  });

  async function handleLogout() {
    await logout();
    navigate("/login", { replace: true });
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
          {sessionsQuery.data?.map((session) => (
            <li key={session.id}>
              <NavLink to={`/chat/${session.id}`} className={linkClasses}>
                {session.title ?? "New chat"}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      <div className="border-t border-gray-200 p-3 dark:border-gray-800">
        <NavLink to="/documents" className={linkClasses}>
          📄 Documents
        </NavLink>
      </div>

      <div className="flex items-center justify-between border-t border-gray-200 p-3 dark:border-gray-800">
        <span className="truncate text-sm text-gray-600 dark:text-gray-400">{user?.username}</span>
        <button
          type="button"
          onClick={handleLogout}
          className="text-sm font-medium text-gray-500 hover:text-gray-800 dark:text-gray-400 dark:hover:text-gray-100"
        >
          Log out
        </button>
      </div>
    </aside>
  );
}
