#ifndef BASIC_OPENCL_JNI_H
#define BASIC_OPENCL_JNI_H

#include <jni.h>

void
ThrowException(void *env_ptr, const char *message);

void
setOpenCLExceptionHandler(JNIEnv *env);


#endif
