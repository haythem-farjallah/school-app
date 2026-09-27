import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { connectWebSocket, disconnectWebSocket } from "./socket";
import { token } from "./token";

// Only the transport is replaced: the real STOMP client writes its frames to this socket.
const { FakeSockJS } = vi.hoisted(() => {
  class FakeSockJS {
    static instances: FakeSockJS[] = [];
    url = "fake-sockjs";
    readyState = 0;
    binaryType = "";
    sent: string[] = [];
    onopen: (() => void) | null = null;
    onclose: ((event: { code: number; reason: string; wasClean: boolean }) => void) | null = null;
    onmessage: ((event: unknown) => void) | null = null;
    onerror: ((event: unknown) => void) | null = null;

    constructor() {
      FakeSockJS.instances.push(this);
    }

    send(data: string) {
      this.sent.push(data);
    }

    open() {
      this.readyState = 1;
      this.onopen?.();
    }

    close() {
      this.drop();
    }

    drop() {
      this.readyState = 3;
      this.onclose?.({ code: 1006, reason: "", wasClean: false });
    }
  }
  return { FakeSockJS };
});

vi.mock("sockjs-client", () => ({ default: FakeSockJS }));

function connectFrameHeaders(socket: InstanceType<typeof FakeSockJS>): Record<string, string> {
  const frame = socket.sent.find((data) => data.startsWith("CONNECT\n"));
  expect(frame).toBeDefined();
  const headerLines = frame!.split("\n\n")[0].split("\n").slice(1);
  return Object.fromEntries(headerLines.map((line) => {
    const separator = line.indexOf(":");
    return [line.slice(0, separator), line.slice(separator + 1)];
  }));
}

async function openLatestSocket() {
  await vi.advanceTimersByTimeAsync(0);
  const socket = FakeSockJS.instances.at(-1)!;
  socket.open();
  return socket;
}

describe("connectWebSocket", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    FakeSockJS.instances = [];
  });

  afterEach(() => {
    disconnectWebSocket();
    vi.useRealTimers();
  });

  it("authenticates CONNECT with the bearer token only", async () => {
    token.access = "access-A";

    connectWebSocket({ id: 52, role: "STUDENT" });
    const headers = connectFrameHeaders(await openLatestSocket());

    expect(headers.Authorization).toBe("Bearer access-A");
    expect(headers).not.toHaveProperty("X-User-ID");
    expect(headers).not.toHaveProperty("X-User-Role");
  });

  it("only subscribes after connecting and never sends application messages", async () => {
    token.access = "access-A";
    connectWebSocket({ id: 52, role: "STUDENT" });
    const socket = await openLatestSocket();

    socket.onmessage?.({ data: "CONNECTED\nversion:1.2\nheart-beat:0,0\n\n\0" });
    await vi.advanceTimersByTimeAsync(0);

    const commands = socket.sent.map((frame) => frame.split("\n")[0]);
    expect(commands).toContain("SUBSCRIBE");
    expect(commands).not.toContain("SEND");
  });

  it("reconnects with the access token that is current at reconnect time", async () => {
    token.access = "access-A";
    connectWebSocket({ id: 52, role: "STUDENT" });
    const first = await openLatestSocket();
    expect(connectFrameHeaders(first).Authorization).toBe("Bearer access-A");

    // The HTTP client refreshed the session, then the connection dropped.
    token.access = "access-B";
    first.drop();
    await vi.advanceTimersByTimeAsync(5000);
    const second = await openLatestSocket();

    expect(second).not.toBe(first);
    expect(connectFrameHeaders(second).Authorization).toBe("Bearer access-B");
  });
});
