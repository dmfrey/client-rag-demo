import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { StatusBadge } from "./StatusBadge";

describe("StatusBadge", () => {
  it("shows the human-readable label for each status", () => {
    const { rerender } = render(<StatusBadge status="PROCESSING" />);
    expect(screen.getByText("Processing")).toBeInTheDocument();

    rerender(<StatusBadge status="READY" />);
    expect(screen.getByText("Ready")).toBeInTheDocument();

    rerender(<StatusBadge status="FAILED" />);
    expect(screen.getByText("Failed")).toBeInTheDocument();
  });
});
