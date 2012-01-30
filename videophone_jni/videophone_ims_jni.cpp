/* Copyright (c) 2012, Code Aurora Forum. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of Code Aurora Forum, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#define LOG_TAG "jniDpl"
#define LOG_NDEBUG 0
#define LOG_NIDEBUG 0
#define LOG_NDDEBUG 0
#include <cutils/log.h>
#include "videophone_ims_jni.h"
#include <stdlib.h>
#include "videophone_impl.h"

static jint dpl_init(JNIEnv *e, jobject o)
{
    LOGD("%s", __func__);
	initImsThinClient();
    return 0;
}

static void dpl_deinit(JNIEnv *e, jobject o)
{
    LOGD("%s", __func__);
	deInitImsThinClient();
}

static void dpl_render_thread(JNIEnv *e, jobject o)
{
    LOGD("%s", __func__);
}
static jint dpl_handleRawFrame(JNIEnv *e, jobject o, jbyteArray frame)
{
    jint ret = 0;
    jsize size;
    jbyte *bytes;
    uint8_t *nativeframe;

    LOGD("%s", __func__);

    if (frame != NULL) {
        jsize size = e->GetArrayLength(frame);
        bytes = e->GetByteArrayElements(frame, JNI_FALSE);
		    frameToEncode((unsigned short *)bytes,(int)size);
        e->ReleaseByteArrayElements(frame, bytes, JNI_ABORT);
    } else {
        LOGD("%s: Received a null frame", __func__);
        ret = -1;
    }
    return ret;
}

static int dpl_setSurface(JNIEnv *e, jobject o, jobject osurface)
{
    LOGD("%s", __func__);
	setFarEndSurface(e, osurface);
    return 0;
}

static JNINativeMethod sMethods[] =
{
    {"nativeInit", "()I", (void *)dpl_init},
    {"nativeRenderThread", "()V", (void *)dpl_render_thread},
    {"nativeDeInit", "()V", (void *)dpl_deinit},
    {"nativeHandleRawFrame", "([B)V", (void *)dpl_handleRawFrame},
    {"nativeSetSurface", "(Landroid/graphics/SurfaceTexture;)I", (void *)dpl_setSurface},
};

#define METHODS_LEN (sizeof(sMethods) / sizeof(sMethods[0]))

int register_videophone_Dpl(JNIEnv *e)
{
    LOGD("%s\n", __func__);

    jclass klass;

    klass = e->FindClass("com/android/phone/MediaHandler");
    if (!klass) {
        LOGE("%s: Unable to find java class com/android/phone/MediaHandler\n", __func__);
        return JNI_ERR;
    }
    return e->RegisterNatives(klass, sMethods, METHODS_LEN);
}
