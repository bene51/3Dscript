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

void
setOpenCLWarningHandler(JNIEnv *env)
{
	setWarningHandler(ShowWarning, env);
}


void
ShowWarning(void *env_ptr, const char *message)
{
	JNIEnv *env = (JNIEnv *)env_ptr;
	jclass cl;
	cl = env->FindClass("ij/IJ");
	jmethodID mid = env->GetStaticMethodID(cl, "showMessage", "(Ljava/lang/String;)V");
	jstring jmsg = env->NewStringUTF(message);
	env->CallStaticVoidMethod(cl, mid, jmsg);
}


