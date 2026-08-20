import type { DocumentStatus } from "../api/types";

const STYLES: Record<DocumentStatus, string> = {
  PROCESSING: "bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300",
  READY: "bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-300",
  FAILED: "bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300",
};

const LABELS: Record<DocumentStatus, string> = {
  PROCESSING: "Processing",
  READY: "Ready",
  FAILED: "Failed",
};

export function StatusBadge({ status }: { status: DocumentStatus }) {
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ${STYLES[status]}`}>
      {status === "PROCESSING" && (
        <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-amber-500" aria-hidden="true" />
      )}
      {LABELS[status]}
    </span>
  );
}
