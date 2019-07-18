#include "animation3d_renderer3d_OpenCLRaycaster.h"

#include <string.h>
#include <stdlib.h>
#include <stdexcept>

#include "BasicOpenCLJNI.h"
#include "RaycasterJNI.h"

static RaycasterJNI<unsigned char> *raycaster8 = NULL;
static RaycasterJNI<unsigned short> *raycaster16 = NULL;

JNIEXPORT void JNICALL Java_animation3d_renderer3d_OpenCLRaycaster_initRaycaster8(
		JNIEnv *env,
		jclass,
		jint nChannels,
		jint w, jint h, jint d,
		jint wOut, jint hOut)
{
	setOpenCLWarningHandler(env);
	try {
		raycaster8 = new RaycasterJNI<unsigned char>(env,
				nChannels, w, h, d, wOut, hOut);
	} catch(std::runtime_error& e) {
		ThrowException(env, e.what());
	}
}

JNIEXPORT void JNICALL Java_animation3d_renderer3d_OpenCLRaycaster_initRaycaster16(
		JNIEnv *env,
		jclass,
		jint nChannels,
		jint w, jint h, jint d,
		jint wOut, jint hOut)
{
	setOpenCLWarningHandler(env);
	try {
		raycaster16 = new RaycasterJNI<unsigned short>(env,
				nChannels, w, h, d, wOut, hOut);
	} catch(std::runtime_error& e) {
		ThrowException(env, e.what());
	}
}

JNIEXPORT void JNICALL Java_animation3d_renderer3d_OpenCLRaycaster_setBackground(
		JNIEnv *env,
		jclass,
		jintArray data,
		jint w, jint h)
{
	try {
		if(raycaster8 != NULL)
			raycaster8->setBackground(env, data, w, h);
		else if(raycaster16 != NULL)
			raycaster16->setBackground(env, data, w, h);
		else
			ThrowException(env, "No raycaster initialized\n");
	} catch(std::runtime_error& e) {
		ThrowException(env, e.what());
	}
}

JNIEXPORT void JNICALL Java_animation3d_renderer3d_OpenCLRaycaster_clearBackground(JNIEnv *env)
{
	try {
		if(raycaster8 != NULL)
			raycaster8->clearBackground(env);
		else if(raycaster16 != NULL)
			raycaster16->clearBackground(env);
		else
			ThrowException(env, "No raycaster initialized\n");
	} catch(std::runtime_error& e) {
		ThrowException(env, e.what());
	}
}

JNIEXPORT void JNICALL Java_animation3d_renderer3d_OpenCLRaycaster_setColorLUT(
		JNIEnv *env,
		jclass,
		jint channel,
		jintArray lut)
{
	try {
		if(raycaster8 != NULL)
			raycaster8->setColorLUT(env, channel, lut);
		else if(raycaster16 != NULL)
			raycaster16->setColorLUT(env, channel, lut);
		else
			ThrowException(env, "No raycaster initialized\n");
	} catch(std::runtime_error& e) {
		ThrowException(env, e.what());
	}
}

JNIEXPORT void JNICALL Java_animation3d_renderer3d_OpenCLRaycaster_clearColorLUT(
		JNIEnv *env,
		jclass,
		jint channel)
{
	try {
		if(raycaster8 != NULL)
			raycaster8->clearColorLUT(env, channel);
		else if(raycaster16 != NULL)
			raycaster16->clearColorLUT(env, channel);
		else
			ThrowException(env, "No raycaster initialized\n");
	} catch(std::runtime_error& e) {
		ThrowException(env, e.what());
	}
}

JNIEXPORT void JNICALL Java_animation3d_renderer3d_OpenCLRaycaster_setTexture8(
		JNIEnv *env,
		jclass,
		jint channel,
		jobjectArray data,
		jfloat dzByDx)
{
	if(raycaster8 != NULL) {
		try {
			raycaster8->setTexture(env, channel, data, dzByDx);
		} catch(std::runtime_error& e) {
			ThrowException(env, e.what());
		}
	} else {
		ThrowException(env, "No raycaster initialized\n");
	}
}

