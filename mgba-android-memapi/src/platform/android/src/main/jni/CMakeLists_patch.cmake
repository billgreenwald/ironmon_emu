# CMakeLists patch for mgba-android-memapi
#
# Apply these additions to the existing Android CMakeLists.txt in the mGBA
# Android module (typically at: src/platform/android/CMakeLists.txt)
#
# ─────────────────────────────────────────────────────────────────────────────
# 1. Near the top, after project() declaration, add:
# ─────────────────────────────────────────────────────────────────────────────

option(MEMAPI_ENABLE "Enable memory read API server (debug builds only)" OFF)

# ─────────────────────────────────────────────────────────────────────────────
# 2. After the main mgba target_sources() block, add:
# ─────────────────────────────────────────────────────────────────────────────

if(MEMAPI_ENABLE)
    target_sources(mgba PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/src/main/jni/memapi_server.c
    )
    target_compile_definitions(mgba PRIVATE MEMAPI_ENABLE)
    message(STATUS "mGBA: Memory API server ENABLED on port 7777")
endif()
