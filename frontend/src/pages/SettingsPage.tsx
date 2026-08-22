import { useState, type FormEvent } from "react";
import { useMutation } from "@tanstack/react-query";
import { useAuth } from "../auth/AuthContext";
import { usersApi } from "../api/users";
import { ApiError } from "../api/client";
import { ArchivedChatsSection } from "../components/ArchivedChatsSection";

type Tab = "profile" | "security" | "archived";

const TABS: { id: Tab; label: string }[] = [
  { id: "profile", label: "Profile" },
  { id: "security", label: "Security" },
  { id: "archived", label: "Archived Chats" },
];

export function SettingsPage() {
  const [activeTab, setActiveTab] = useState<Tab>("profile");

  return (
    <div className="mx-auto flex h-full max-w-2xl flex-col gap-6 overflow-y-auto p-6">
      <h1 className="text-xl font-semibold text-gray-900 dark:text-gray-100">Settings</h1>

      <div className="flex gap-1 border-b border-gray-200 dark:border-gray-800">
        {TABS.map((tab) => (
          <button
            key={tab.id}
            type="button"
            onClick={() => setActiveTab(tab.id)}
            className={`px-3 py-2 text-sm font-medium ${
              activeTab === tab.id
                ? "border-b-2 border-blue-600 text-blue-700 dark:text-blue-400"
                : "text-gray-500 hover:text-gray-800 dark:text-gray-400 dark:hover:text-gray-100"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === "profile" && <ProfileSection />}
      {activeTab === "security" && <SecuritySection />}
      {activeTab === "archived" && <ArchivedChatsSection />}
    </div>
  );
}

function ProfileSection() {
  const { user, refreshUser } = useAuth();
  const [firstName, setFirstName] = useState(user?.firstName ?? "");
  const [lastName, setLastName] = useState(user?.lastName ?? "");
  const [email, setEmail] = useState(user?.email ?? "");
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  const updateMutation = useMutation({
    mutationFn: (input: { firstName: string; lastName: string; email: string }) => usersApi.updateProfile(input),
    onSuccess: (updatedUser) => {
      refreshUser(updatedUser);
      setSaved(true);
      setError(null);
    },
    onError: (err) => {
      setSaved(false);
      setError(err instanceof ApiError ? err.message : "Something went wrong. Please try again.");
    },
  });

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSaved(false);
    updateMutation.mutate({ firstName, lastName, email });
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
      <div className="flex gap-3">
        <label className="flex flex-1 flex-col gap-1 text-sm text-gray-700 dark:text-gray-300">
          First name
          <input
            type="text"
            value={firstName}
            onChange={(event) => setFirstName(event.target.value)}
            className="rounded-md border border-gray-300 px-3 py-2 text-gray-900 focus:border-blue-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
          />
        </label>
        <label className="flex flex-1 flex-col gap-1 text-sm text-gray-700 dark:text-gray-300">
          Last name
          <input
            type="text"
            value={lastName}
            onChange={(event) => setLastName(event.target.value)}
            className="rounded-md border border-gray-300 px-3 py-2 text-gray-900 focus:border-blue-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
          />
        </label>
      </div>
      <label className="flex flex-col gap-1 text-sm text-gray-700 dark:text-gray-300">
        Email
        <input
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          className="rounded-md border border-gray-300 px-3 py-2 text-gray-900 focus:border-blue-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
        />
      </label>
      {error && <p className="text-sm text-red-600 dark:text-red-400">{error}</p>}
      {saved && !error && <p className="text-sm text-green-600 dark:text-green-400">Profile saved.</p>}
      <button
        type="submit"
        disabled={updateMutation.isPending}
        className="w-fit rounded-md bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-60"
      >
        {updateMutation.isPending ? "Saving…" : "Save profile"}
      </button>
    </form>
  );
}

function SecuritySection() {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [succeeded, setSucceeded] = useState(false);

  const changePasswordMutation = useMutation({
    mutationFn: (input: { currentPassword: string; newPassword: string }) => usersApi.changePassword(input),
    onSuccess: () => {
      setSucceeded(true);
      setError(null);
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
    },
    onError: (err) => {
      setSucceeded(false);
      setError(err instanceof ApiError ? err.message : "Something went wrong. Please try again.");
    },
  });

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSucceeded(false);
    if (newPassword !== confirmPassword) {
      setError("New passwords do not match.");
      return;
    }
    setError(null);
    changePasswordMutation.mutate({ currentPassword, newPassword });
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
      <label className="flex flex-col gap-1 text-sm text-gray-700 dark:text-gray-300">
        Current password
        <input
          type="password"
          required
          value={currentPassword}
          onChange={(event) => setCurrentPassword(event.target.value)}
          className="rounded-md border border-gray-300 px-3 py-2 text-gray-900 focus:border-blue-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
        />
      </label>
      <label className="flex flex-col gap-1 text-sm text-gray-700 dark:text-gray-300">
        New password
        <input
          type="password"
          required
          minLength={8}
          value={newPassword}
          onChange={(event) => setNewPassword(event.target.value)}
          className="rounded-md border border-gray-300 px-3 py-2 text-gray-900 focus:border-blue-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
        />
      </label>
      <label className="flex flex-col gap-1 text-sm text-gray-700 dark:text-gray-300">
        Confirm new password
        <input
          type="password"
          required
          minLength={8}
          value={confirmPassword}
          onChange={(event) => setConfirmPassword(event.target.value)}
          className="rounded-md border border-gray-300 px-3 py-2 text-gray-900 focus:border-blue-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
        />
      </label>
      {error && <p className="text-sm text-red-600 dark:text-red-400">{error}</p>}
      {succeeded && !error && <p className="text-sm text-green-600 dark:text-green-400">Password changed.</p>}
      <button
        type="submit"
        disabled={changePasswordMutation.isPending}
        className="w-fit rounded-md bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-60"
      >
        {changePasswordMutation.isPending ? "Changing…" : "Change password"}
      </button>
    </form>
  );
}
