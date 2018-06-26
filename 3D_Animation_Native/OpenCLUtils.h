#ifndef OPENCL_UTILS_H
#define OPENCL_UTILS_H

void
setErrorHandler(void (*handler)(void *, const char *), void *param);

int
iDivUp(int a, int b);


#define checkOpenCLErrors(ans) {__clAssert((ans), __FILE__, __LINE__); }
void
__clAssert(unsigned int code, const char *file, int line);



#endif

