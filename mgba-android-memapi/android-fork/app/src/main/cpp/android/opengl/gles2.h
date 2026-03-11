/* Shim: forward to the mgba-internal GLES2 header so that mGLES2Context
 * uses the same (OLD) struct layout as mgba/src/platform/opengl/gles2.c.
 * The android-fork copy had a newer, incompatible layout. */
#ifndef ANDROID_GLES2_H
#define ANDROID_GLES2_H

/* Pull in the GL headers the old gles2.h would have */
#ifdef USE_EPOXY
#include <epoxy/gl.h>
#elif defined(BUILD_GLES3)
#include <GLES3/gl3.h>
#else
#include <GLES2/gl2.h>
#endif

/* Use the version-matched struct definitions from mgba source */
#include "platform/opengl/gles2.h"

/* Additional declarations used by android/sdl code but not in the old header */
#ifdef ENABLE_VFS
struct VDir;
bool mGLES2ShaderLoad(struct VideoShader*, struct VDir*);
#endif
void mGLES2ShaderFree(struct VideoShader*);

#endif /* ANDROID_GLES2_H */
