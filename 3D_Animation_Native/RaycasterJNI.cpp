#include "RaycasterJNI.h"
#include "Raycaster.h"
#include "BasicOpenCLJNI.h"

template<typename T>
RaycasterJNI<T>::RaycasterJNI(
		JNIEnv *env,
		jint nChannels,
		jint width,
		jint height,
		jint depth,
		jint tgtWidth, jint tgtHeight) :
	env_(env)
{
	cArray_ = new T**[nChannels];
	int l = width * height;
	for(int i = 0; i < nChannels; i++) {
		cArray_[i] = new T*[depth];
		for(int z = 0; z < depth; z++) {
			cArray_[i][z] = new T[l];
			if(!cArray_[i][z]) {
				ThrowException(env, "Not enough memory");
				return;
			}
		}
	}

	raycaster = new Raycaster<T>(nChannels, width, height, depth,
			tgtWidth, tgtHeight);
}

template<typename T>
void
RaycasterJNI<T>::setColorLUT(
		JNIEnv *env,
		jint channel,
		jintArray lut)
{
	int l = env->GetArrayLength(lut);

	unsigned int *cArray = new unsigned int[l];
	if(!cArray) {
		ThrowException(env, "Not enough memory");
		return;
	}
	env->GetIntArrayRegion(lut, 0, l, (jint *)cArray);
	raycaster->setColorLUT(channel, cArray, l);
}

template<typename T>
void
RaycasterJNI<T>::clearColorLUT(
		JNIEnv *env,
		jint channel)
{
	raycaster->clearColorLUT(channel);
}

template<typename T>
void
RaycasterJNI<T>::calculateGradients(
		JNIEnv *env,
		jint channel,
		jfloat dzByDx)
{
	raycaster->calculateGradients(channel, dzByDx);
}

template<typename T>
void
RaycasterJNI<T>::clearGradients(
		JNIEnv *env,
		jint channel)
{
	raycaster->clearGradients(channel);
}

template<typename T>
void
RaycasterJNI<T>::setTexture(
		JNIEnv *env,
		jint channel,
		jobjectArray data)
{
	int depth = raycaster->getDepth();
	for(int z = 0; z < depth; z++)
		initPlane(env, data, channel, z);

	raycaster->setTexture(channel, cArray_[channel]);
}

template<typename T>
void
RaycasterJNI<T>::setBackground(JNIEnv *env, jintArray data, jint w, jint h)
{
	unsigned int *cArray = new unsigned int[w * h];
	if(!cArray) {
		ThrowException(env, "Not enough memory");
		return;
	}
	env->GetIntArrayRegion(data, 0, w * h, (jint *)cArray);
	raycaster->setBackground(cArray, w, h);
}

template<typename T>
void
RaycasterJNI<T>::clearBackground(JNIEnv *env)
{
	raycaster->clearBackground();
}

// template version of initPlane
template<typename T>
void
RaycasterJNI<T>::initPlane(
		JNIEnv *env,
		jobjectArray data,
		int channel,
		int z)
{
	printf("error\n");
}

// explicit specification of initPlane for 8-bit data
template<>
void
RaycasterJNI<unsigned char>::initPlane(
		JNIEnv *env,
		jobjectArray data,
		int channel,
		int z)
{
	jbyteArray jarray = (jbyteArray)env->GetObjectArrayElement(data, z);
	int l = env->GetArrayLength(jarray);
	/*
	cArray_[channel][z] = new unsigned char[l];
	if(!cArray_[channel][z]) {
		ThrowException(env, "Not enough memory");
		return;
	}
	*/
	env->GetByteArrayRegion(jarray, 0, l, (jbyte *)cArray_[channel][z]);
}

// explicit specification of initPlane for 16-bit data
template<>
void
RaycasterJNI<unsigned short>::initPlane(
	JNIEnv *env,
		jobjectArray data,
		int channel,
		int z)
{
	jshortArray jarray = (jshortArray)env->GetObjectArrayElement(data, z);
	int l = env->GetArrayLength(jarray);
	/*
	cArray_[channel][z] = new unsigned short[l];
	if(!cArray_[channel][z]) {
		ThrowException(env, "Not enough memory");
		return;
	}
	*/
	env->GetShortArrayRegion(jarray, 0, l, (jshort *)cArray_[channel][z]);
}

