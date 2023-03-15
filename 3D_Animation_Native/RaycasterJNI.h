#ifndef RAYCASTER_JNI_H
#define RAYCASTER_JNI_H

#include <jni.h>
#include "Raycaster.h"

template<typename T>
class RaycasterJNI
{
private:
	T ***cArray_;

	JNIEnv *env_;

	Raycaster<T> *raycaster;

	void initPlane(JNIEnv *env, jobjectArray data, int channel, int z);
	void deletePlane(JNIEnv *env, int channel, int z);
public:
	RaycasterJNI(JNIEnv *env, jint nChannels,
			jint width, jint height, jint depth,
			jint tgtWidth, jint tgtHeight);
	~RaycasterJNI();
	void setTexture(JNIEnv *env, jint channel, jobjectArray data);

	void setColorLUT(JNIEnv *env, jint channel, jintArray lut);

	void clearColorLUT(JNIEnv *env, jint channel);

	void calculateGradients(JNIEnv *env, jint channel, jfloat dzByDx);

	void clearGradients(JNIEnv *env, jint channel);

	void setTgtDimensions(int targetWidth, int targetHeight)
	{
		raycaster->setTgtDimensions(targetWidth, targetHeight);
	}

	void setBackground(JNIEnv *env, jintArray data, jint w, jint h);

	void clearBackground(JNIEnv *env);

	void white(JNIEnv *env, jint channel);

	void setKernel(JNIEnv *env, const char *kernel);

	jintArray project(
		JNIEnv *env,
		jfloatArray inverseTransform,
		jfloat alphacorr,
		jfloat combinedAlphaWeight,
		jobjectArray channelSettings,
		jint bgred, jint bggreen, jint bgblue);
};


#endif

