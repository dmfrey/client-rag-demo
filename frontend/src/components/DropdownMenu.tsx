import { useEffect, useRef, useState, type ReactNode } from "react";

export interface DropdownMenuProps {
  trigger: (props: { onClick: (event: React.MouseEvent) => void; open: boolean }) => ReactNode;
  children: ReactNode;
  align?: "left" | "right";
  // "bottom" (default) opens downward, relying on the menu's natural static position right
  // below the trigger. "top" opens upward instead - needed for a trigger pinned to the bottom
  // of its container (e.g. UserMenu, at the very bottom of the sidebar) where a downward menu
  // has nowhere to render and ends up clipped off-screen below the viewport.
  position?: "top" | "bottom";
}

export function DropdownMenu({ trigger, children, align = "right", position = "bottom" }: DropdownMenuProps) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;

    function handlePointerDown(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setOpen(false);
    }

    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]);

  return (
    <div ref={containerRef} className="relative inline-block">
      {trigger({
        open,
        onClick: (event) => {
          event.stopPropagation();
          event.preventDefault();
          setOpen((value) => !value);
        },
      })}
      {open && (
        <div
          role="menu"
          onClick={() => setOpen(false)}
          className={`absolute z-10 min-w-[9rem] rounded-md border border-gray-200 bg-white py-1 shadow-lg dark:border-gray-800 dark:bg-gray-900 ${
            align === "right" ? "right-0" : "left-0"
          } ${position === "top" ? "bottom-full mb-1" : "top-full mt-1"}`}
        >
          {children}
        </div>
      )}
    </div>
  );
}

export function DropdownMenuItem({
  onClick,
  danger,
  children,
}: {
  onClick: () => void;
  danger?: boolean;
  children: ReactNode;
}) {
  return (
    <button
      type="button"
      role="menuitem"
      onClick={onClick}
      className={`block w-full px-3 py-1.5 text-left text-sm ${
        danger
          ? "text-red-600 hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-950/30"
          : "text-gray-700 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-800"
      }`}
    >
      {children}
    </button>
  );
}
