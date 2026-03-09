/*
 * memapi_server.h — Memory Read API public interface
 *
 * Include this header in the Android JNI entry point to hook
 * memapi_server_start() / memapi_server_stop() into the emulator lifecycle.
 */

#pragma once

#ifdef MEMAPI_ENABLE

#include <mgba/core/core.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Start the memory API TCP server (localhost:7777).
 * Must be called after the mCore is initialized and the ROM is loaded.
 * Safe to call from the main thread — server runs on its own pthread.
 */
void memapi_server_start(struct mCore *core);

/**
 * Stop the memory API server and join its thread.
 * Must be called before tearing down the mCore.
 */
void memapi_server_stop(void);

#ifdef __cplusplus
}
#endif

#else /* !MEMAPI_ENABLE */

/* Compile away to nothing in release builds */
static inline void memapi_server_start(void *core) { (void)core; }
static inline void memapi_server_stop(void)         {}

#endif /* MEMAPI_ENABLE */
