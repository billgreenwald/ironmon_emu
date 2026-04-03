#include "main.h"

#include <mgba/core/version.h>
#include <time.h>

float g_fps = 0.0f;
int g_stall_count = 0;

void mSDLGLDoViewport(int w, int h, struct VideoBackend* v) {
	v->resized(v, w, h);
	v->clear(v);
	v->swap(v);
	v->clear(v);
}

#include <android/log.h>
#define LOG_TAG "gl-common"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

#include "swappy/swappyGL.h"
#include <EGL/egl.h>

void mSDLGLCommonSwap(struct VideoBackend* context) {
	struct mSDLRenderer* renderer = (struct mSDLRenderer*) context->user;
#if SDL_VERSION_ATLEAST(2, 0, 0)
	// SDL_GL_SwapWindow(renderer->window);
    
    EGLDisplay dpy = eglGetCurrentDisplay();
    EGLSurface surf = eglGetCurrentSurface(EGL_DRAW);
    if (dpy == EGL_NO_DISPLAY || surf == EGL_NO_SURFACE) {
        LOGD("Swappy Warning: Invalid EGL Display (%p) or Surface (%p). Fallback to SDL Swap.", dpy, surf);
        SDL_GL_SwapWindow(renderer->window);
    } else {
        struct timespec t0, t1;
        clock_gettime(CLOCK_MONOTONIC, &t0);
        SwappyGL_swap(dpy, surf);
        clock_gettime(CLOCK_MONOTONIC, &t1);
        long elapsed_us = (t1.tv_sec - t0.tv_sec) * 1000000L + (t1.tv_nsec - t0.tv_nsec) / 1000L;
        if (elapsed_us > 33000) {
            g_stall_count++;
            __android_log_print(ANDROID_LOG_WARN, "mGBA_Perf", "Frame stall: %ld us (stall #%d)", elapsed_us, g_stall_count);
        }
        // SDL_GL_SwapWindow(renderer->window); // Disabled to prevent double swap
    }
    
    
    static uint32_t lastTime = 0;
    static int frames = 0;
    uint32_t now = SDL_GetTicks();
    uint32_t delta = now - lastTime;
    
    // Log delta every frame to see jitter (warning: spammy, but necessary for diagnosis)
    // LOGD("Frame Delta: %u ms", delta); 

    frames++;
    if (delta >= 1000) {
        g_fps = frames * 1000.0f / delta;
        LOGD("Game FPS: %.2f", g_fps);
        frames = 0;
        lastTime = now;
    }
#else
	UNUSED(renderer);
	SDL_GL_SwapBuffers();
#endif
}

bool mSDLGLCommonInit(struct mSDLRenderer* renderer) {
#ifndef COLOR_16_BIT
	SDL_GL_SetAttribute(SDL_GL_RED_SIZE, 8);
	SDL_GL_SetAttribute(SDL_GL_GREEN_SIZE, 8);
	SDL_GL_SetAttribute(SDL_GL_BLUE_SIZE, 8);
#else
	SDL_GL_SetAttribute(SDL_GL_RED_SIZE, 5);
#ifdef COLOR_5_6_5
	SDL_GL_SetAttribute(SDL_GL_GREEN_SIZE, 6);
#else
	SDL_GL_SetAttribute(SDL_GL_GREEN_SIZE, 5);
#endif
	SDL_GL_SetAttribute(SDL_GL_BLUE_SIZE, 5);
#endif

#if SDL_VERSION_ATLEAST(2, 0, 0)
	renderer->window = SDL_CreateWindow(projectName, SDL_WINDOWPOS_UNDEFINED, SDL_WINDOWPOS_UNDEFINED, renderer->viewportWidth, renderer->viewportHeight, SDL_WINDOW_OPENGL |SDL_WINDOW_FULLSCREEN_DESKTOP);
	renderer->glCtx = SDL_GL_CreateContext(renderer->window);
	if (!renderer->glCtx) {
		SDL_DestroyWindow(renderer->window);
		return false;
	}
	// SDL_GL_SetSwapInterval(0) removed — leaving vsync=1 (default) so the display's hardware
	// vsync caps frames at 60fps. With vsync=0, Swappy was the sole gatekeeper; any
	// miscalibration on first launch caused uncapped rendering → GPU thermal throttle → lag.
	// After bg/fg the EGL surface was recreated (resetting to vsync=1), which is why lag
	// disappeared after backgrounding. Swappy's setAutoSwapInterval(false) prevents it from
	// adding extra delay on top of vsync, so the two work together cleanly.
	SDL_GetWindowSize(renderer->window, &renderer->viewportWidth, &renderer->viewportHeight);
	renderer->player.window = renderer->window;
	if (renderer->lockIntegerScaling) {
		SDL_SetWindowMinimumSize(renderer->window, renderer->width, renderer->height);
	}
#else
	SDL_GL_SetAttribute(SDL_GL_SWAP_CONTROL, 1);
#ifdef COLOR_16_BIT
	SDL_Surface* surface = SDL_SetVideoMode(renderer->viewportWidth, renderer->viewportHeight, 16, SDL_OPENGL | SDL_RESIZABLE | (SDL_FULLSCREEN * renderer->player.fullscreen));
#else
	SDL_Surface* surface = SDL_SetVideoMode(renderer->viewportWidth, renderer->viewportHeight, 32, SDL_OPENGL | SDL_RESIZABLE | (SDL_FULLSCREEN * renderer->player.fullscreen));
#endif
	if (!surface) {
		return false;
	}
	SDL_WM_SetCaption(projectName, "");
#endif
	return true;
}