JNIEXPORT void JNICALL Java_animation3d_renderer3d_OpenCLRaycaster_setTexture16(
		JNIEnv *env,
		jclass,
		jint channel,
		jobjectArray data,
		jfloat dzByDx)
{
	if(raycaster16 != NULL) {
		try {
			raycaster16->setTexture(env, channel, data, dzByDx);
		} catch(std::runtime_error& e) {
			ThrowException(env, e.what());
		}
	} else {
		ThrowException(env, "No raycaster initialized\n");
	}
}

JNIEXPORT void JNICALL Java_animation3d_renderer3d_OpenCLRaycaster_setTargetSize(
		JNIEnv *env,
		jclass,
		jint tgtWidth,
		jint tgtHeight)
{
	try {
		if(raycaster8 != NULL) {
			raycaster8->setTgtDimensions(tgtWidth, tgtHeight);
		} else if(raycaster16 != NULL) {
			raycaster16->setTgtDimensions(tgtWidth, tgtHeight);
		} else {
			ThrowException(env, "No raycaster initialized\n");
		}
	} catch(std::runtime_error& e) {
		ThrowException(env, e.what());
	}
}

JNIEXPORT void JNICALL Java_animation3d_renderer3d_OpenCLRaycaster_close
  (JNIEnv *env, jclass)
{
	printf("renderer3d_OpenCLRaycaster_close()\n");
	fflush(stdout);
	delete raycaster16;
	raycaster16 = NULL;
	delete raycaster8;
	raycaster8 = NULL;
}

JNIEXPORT void JNICALL Java_animation3d_renderer3d_OpenCLRaycaster_white (
		JNIEnv *env,
		jclass,
		jint channel)
{
	try {
		if(raycaster8 != NULL) {
			raycaster8->white(env, channel);
		} else if(raycaster16 != NULL) {
			raycaster16->white(env, channel);
		} else {
			ThrowException(env, "No raycaster initialized\n");
		}
	} catch(std::runtime_error& e) {
		ThrowException(env, e.what());
	}
}

JNIEXPORT void JNICALL Java_animation3d_renderer3d_OpenCLRaycaster_setKernel(
		JNIEnv *env,
		jclass,
		jstring kernel)
{
	const char* k = env->GetStringUTFChars(kernel, 0);

	try {
		if(raycaster8 != NULL) {
			raycaster8->setKernel(env, k);
			env->ReleaseStringUTFChars(kernel, k);
		} else if(raycaster16 != NULL) {
			raycaster16->setKernel(env, k);
			env->ReleaseStringUTFChars(kernel, k);
		} else {
			env->ReleaseStringUTFChars(kernel, k);
			ThrowException(env, "No raycaster initialized\n");
		}
	} catch(std::runtime_error& e) {
		env->ReleaseStringUTFChars(kernel, k);
		ThrowException(env, e.what());
	}
}

JNIEXPORT jintArray JNICALL Java_animation3d_renderer3d_OpenCLRaycaster_cast (
		JNIEnv *env,
		jclass,
	       	jfloatArray inverseTransform,
		jfloat alphacorr,
		jobjectArray channelSettings,
		jint bgr, jint bgg, jint bgb)
{
	try {
		if(raycaster8 != NULL) {
			return raycaster8->project(env,
					inverseTransform,
					alphacorr,
					channelSettings,
					bgr, bgg, bgb);
		}
		else if(raycaster16 != NULL) {
			return raycaster16->project(env,
					inverseTransform,
					alphacorr,
					channelSettings,
					bgr, bgg, bgb);
		} else {
			ThrowException(env, "No raycaster initialized\n");
			return NULL;
		}
	} catch(std::runtime_error& e) {
		ThrowException(env, e.what());
		return NULL;
	}
}

