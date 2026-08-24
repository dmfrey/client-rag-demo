import { useState, type ReactNode } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { bannerApi } from "../api/banner";

const BANNER_QUERY_KEY = ["banner"];

// Wraps the whole app (see main.tsx) - renders nothing but a blocking consent screen until this
// session has acknowledged the banner. The backend is the actual enforcement point (every other
// endpoint 428s pre-acknowledgment - see BannerAckFilter); this component exists to show the
// required text and drive the acknowledgment call, not to gate access on its own. A client that
// skipped this component entirely would just get 428s from every other request.
export function BannerGate({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [ackError, setAckError] = useState<string | null>(null);

  const bannerQuery = useQuery({
    queryKey: BANNER_QUERY_KEY,
    queryFn: bannerApi.get,
  });

  const ackMutation = useMutation({
    mutationFn: bannerApi.acknowledge,
    onSuccess: () => queryClient.setQueryData(BANNER_QUERY_KEY, { text: bannerQuery.data?.text ?? "", acknowledged: true }),
  });

  if (bannerQuery.isLoading) {
    return null;
  }

  if (bannerQuery.isError) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4 dark:bg-gray-950">
        <div className="max-w-sm text-center">
          <p className="text-sm text-red-600 dark:text-red-400">
            Couldn&apos;t load the consent banner. Please refresh the page to try again.
          </p>
        </div>
      </div>
    );
  }

  if (bannerQuery.data?.acknowledged) {
    return children;
  }

  async function handleAcknowledge() {
    setAckError(null);
    try {
      await ackMutation.mutateAsync();
    } catch {
      setAckError("Something went wrong recording your acknowledgment. Please try again.");
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-gray-50 px-4 py-8 dark:bg-gray-950">
      <div className="flex max-h-full w-full max-w-2xl flex-col rounded-lg border border-gray-200 bg-white shadow-sm dark:border-gray-800 dark:bg-gray-900">
        <div className="overflow-y-auto p-6">
          <h1 className="mb-4 text-center text-lg font-semibold text-gray-900 dark:text-gray-100">
            Notice and Consent
          </h1>
          <pre className="whitespace-pre-wrap font-sans text-sm text-gray-700 dark:text-gray-300">
            {bannerQuery.data?.text}
          </pre>
        </div>
        <div className="border-t border-gray-200 p-4 dark:border-gray-800">
          {ackError && <p className="mb-2 text-sm text-red-600 dark:text-red-400">{ackError}</p>}
          <button
            type="button"
            onClick={handleAcknowledge}
            disabled={ackMutation.isPending}
            className="w-full rounded-md bg-blue-600 px-3 py-2 font-medium text-white hover:bg-blue-700 disabled:opacity-60"
          >
            {ackMutation.isPending ? "Recording…" : "I Agree"}
          </button>
        </div>
      </div>
    </div>
  );
}