// template version of deletePlane
template<typename T>
void
RaycasterJNI<T>::deletePlane(JNIEnv *env, int channel, int z)
{
	printf("error\n");
}

// explicit specification of deletePlane for 8-bit data
template<>
void
RaycasterJNI<unsigned char>::deletePlane(JNIEnv *env, int channel, int z)
{
	delete cArray_[channel][z];
}

// explicit specification of deletePlane for 16-bit data
template<>
void
RaycasterJNI<unsigned short>::deletePlane(JNIEnv *env, int channel, int z)
{
	delete cArray_[channel][z];
}

template<typename T>
void RaycasterJNI<T>::setKernel(JNIEnv *env, const char *kernel)
{
	raycaster->setKernel(kernel);
}

template<typename T>
void RaycasterJNI<T>::white(JNIEnv *env, jint channel)
{
	raycaster->white(channel);
}

template<typename T>
jintArray RaycasterJNI<T>::project(
		JNIEnv *env,
		jfloatArray inverseTransform,
		jfloat alphacorr,
		jobjectArray channelSettings,
		jint bgred, jint bggreen, jint bgblue)
{
	int nChannels = env->GetArrayLength(channelSettings);
	ChannelInfo **ci = new ChannelInfo*[nChannels];
	for(int ch = 0; ch < nChannels; ch++) {
		jfloatArray jarray = (jfloatArray)env->GetObjectArrayElement(channelSettings, ch);
		float *floats = (float *)env->GetFloatArrayElements(jarray, NULL);
		// printf("%f, %f, %f, %f, %f, %f\n", floats[0], floats[1], floats[2], floats[3], floats[4], floats[5]);
		ci[ch] = new ChannelInfo(
				floats[0], floats[1], floats[2], // cmin, cmax, cgamma
				floats[3], floats[4], floats[5], // amin, amax, agamma
				floats[6], // weight
				(int)floats[7], (int)floats[8], (int)floats[9],    // bb0
				(int)floats[10], (int)floats[11], (int)floats[12], // bb1
				floats[13], floats[14],
				(int)floats[15], (int)floats[16], (int)floats[17], // r, g, b
				(int)floats[18], // use light
				floats[19], floats[20], floats[21], floats[22] // k_o, k_d, k_s, shininess
				);
		env->ReleaseFloatArrayElements(jarray, floats, JNI_ABORT);
	}

	float *mat = (float *)env->GetFloatArrayElements(inverseTransform, NULL);

	const unsigned int * theresult = raycaster->cast(mat, alphacorr, ci, bgred, bggreen, bgblue);

	env->ReleaseFloatArrayElements(inverseTransform, mat, JNI_ABORT);
	for(int ch = 0; ch < nChannels; ch++)
		delete ci[ch];
	delete ci;

	jsize wh = raycaster->getTgtWidth() * raycaster->getTgtHeight();
	jintArray ret = env->NewIntArray(wh);
	env->SetIntArrayRegion(ret, 0, wh, (jint *)theresult);
	return ret;
}

template<typename T>
RaycasterJNI<T>::~RaycasterJNI()
{
	printf("~RaycasterJNI()\n");
	fflush(stdout);
	if(raycaster == NULL)
		return;
	for(int i = 0; i < raycaster->getNChannels(); i++) {
		for(int z = 0; z < raycaster->getDepth(); z++)
			deletePlane(env_, i, z);
		delete[] cArray_[i];
		cArray_[i] = NULL;
	}
	delete[] cArray_;
	delete raycaster;
	cArray_ = NULL;
	raycaster = NULL;
}

// explicit template instantiation
template class RaycasterJNI<unsigned char>;
template class RaycasterJNI<unsigned short>;

