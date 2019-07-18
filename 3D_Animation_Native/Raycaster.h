#ifndef RAYCASTER_H
#define RAYCASTER_H

#ifdef __APPLE__
#include <OpenCL/opencl.h>
#else
#include <CL/cl.h>
#endif

#include "ChannelInfo.h"

template<typename T>
class Raycaster
{

private:
	unsigned int *h_result;

	cl_mem d_result;

	const int nChannels_;

	const int dataWidth_, dataHeight_, dataDepth_;
	int targetWidth_, targetHeight_;

	int planeSize_;

	const int bitsPerSample_;

	cl_context context;
	cl_device_id device_id;
	cl_kernel kernel;
	cl_kernel clear_kernel;
	cl_kernel grad_kernel;
	cl_command_queue command_queue;
	cl_program program;

	cl_mem *texture_;
	cl_mem *gradients_;
	cl_mem *LUT_;
	cl_sampler sampler;

	cl_mem background_;
	cl_sampler bgsampler;

	cl_mem d_inverseTransform_;

	void init_opencl();
	void cleanup_opencl();

public:
	Raycaster(int nChannels,
			int dataWidth, int dataHeight, int dataDepth,
			int targetWidth, int targetHeight);
	~Raycaster();

	void setBackground(const unsigned int * const data, int w, int h);

	void clearBackground();

	void setTexture(int channel, const T * const * const data);

	void setTgtDimensions(int targetWidth, int targetHeight);

	int getTgtWidth() {
		return targetWidth_;
	}

	int getTgtHeight() {
		return targetHeight_;
	}

	int getWidth() {
		return dataWidth_;
	}

	int getHeight() {
		return dataHeight_;
	}

	int getDepth() {
		return dataDepth_;
	}

	int getNChannels() {
		return nChannels_;
	}

	void setKernel(const char *kernel);

	const unsigned int *
	cast(
		const float * const inverseTransform,
		float alphacorr,
		const ChannelInfo *const *const channels,
		int bgr, int bgg, int bgb);

	void
	white(int channel);
};

#endif

