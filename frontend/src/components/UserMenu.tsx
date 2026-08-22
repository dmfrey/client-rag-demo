import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { DropdownMenu, DropdownMenuItem } from "./DropdownMenu";

export function UserMenu() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate("/login", { replace: true });
  }

  return (
    <DropdownMenu
      align="left"
      trigger={({ onClick }) => (
        <button
          type="button"
          onClick={onClick}
          className="flex w-full items-center justify-between rounded-md px-2 py-1.5 text-sm text-gray-700 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-800"
        >
          <span className="truncate">{user?.username}</span>
          <span className="text-gray-400">⋯</span>
        </button>
      )}
    >
      <DropdownMenuItem onClick={() => navigate("/settings")}>⚙️ Settings</DropdownMenuItem>
      <DropdownMenuItem onClick={() => void handleLogout()}>Log out</DropdownMenuItem>
    </DropdownMenu>
  );
}
