#include "BasicOpenCLJNI.h"
#include "OpenCLUtils.h"

void
ThrowException(void *env_ptr, const char *message)
{
	JNIEnv *env = (JNIEnv *)env_ptr;
	jclass cl;
	cl = env->FindClass("java/lang/RuntimeException");
	env->ThrowNew(cl, message);
}


