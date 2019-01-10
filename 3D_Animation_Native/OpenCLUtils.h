#ifndef OPENCL_UTILS_H
#define OPENCL_UTILS_H

#include <string>

void
setWarningHandler(void (*handler)(void *, const char *), void *param);

int
iDivUp(int a, int b);


#define checkOpenCLErrors(ans) {__clAssert((ans), __FILE__, __LINE__); }
void
__clAssert(unsigned int code, const char *file, int line);

#define clexception(text) {__clexception((text), __FILE__, __LINE__); }
void
__clexception(const char *text, const char *file, int line);

#define clwarning(text) {__clwarning((text), __FILE__, __LINE__); }
void
__clwarning(const std::string& text, const std::string& file, int line);

#endif

