import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import JournalInput from "../JournalInput";

// Mock the custom hooks
const mockMutateAsync = vi.fn();
vi.mock("../../hooks/useJournalData", () => ({
  useAddJournalEntry: () => ({
    mutateAsync: mockMutateAsync,
    isPending: false,
    isError: false,
    error: null,
  }),
}));

// Mock the theme context
vi.mock("../../contexts/ThemeContext", () => ({
  useTheme: () => ({ theme: "light", toggleTheme: vi.fn() }),
}));

describe("JournalInput", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders textarea and submit button", () => {
    render(<JournalInput />);
    expect(screen.getByPlaceholderText(/Write your thoughts here/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Analyze & Save/i })).toBeInTheDocument();
  });

  it("disables the submit button when text is empty", () => {
    render(<JournalInput />);
    const button = screen.getByRole("button", { name: /Analyze & Save/i });
    expect(button).toBeDisabled();
  });

  it("calls mutateAsync with text when submitted", async () => {
    render(<JournalInput />);
    const textarea = screen.getByPlaceholderText(/Write your thoughts here/i);
    const button = screen.getByRole("button", { name: /Analyze & Save/i });

    await userEvent.type(textarea, "My test journal entry");
    fireEvent.click(button);

    await waitFor(() => {
      expect(mockMutateAsync).toHaveBeenCalledWith({ rawText: "My test journal entry" });
    });
  });
});