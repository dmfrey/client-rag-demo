import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from "react";
import { useParams } from "react-router-dom";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { chatApi, sendMessageStream } from "../api/chat";
import type { ChatMessage, Citation } from "../api/types";

export function ChatPage() {
  const { id } = useParams<{ id: string }>();
  const sessionId = Number(id);
  const queryClient = useQueryClient();

  const sessionQuery = useQuery({
    queryKey: ["chats", sessionId],
    queryFn: () => chatApi.get(sessionId),
  });

  const messagesQuery = useQuery({
    queryKey: ["chats", sessionId, "messages"],
    queryFn: () => chatApi.getMessages(sessionId),
  });

  const [input, setInput] = useState("");
  const [pendingUserMessage, setPendingUserMessage] = useState<string | null>(null);
  const [streamingText, setStreamingText] = useState("");
  const [streamingSources, setStreamingSources] = useState<Citation[] | null>(null);
  const [isStreaming, setIsStreaming] = useState(false);
  const [streamError, setStreamError] = useState<string | null>(null);

  const bottomRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messagesQuery.data, streamingText, pendingUserMessage]);

  function submitMessage() {
    const content = input.trim();
    if (!content || isStreaming) return;

    setInput("");
    setPendingUserMessage(content);
    setStreamingText("");
    setStreamingSources(null);
    setStreamError(null);
    setIsStreaming(true);

    sendMessageStream(sessionId, content, {
      onToken: (text) => setStreamingText((prev) => prev + text),
      onSources: (sources) => setStreamingSources(sources),
      onError: () => {
        setStreamError("Something went wrong while streaming the response. Please try again.");
        setIsStreaming(false);
      },
      onDone: async () => {
        await queryClient.invalidateQueries({ queryKey: ["chats", sessionId, "messages"] });
        void queryClient.invalidateQueries({ queryKey: ["chats"] }); // picks up the auto-generated title
        setPendingUserMessage(null);
        setStreamingText("");
        setStreamingSources(null);
        setIsStreaming(false);
      },
    });
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    submitMessage();
  }

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      submitMessage();
    }
  }

  const messages = messagesQuery.data ?? [];

  return (
    <div className="flex h-full flex-col">
      <div className="flex-none border-b border-gray-200 px-6 py-3 dark:border-gray-800">
        <h1 className="truncate text-sm font-medium text-gray-900 dark:text-gray-100">
          {sessionQuery.data?.title ?? "New chat"}
        </h1>
      </div>

      <div className="flex-1 overflow-y-auto">
        <div className="mx-auto flex max-w-3xl flex-col gap-4 p-6">
          {messages.map((message, index) => (
            <MessageBubble key={index} message={message} />
          ))}

          {pendingUserMessage && (
            <MessageBubble message={{ role: "USER", content: pendingUserMessage, citations: [] }} />
          )}

          {isStreaming && (
            <MessageBubble
              message={{
                role: "ASSISTANT",
                content: streamingText || "…",
                citations: streamingSources ?? [],
              }}
            />
          )}

          {streamError && <p className="text-sm text-red-600 dark:text-red-400">{streamError}</p>}

          <div ref={bottomRef} />
        </div>
      </div>

      <form onSubmit={handleSubmit} className="border-t border-gray-200 p-4 dark:border-gray-800">
        <div className="mx-auto flex max-w-3xl items-end gap-2">
          <textarea
            value={input}
            onChange={(event) => setInput(event.target.value)}
            onKeyDown={handleKeyDown}
            rows={1}
            placeholder="Ask a question grounded in the uploaded documents…"
            className="flex-1 resize-none rounded-md border border-gray-300 px-3 py-2 text-sm text-gray-900 focus:border-blue-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
          />
          <button
            type="submit"
            disabled={isStreaming || !input.trim()}
            className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-60"
          >
            Send
          </button>
        </div>
      </form>
    </div>
  );
}

function MessageBubble({ message }: { message: ChatMessage }) {
  const isUser = message.role === "USER";
  return (
    <div className={`flex ${isUser ? "justify-end" : "justify-start"}`}>
      <div
        className={`max-w-[80%] rounded-lg px-4 py-2 text-sm ${
          isUser
            ? "bg-blue-600 text-white whitespace-pre-wrap"
            : "bg-gray-100 text-gray-900 dark:bg-gray-800 dark:text-gray-100"
        }`}
      >
        {isUser ? message.content : <MarkdownContent content={message.content} />}
        {message.citations.length > 0 && (
          <details className="mt-2 text-xs opacity-80">
            <summary className="cursor-pointer font-medium">Sources</summary>
            <ul className="mt-1 list-inside list-disc">
              {message.citations.map((citation, index) => (
                <li key={index}>{citation.filename}</li>
              ))}
            </ul>
          </details>
        )}
      </div>
    </div>
  );
}

function MarkdownContent({ content }: { content: string }) {
  return (
    <div className="[&>*:first-child]:mt-0 [&>*:last-child]:mb-0">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          p: ({ children }) => <p className="my-2 leading-relaxed">{children}</p>,
          strong: ({ children }) => <strong className="font-semibold">{children}</strong>,
          ul: ({ children }) => <ul className="my-2 list-inside list-disc space-y-1">{children}</ul>,
          ol: ({ children }) => <ol className="my-2 list-inside list-decimal space-y-1">{children}</ol>,
          li: ({ children }) => <li>{children}</li>,
          h1: ({ children }) => <h1 className="mt-3 mb-2 text-base font-semibold">{children}</h1>,
          h2: ({ children }) => <h2 className="mt-3 mb-2 text-base font-semibold">{children}</h2>,
          h3: ({ children }) => <h3 className="mt-3 mb-1 text-sm font-semibold">{children}</h3>,
          a: ({ children, href }) => (
            <a href={href} target="_blank" rel="noreferrer" className="text-blue-600 underline dark:text-blue-400">
              {children}
            </a>
          ),
          code: ({ children }) => (
            <code className="rounded bg-gray-200 px-1 py-0.5 font-mono text-xs dark:bg-gray-700">{children}</code>
          ),
          pre: ({ children }) => (
            <pre className="my-2 overflow-x-auto rounded bg-gray-200 p-2 font-mono text-xs dark:bg-gray-700">
              {children}
            </pre>
          ),
          table: ({ children }) => (
            <div className="my-2 overflow-x-auto">
              <table className="min-w-full border-collapse text-xs">{children}</table>
            </div>
          ),
          thead: ({ children }) => (
            <thead className="border-b border-gray-300 dark:border-gray-600">{children}</thead>
          ),
          tr: ({ children }) => <tr className="border-b border-gray-200 dark:border-gray-700">{children}</tr>,
          th: ({ children }) => <th className="px-2 py-1 text-left font-semibold">{children}</th>,
          td: ({ children }) => <td className="px-2 py-1 align-top">{children}</td>,
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
}
