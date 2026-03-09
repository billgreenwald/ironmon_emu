/*
 * memapi_server.c — mGBA Android Memory Read API
 *
 * Starts a TCP server on localhost:7777 that allows external clients (e.g. the
 * Ironmon Tracker Android app) to read GBA memory contents from a running game.
 *
 * PROTOCOL (all little-endian):
 *   Request  (6 bytes): [uint32 address][uint8 length][uint8 reserved=0x00]
 *   Response (1-4 bytes): [length bytes of raw memory, little-endian]
 *   On invalid request: server closes connection.
 *
 * Only compiled when MEMAPI_ENABLE is defined (debug builds only).
 * Never present in release builds.
 */

#ifdef MEMAPI_ENABLE

#include <pthread.h>
#include <stdint.h>
#include <string.h>
#include <errno.h>

#include <mgba-util/socket.h>
#include <mgba/core/core.h>

#define MEMAPI_PORT      7777
#define MEMAPI_HOST      "127.0.0.1"
#define MEMAPI_REQ_LEN   6
#define MEMAPI_BACKLOG   4

static pthread_t        s_memapi_thread;
static volatile int     s_memapi_running = 0;
static struct mCore    *s_core           = NULL;
static Socket           s_server_sock    = INVALID_SOCKET;

/* -------------------------------------------------------------------------
 * Thread function: accept loop
 * ---------------------------------------------------------------------- */
static void *memapi_thread_func(void *arg) {
    (void)arg;

    /* Bind to 127.0.0.1 only — not accessible from the network */
    struct Address localAddr;
    localAddr.version = IPV4;
    localAddr.ipv4    = 0x7F000001UL; /* 127.0.0.1 in host byte order */

    Socket server = SocketOpenTCP(MEMAPI_PORT, &localAddr);
    if (!SOCKET_FAILED(server)) {
        /* Stash so memapi_server_stop() can close it to unblock accept(). */
        s_server_sock = server;
    } else {
        return NULL;
    }

    if (SocketListen(server, MEMAPI_BACKLOG) < 0) {
        SocketClose(server);
        return NULL;
    }

    while (s_memapi_running) {
        Socket client = SocketAccept(server, NULL);
        if (SOCKET_FAILED(client)) {
            /* Either stop was requested (server closed) or transient error. */
            break;
        }

        /* ---- Read 6-byte request ---- */
        uint8_t req[MEMAPI_REQ_LEN];
        int     received = 0;
        while (received < MEMAPI_REQ_LEN) {
            int r = SocketRecv(client, req + received, MEMAPI_REQ_LEN - received);
            if (r <= 0) {
                received = -1;
                break;
            }
            received += r;
        }

        if (received != MEMAPI_REQ_LEN) {
            SocketClose(client);
            continue;
        }

        uint32_t address = (uint32_t)req[0]
                         | ((uint32_t)req[1] << 8)
                         | ((uint32_t)req[2] << 16)
                         | ((uint32_t)req[3] << 24);
        uint8_t  length  = req[4];
        /* req[5] is reserved — we accept any value */

        /* Validate length */
        if (length != 1 && length != 2 && length != 4) {
            SocketClose(client);
            continue;
        }

        /* ---- Read memory from emulator core ---- */
        uint8_t resp[4] = {0};
        if (s_core != NULL) {
            switch (length) {
            case 1: {
                uint8_t v = (uint8_t)s_core->busRead8(s_core, address);
                resp[0] = v;
                break;
            }
            case 2: {
                uint16_t v = (uint16_t)s_core->busRead16(s_core, address);
                resp[0] = (uint8_t)(v & 0xFF);
                resp[1] = (uint8_t)(v >> 8);
                break;
            }
            case 4: {
                uint32_t v = s_core->busRead32(s_core, address);
                resp[0] = (uint8_t)(v & 0xFF);
                resp[1] = (uint8_t)((v >> 8)  & 0xFF);
                resp[2] = (uint8_t)((v >> 16) & 0xFF);
                resp[3] = (uint8_t)((v >> 24) & 0xFF);
                break;
            }
            }
        }
        /* If core is NULL, respond with zeros — client will interpret as disconnected. */

        /* ---- Send response ---- */
        int sent = 0;
        while (sent < length) {
            int s = SocketSend(client, resp + sent, length - sent);
            if (s <= 0) break;
            sent += s;
        }

        SocketClose(client);
    }

    SocketClose(server);
    s_server_sock = INVALID_SOCKET;
    return NULL;
}

/* -------------------------------------------------------------------------
 * Public API
 * ---------------------------------------------------------------------- */

/**
 * Start the memory API server on a background thread.
 * Call this after the emulator core is fully initialized and ROM is loaded.
 */
void memapi_server_start(struct mCore *core) {
    if (s_memapi_running) {
        return; /* Already running */
    }
    s_core           = core;
    s_memapi_running = 1;

    if (pthread_create(&s_memapi_thread, NULL, memapi_thread_func, NULL) != 0) {
        s_memapi_running = 0;
        s_core           = NULL;
    }
}

/**
 * Stop the memory API server.
 * Call this before tearing down the emulator core.
 * Blocks until the server thread exits.
 */
void memapi_server_stop(void) {
    if (!s_memapi_running) {
        return;
    }
    s_memapi_running = 0;
    s_core           = NULL;

    /* Close server socket to unblock any pending accept(). */
    if (!SOCKET_FAILED(s_server_sock)) {
        SocketClose(s_server_sock);
        s_server_sock = INVALID_SOCKET;
    }

    pthread_join(s_memapi_thread, NULL);
}

#endif /* MEMAPI_ENABLE */
