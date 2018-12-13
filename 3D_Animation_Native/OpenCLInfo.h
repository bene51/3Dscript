#ifndef OPENCL_INFO_H
#define OPENCL_INFO_H

#ifdef __APPLE__
#include <OpenCL/opencl.h>
#else
#include <CL/cl.h>
#endif

class OpenCLInfo
{

private:
	int w_max;

public:
	OpenCLInfo() {};

	~OpenCLInfo() {};

	void printInfo(cl_context context, cl_device_id device);
};

#endif

