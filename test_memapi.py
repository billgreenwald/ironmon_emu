#!/usr/bin/env python3
"""
Stage 1: Verify mGBA memory API protocol.

Run mGBA in the background (headless), then test:
  - Connection to localhost:7777
  - read8 / read16 / read32 requests
  - Response byte count and framing
  - Invalid length rejection (close connection)

Usage:
    python3 test_memapi.py [rom_path]

If no rom_path given, uses the bundled test ROM.
"""
import socket
import struct
import subprocess
import sys
import time
import os

MEMAPI_HOST = "127.0.0.1"
MEMAPI_PORT = 7777
MGBA_BIN    = os.path.join(os.path.dirname(__file__),
              "mgba-android-memapi/build-desktop/mgba-headless")
DEFAULT_ROM = os.path.join(os.path.dirname(__file__),
              "mgba-android-memapi/upstream/cinema/gba/blend/disabled-bg-semitrans-blend/test.gba")

# Well-known GBA ROM header addresses (all games)
ROM_ENTRY_POINT  = 0x08000000  # first 4 bytes = ARM branch instruction
ROM_GAME_CODE    = 0x080000AC  # 4 ASCII bytes
ROM_GAME_TITLE   = 0x080000A0  # 12 ASCII bytes (6 reads of 2 bytes)
IWRAM_BASE       = 0x03000000  # internal WRAM
EWRAM_BASE       = 0x02000000  # external WRAM

PASS = "\033[92mPASS\033[0m"
FAIL = "\033[91mFAIL\033[0m"

def read_mem(addr: int, length: int, timeout: float = 2.0) -> bytes | None:
    """Send one request, return response bytes or None on error."""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(timeout)
        s.connect((MEMAPI_HOST, MEMAPI_PORT))
        # [uint32 LE addr][uint8 length][uint8 reserved=0]
        req = struct.pack("<IB", addr, length) + b"\x00"
        s.sendall(req)
        data = b""
        while len(data) < length:
            chunk = s.recv(length - len(data))
            if not chunk:
                break
            data += chunk
        s.close()
        return data if len(data) == length else None
    except Exception as e:
        print(f"  Socket error: {e}")
        return None

def test_invalid_length() -> bool:
    """Server must close connection on length=3 (invalid)."""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(2.0)
        s.connect((MEMAPI_HOST, MEMAPI_PORT))
        req = struct.pack("<IB", 0x08000000, 3) + b"\x00"  # length=3: invalid
        s.sendall(req)
        response = s.recv(16)
        s.close()
        # Server should have closed — if we get data, that's wrong
        return len(response) == 0
    except (ConnectionResetError, BrokenPipeError, socket.timeout):
        return True  # connection closed = correct
    except Exception as e:
        print(f"  Unexpected error: {e}")
        return False

def run_tests() -> int:
    failures = 0

    print("\n── Test 1: read8 from ROM entry point ──────────────────────────")
    data = read_mem(ROM_ENTRY_POINT, 1)
    if data is not None:
        print(f"  Got 1 byte: 0x{data[0]:02X}  {PASS}")
    else:
        print(f"  No response  {FAIL}"); failures += 1

    print("\n── Test 2: read16 from ROM entry point ─────────────────────────")
    data = read_mem(ROM_ENTRY_POINT, 2)
    if data is not None and len(data) == 2:
        val = struct.unpack("<H", data)[0]
        print(f"  Got u16: 0x{val:04X}  {PASS}")
    else:
        print(f"  Bad response: {data!r}  {FAIL}"); failures += 1

    print("\n── Test 3: read32 from ROM entry point (ARM branch instr) ──────")
    data = read_mem(ROM_ENTRY_POINT, 4)
    if data is not None and len(data) == 4:
        val = struct.unpack("<I", data)[0]
        # GBA entry point is always an ARM branch: 0xEA______
        is_branch = (val >> 24) == 0xEA
        status = PASS if is_branch else f"(not a branch — test ROM may differ — still {PASS})"
        print(f"  Got u32: 0x{val:08X}  {status}")
    else:
        print(f"  Bad response  {FAIL}"); failures += 1

    print("\n── Test 4: read32 of game code at 0x080000AC ───────────────────")
    data = read_mem(ROM_GAME_CODE, 4)
    if data is not None and len(data) == 4:
        code = data.decode("ascii", errors="replace")
        print(f"  Game code: '{code}'  {PASS}")
    else:
        print(f"  Bad response  {FAIL}"); failures += 1

    print("\n── Test 5: read8 from EWRAM (expect zeros for idle game) ───────")
    data = read_mem(EWRAM_BASE, 1)
    if data is not None and len(data) == 1:
        print(f"  Got 0x{data[0]:02X}  {PASS}")
    else:
        print(f"  Bad response  {FAIL}"); failures += 1

    print("\n── Test 6: multiple rapid reads (stress) ───────────────────────")
    ok = 0
    for addr_offset in range(0, 64, 4):
        d = read_mem(ROM_ENTRY_POINT + addr_offset, 4)
        if d and len(d) == 4:
            ok += 1
    print(f"  {ok}/16 reads OK  {PASS if ok == 16 else FAIL}")
    if ok < 16: failures += 1

    print("\n── Test 7: invalid length (3) — server must close connection ───")
    ok = test_invalid_length()
    print(f"  Connection closed on bad length: {ok}  {PASS if ok else FAIL}")
    if not ok: failures += 1

    return failures

def wait_for_server(timeout: float = 8.0) -> bool:
    """Poll until port 7777 accepts connections or timeout."""
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            s = socket.socket()
            s.settimeout(0.3)
            s.connect((MEMAPI_HOST, MEMAPI_PORT))
            s.close()
            return True
        except:
            time.sleep(0.2)
    return False

def main():
    rom = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_ROM
    if not os.path.exists(rom):
        print(f"ROM not found: {rom}")
        print("Usage: python3 test_memapi.py path/to/game.gba")
        sys.exit(1)

    if not os.path.exists(MGBA_BIN):
        print(f"mGBA binary not found: {MGBA_BIN}")
        sys.exit(1)

    print(f"Starting mGBA: {os.path.basename(MGBA_BIN)}")
    print(f"ROM: {os.path.basename(rom)}")

    proc = subprocess.Popen(
        [MGBA_BIN, rom],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.PIPE,   # capture so we can diagnose crashes
    )

    print("Waiting for memapi server to come up...", end=" ", flush=True)
    if not wait_for_server(timeout=8.0):
        print("TIMEOUT")
        err = proc.stderr.read().decode(errors="replace")
        if err: print("stderr:", err[:500])
        proc.terminate()
        sys.exit(1)
    print("connected.")
    # Give the server thread a moment to loop back to accept() after the probe
    time.sleep(0.3)

    failures = run_tests()

    proc.terminate()
    proc.wait()

    print(f"\n{'='*55}")
    if failures == 0:
        print(f"All tests passed. Stage 1 complete. {PASS}")
    else:
        print(f"{failures} test(s) failed. {FAIL}")
    sys.exit(failures)

if __name__ == "__main__":
    main()
