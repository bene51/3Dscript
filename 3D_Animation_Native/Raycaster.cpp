#ifdef _WIN32
#define _CRT_SECURE_NO_DEPRECATE
#endif

#include "Raycaster.h"
#include "ChannelInfo.h"

#include <stdio.h>
#include <string.h>
#include <math.h>

#ifdef _WIN32
#include <windows.h>
#endif

// #include "RaycasterKernels.h"
#include "OpenCLUtils.h"

#define GRADIENT_MODE_ONTHEFLY           0
#define GRADIENT_MODE_TEXTURE            1
#define GRADIENT_MODE_DOWNSAMPED_TEXTURE 2

#define GRADIENT_MODE GRADIENT_MODE_DOWNSAMPED_TEXTURE

static const char *
loadText(const char *file)
{
	FILE *fp;
	char *source_str;
	size_t source_size, program_size;

	fp = fopen(file, "rb");
	if (!fp) {
	    printf("Failed to load kernel\n");
	    return NULL;
	}

	fseek(fp, 0, SEEK_END);
	program_size = ftell(fp);
	rewind(fp);
	source_str = (char*)malloc(program_size + 1);
	source_str[program_size] = '\0';
	fread(source_str, sizeof(char), program_size, fp);
	fclose(fp);

	return source_str;
}

template<typename T>
void
Raycaster<T>::cleanup_opencl()
{
	clReleaseProgram(program);
	clReleaseKernel(kernel);
	clReleaseKernel(clear_kernel);
#if GRADIENT_MODE != GRADIENT_MODE_ONTHEFLY
	clReleaseKernel(grad_kernel);
#endif
	clReleaseCommandQueue(command_queue);
	clReleaseContext(context);
}

template<typename T>
void
Raycaster<T>::init_opencl()
{

	cl_int err;
	cl_platform_id platform_id;

	// retreive a list of platforms avaible
        if (clGetPlatformIDs(1, &platform_id, NULL)!= CL_SUCCESS) {
                printf("Unable to get platform_id\n");
                return;
        }

        // try to get a supported GPU device
        if (clGetDeviceIDs(platform_id, CL_DEVICE_TYPE_GPU, 1, &device_id, NULL) != CL_SUCCESS) {
                printf("Unable to get device_id\n");
                return;
        }

	size_t maxWGSize = 0;
	clGetDeviceInfo(device_id,
			CL_DEVICE_MAX_WORK_GROUP_SIZE,
		  	sizeof(size_t),
			&maxWGSize,
			NULL);
	printf("max workgroup size = %d\n", maxWGSize);

	size_t maxImageWidth = 0;
	clGetDeviceInfo(device_id,
			CL_DEVICE_IMAGE2D_MAX_WIDTH,
		  	sizeof(size_t),
			&maxImageWidth,
			NULL);
	printf("max image width = %d\n", maxImageWidth);

	cl_ulong lmem = 0;
	clGetDeviceInfo(device_id,
			CL_DEVICE_LOCAL_MEM_SIZE,
		  	sizeof(cl_ulong),
			&lmem,
			NULL);
	printf("local memory = %d\n", lmem);

        // context properties list - must be terminated with 0
	cl_context_properties properties[3];
        properties[0]= CL_CONTEXT_PLATFORM;
        properties[1]= (cl_context_properties) platform_id;
        properties[2]= 0;

        // create a context with the GPU device
        context = clCreateContext(properties, 1, &device_id, NULL, NULL, &err);

        // create command queue using the context and device
        command_queue = clCreateCommandQueue(context, device_id, 0, &err);

	program = NULL;
}

template<typename T>
Raycaster<T>::Raycaster(
		int nChannels,
		int dataWidth, int dataHeight, int dataDepth,
		int targetWidth, int targetHeight) :
	nChannels_(nChannels),
	dataWidth_(dataWidth),
	dataHeight_(dataHeight),
	dataDepth_(dataDepth),
	targetWidth_(targetWidth),
	targetHeight_(targetHeight),
	bitsPerSample_(8 * sizeof(T)),
	planeSize_(targetWidth * targetHeight * sizeof(unsigned int)),
	background_(NULL),
	bgsampler(NULL)
{
	init_opencl();
	texture_ = new cl_mem[nChannels];
	gradients_ = new cl_mem[nChannels];

	// allocate host and device memory for the result
	cl_int err = CL_SUCCESS;
	d_result = clCreateBuffer(context, CL_MEM_WRITE_ONLY, planeSize_, NULL, &err);
	checkOpenCLErrors(err);
	h_result = new unsigned int[planeSize_];
	// TODO check h_result
	d_inverseTransform_ = clCreateBuffer(context, CL_MEM_READ_ONLY, 12 * sizeof(float), NULL, &err);
	checkOpenCLErrors(err);

	// initialize textures
	cl_channel_type chtype = sizeof(T) == 1 ? CL_UNORM_INT8 : CL_UNORM_INT16;
	cl_image_format format = {CL_INTENSITY, chtype};
	cl_image_desc desc = {
			CL_MEM_OBJECT_IMAGE3D,
			dataWidth_, dataHeight_, dataDepth_,
			0, // image_array_size
			0, // image_row_pitch
			0, // image_slice_pitch
			0, // num_mip_levels
			0, // num_samples
			NULL}; // buffer (must be NULL)
	for(int channel = 0; channel < nChannels; channel++) {
		texture_[channel] = clCreateImage(context, CL_MEM_READ_WRITE, &format, &desc, NULL, &err);
		checkOpenCLErrors(err);
	}

//	// initialize LUT textures
//	chtype = CL_FLOAT;
//	format = {CL_RA, chtype};
//	desc = {
//		CL_MEM_OBJECT_IMAGE1D,
//		(size_t)(dataWidth_), (size_t)(dataHeight_), (size_t)(dataDepth_),
//#elif GRADIENT_MODE == GRADIENT_MODE_DOWNSAMPED_TEXTURE
//		(size_t)(dataWidth_ / 2), (size_t)(dataHeight_ / 2), (size_t)(dataDepth_ / 2),
//#endif
//		0, // image_array_size
//		0, // image_row_pitch
//		0, // image_slice_pitch
//		0, // num_mip_levels
//		0, // num_samples
//		NULL}; // buffer (must be NULL)
//	for(int channel = 0; channel < nChannels; channel++) {
//		gradients_[channel] = clCreateImage(context, CL_MEM_READ_WRITE, &format, &desc, NULL, &err);
//		checkOpenCLErrors(err);
//	}

	// initialize gradient textures
#if GRADIENT_MODE != GRADIENT_MODE_ONTHEFLY
	chtype = CL_SIGNED_INT8;
	cl_image_format gformat = {CL_RGBA, chtype};
	cl_image_desc gdesc = {
		CL_MEM_OBJECT_IMAGE3D,
#if GRADIENT_MODE == GRADIENT_MODE_TEXTURE
		(size_t)(dataWidth_), (size_t)(dataHeight_), (size_t)(dataDepth_),
#elif GRADIENT_MODE == GRADIENT_MODE_DOWNSAMPED_TEXTURE
		(size_t)(dataWidth_ / 2), (size_t)(dataHeight_ / 2), (size_t)(dataDepth_ / 2),
#endif
		0, // image_array_size
		0, // image_row_pitch
		0, // image_slice_pitch
		0, // num_mip_levels
		0, // num_samples
		NULL}; // buffer (must be NULL)
	for(int channel = 0; channel < nChannels; channel++) {
		gradients_[channel] = clCreateImage(context, CL_MEM_READ_WRITE, &gformat, &gdesc, NULL, &err);
		checkOpenCLErrors(err);
	}
#endif
		

	sampler = clCreateSampler(context,
	                CL_FALSE,         // cl_bool normalized_coords,
			CL_ADDRESS_CLAMP, // cl_addressing_mode addressing_mode,
			CL_FILTER_LINEAR, // cl_filter_mode filter_mode,
			&err);
	checkOpenCLErrors(err);

	bgsampler = clCreateSampler(context,
	                CL_TRUE,         // cl_bool normalized_coords,
			CL_ADDRESS_CLAMP, // cl_addressing_mode addressing_mode,
			CL_FILTER_LINEAR, // cl_filter_mode filter_mode,
			&err);
	checkOpenCLErrors(err);
}

template<typename T>
void
Raycaster<T>::setTgtDimensions(int targetWidth, int targetHeight)
{
	cl_int err = 0;
	delete[] h_result;
	clReleaseMemObject(d_result);
	targetWidth_ = targetWidth;
	targetHeight_ = targetHeight;
	planeSize_ = targetWidth * targetHeight * sizeof(unsigned int);
	h_result = new unsigned int[planeSize_];
	// TODO check h_result
	d_result = clCreateBuffer(context, CL_MEM_WRITE_ONLY, planeSize_, NULL, &err);
	checkOpenCLErrors(err);
}

template<typename T>
void
Raycaster<T>::setBackground(const unsigned int * const data, int w, int h)
{
	cl_int err = 0;
	if(background_ != NULL)
		clearBackground();

	cl_channel_type chtype = CL_UNSIGNED_INT8; // CL_UNORM_INT8;

	cl_image_format format = {CL_RGBA, chtype};
	cl_image_desc desc = {
			CL_MEM_OBJECT_IMAGE2D,
			w, h, 1,
			0, // image_array_size
			0, // image_row_pitch
			0, // image_slice_pitch
			0, // num_mip_levels
			0, // num_samples
			NULL}; // buffer (must be NULL)
	background_ = clCreateImage(context, CL_MEM_READ_ONLY, &format, &desc, NULL, &err);
	checkOpenCLErrors(err);

	// copy data to 2D array
	size_t origin[3] = {0, 0, 0};
	size_t region[3] = {w, h, 1};
	checkOpenCLErrors(clEnqueueWriteImage(
		command_queue,
		background_,
		CL_TRUE, // blocking
		origin,
		region,
		0, // input_row_pitch
		0, // input_slice_pitch
		data,
		0,
		NULL,
		NULL));
}

template<typename T>
void
Raycaster<T>::clearBackground()
{
	if(background_ != NULL) {
		clReleaseMemObject(background_);
		background_ = NULL;
	}
}

template<typename T>
void
Raycaster<T>::setTexture(int channel, const T * const * const data)
{
	// copy data to 3D array
	for(int z = 0; z < dataDepth_; z++) {
		size_t origin[3] = {0, 0, z};
		size_t region[3] = {dataWidth_, dataHeight_, 1};
		checkOpenCLErrors(clEnqueueWriteImage(
			command_queue,
			texture_[channel],
			CL_TRUE, // blocking
			origin,
			region,
			0, // input_row_pitch
			0, // input_slice_pitch
			data[z],
			0,
			NULL,
			NULL));
	}
	// calculate gradient texture (kernel call)

#if GRADIENT_MODE != GRADIENT_MODE_ONTHEFLY
	cl_int3 grad_size = {dataWidth_ / 2, dataHeight_ / 2, dataDepth_ / 2};

	checkOpenCLErrors(clSetKernelArg(grad_kernel, 0, sizeof(cl_mem), &texture_[channel]));
	checkOpenCLErrors(clSetKernelArg(grad_kernel, 1, sizeof(cl_sampler), &sampler));
	checkOpenCLErrors(clSetKernelArg(grad_kernel, 2, sizeof(cl_mem), &gradients_[channel]));
	checkOpenCLErrors(clSetKernelArg(grad_kernel, 3, sizeof(cl_int3), &grad_size));

#if GRADIENT_MODE == GRADIENT_MODE_DOWNSAMPED_TEXTURE
	const size_t global_work_size[3] = {
		dataWidth_  / 2,
		dataHeight_ / 2,
		dataDepth_  / 2
	};
#elif GRADIENT_MODE == GRADIENT_MODE_TEXTURE
	const size_t global_work_size[3] = {
		dataWidth_,
		dataHeight_,
		dataDepth_
	};
#endif
	checkOpenCLErrors(clEnqueueNDRangeKernel(
			command_queue,
			grad_kernel,
			3,                     // cl_uint work_dim,
			NULL,                  // const size_t *global_work_offset,
			global_work_size,       // const size_t *global_work_size
			NULL, // local_work_size,      // const size_t *local_work_size,
			0,                     // cl_uint num_events_in_wait_list,
			NULL,                  // const cl_event *event_wait_list,
			NULL));                // cl_event *event

	// TODO wait for it to finish
#endif
}

template<typename T>
void
Raycaster<T>::setKernel(const char *programSource)
{
	printf("%s\n", programSource);
	fflush(stdout);
	if(program != NULL) {
		clReleaseProgram(program);
		clReleaseKernel(clear_kernel);
		clReleaseKernel(kernel);
#if GRADIENT_MODE != GRADIENT_MODE_ONTHEFLY
		clReleaseKernel(grad_kernel);
#endif
	}

	cl_int err;
        program = clCreateProgramWithSource(context,1,(const char **) &programSource, NULL, &err);

        // compile the program
        err = clBuildProgram(program, 0, NULL, NULL, NULL, NULL);
        if (err != CL_SUCCESS)
        {
		if (err == CL_BUILD_PROGRAM_FAILURE) {
			// Determine the size of the log
			size_t log_size;
			clGetProgramBuildInfo(program, device_id, CL_PROGRAM_BUILD_LOG, 0, NULL, &log_size);

			// Allocate memory for the log
			char *log = (char *) malloc(log_size);

			// Get the log
			clGetProgramBuildInfo(program, device_id, CL_PROGRAM_BUILD_LOG, log_size, log, NULL);

			// Print the log
			printf("%s\n", log);
		}
                printf("error = %d\n", err);
                printf("Error building program\n");
		fflush(stdout);
                return;
	}

        // specify which kernel from the program to execute
        kernel = clCreateKernel(program, "raycastKernel", &err);
        if (err != CL_SUCCESS) {
                printf("error = %d\n", err);
                printf("Error creating kernel\n");
                return;
        }
	printf("kernel 'raycastKernel' built successfully\n");

        clear_kernel = clCreateKernel(program, "white", &err);
        if (err != CL_SUCCESS) {
                printf("error = %d\n", err);
                printf("Error creating kernel\n");
                return;
        }
	printf("kernel 'white' built successfully\n");

#if GRADIENT_MODE != GRADIENT_MODE_ONTHEFLY
        grad_kernel = clCreateKernel(program, "calculateGradients", &err);
        if (err != CL_SUCCESS) {
                printf("error = %d\n", err);
                printf("Error creating kernel\n");
                return;
        }
	printf("kernel 'calculateGradients' built successfully\n");
#endif
	fflush(stdout);
}

template<typename T>
void
Raycaster<T>::updateLUT(int channel,
		float cmin, float cmax, float cgamma,
		float amin, float amax, float agamma)
{
}

template<typename T>
const unsigned int *
Raycaster<T>::cast(
		const float * const inverseTransform,
		float alphacorr,
		const ChannelInfo *const *const channels,
		int bgr, int bgg, int bgb)
{
#ifdef _WIN32
	int start = GetTickCount();
#endif

	cl_float16 inv = {
		inverseTransform[0], inverseTransform[1], inverseTransform[2], inverseTransform[3],
		inverseTransform[4], inverseTransform[5], inverseTransform[6], inverseTransform[7],
		inverseTransform[8], inverseTransform[9], inverseTransform[10], inverseTransform[11],
		0, 0, 0, 1};
	cl_int2 tgt_size = {targetWidth_, targetHeight_};
	cl_int3 background = {bgr, bgg, bgb};

	cl_float3 inc = {
		inverseTransform[2],
		inverseTransform[6],
		inverseTransform[10]
	};

	float len = sqrt(inc.x * inc.x + inc.y * inc.y + inc.z * inc.z);

	int useBackgroundTexture = 0;
	if(background_ != NULL)
		useBackgroundTexture = 1;

	int argIdx = 0;
	for(int c = 0; c < nChannels_; c++) {
		cl_int3 rgb = {channels[c]->r, channels[c]->g, channels[c]->b};
		checkOpenCLErrors(clSetKernelArg(kernel, argIdx++,  sizeof(cl_mem), &texture_[c]));
#if GRADIENT_MODE != GRADIENT_MODE_ONTHEFLY
		checkOpenCLErrors(clSetKernelArg(kernel, argIdx++,  sizeof(cl_mem), &gradients_[c]));
#endif
		checkOpenCLErrors(clSetKernelArg(kernel, argIdx++,  sizeof(cl_int3), &rgb));
	}
	checkOpenCLErrors(clSetKernelArg(kernel, argIdx++, sizeof(cl_sampler), &sampler));
	checkOpenCLErrors(clSetKernelArg(kernel, argIdx++, sizeof(cl_mem), &d_result));
	checkOpenCLErrors(clSetKernelArg(kernel, argIdx++, sizeof(cl_int2), &tgt_size));
	checkOpenCLErrors(clSetKernelArg(kernel, argIdx++, sizeof(cl_float16), &inv));
	for(int c = 0; c < nChannels_; c++) {
		float zstart = channels[c]->zStart / len;
		float zend   = channels[c]->zEnd / len;
		cl_int3 bb0 = {channels[c]->bbx0, channels[c]->bby0, channels[c]->bbz0};
		cl_int3 bb1 = {channels[c]->bbx1, channels[c]->bby1, channels[c]->bbz1};
		checkOpenCLErrors(clSetKernelArg(kernel, argIdx++,  sizeof(cl_float), &(channels[c]->amin)));
		checkOpenCLErrors(clSetKernelArg(kernel, argIdx++,  sizeof(cl_float), &(channels[c]->amax)));
		checkOpenCLErrors(clSetKernelArg(kernel, argIdx++,  sizeof(cl_float), &(channels[c]->agamma)));
		checkOpenCLErrors(clSetKernelArg(kernel, argIdx++,  sizeof(cl_float), &(channels[c]->cmin)));
		checkOpenCLErrors(clSetKernelArg(kernel, argIdx++,  sizeof(cl_float), &(channels[c]->cmax)));
		checkOpenCLErrors(clSetKernelArg(kernel, argIdx++,  sizeof(cl_float), &(channels[c]->cgamma)));
		checkOpenCLErrors(clSetKernelArg(kernel, argIdx++,  sizeof(cl_float), &(channels[c]->weight)));
		checkOpenCLErrors(clSetKernelArg(kernel, argIdx++,  sizeof(cl_int3), &bb0));
		checkOpenCLErrors(clSetKernelArg(kernel, argIdx++,  sizeof(cl_int3), &bb1));
		checkOpenCLErrors(clSetKernelArg(kernel, argIdx++,  sizeof(cl_float), &zstart));
		checkOpenCLErrors(clSetKernelArg(kernel, argIdx++,  sizeof(cl_float), &zend));
		cl_float4 light = {channels[c]->k_o, channels[c]->k_d, channels[c]->k_s, channels[c]->shininess};
		checkOpenCLErrors(clSetKernelArg(kernel, argIdx++, sizeof(cl_float4), &light));
	}
	checkOpenCLErrors(clSetKernelArg(kernel, argIdx++, sizeof(cl_float), &alphacorr));
	checkOpenCLErrors(clSetKernelArg(kernel, argIdx++, sizeof(cl_float3), &inc));

	if(background_ != NULL) {
		checkOpenCLErrors(clSetKernelArg(kernel, argIdx++, sizeof(cl_mem), &background_));
		checkOpenCLErrors(clSetKernelArg(kernel, argIdx++, sizeof(cl_sampler), &bgsampler));
	}
	else {
		checkOpenCLErrors(clSetKernelArg(kernel, argIdx++, sizeof(cl_int3), &background));
	}

	checkOpenCLErrors(clSetKernelArg(kernel, argIdx++, sizeof(cl_int), &bitsPerSample_));

	const size_t workgroups[2] = {32, 32};
	const size_t local_work_size[2] = {
		iDivUp(targetWidth_, workgroups[0]),
		iDivUp(targetHeight_, workgroups[1])
	};
	const size_t global_work_size[2] = {
		targetWidth_, // workgroups[0] * local_work_size[0],
		targetHeight_, // workgroups[1] * local_work_size[1]
	};

	const size_t total_work_size = local_work_size[0] * local_work_size[1];
	printf("total_work_size = %d\n", total_work_size);

	// TODO execute kernel
	checkOpenCLErrors(clEnqueueNDRangeKernel(
			command_queue,
			kernel,
			2,                     // cl_uint work_dim,
			NULL,                  // const size_t *global_work_offset,
			global_work_size,       // const size_t *global_work_size
			NULL, // local_work_size,      // const size_t *local_work_size,
			0,                     // cl_uint num_events_in_wait_list,
			NULL,                  // const cl_event *event_wait_list,
			NULL));                // cl_event *event



	checkOpenCLErrors(clEnqueueReadBuffer(
		command_queue,
		d_result,
		CL_TRUE, // blocking
		0, // offset
		planeSize_, // size in bytes
		h_result,
		0,
		NULL,
		NULL));

#ifdef _WIN32
	int end = GetTickCount();
	printf("needed %d ms\n", (end - start));
	fflush(stdout);
#endif
	return h_result;
}

template<typename T>
void
Raycaster<T>::white(int channel)
{
#ifdef _WIN32
	int start = GetTickCount();
#endif

	cl_int3 data_size = {dataWidth_, dataHeight_, dataDepth_};

	checkOpenCLErrors(clSetKernelArg(clear_kernel, 0, sizeof(cl_mem), &texture_[channel]));
	checkOpenCLErrors(clSetKernelArg(clear_kernel, 1, sizeof(cl_int3), &data_size));
	checkOpenCLErrors(clSetKernelArg(clear_kernel, 2, sizeof(cl_int), &bitsPerSample_));

	const size_t global_work_size[3] = {
		dataWidth_,
		dataHeight_,
		dataDepth_
	};

	checkOpenCLErrors(clEnqueueNDRangeKernel(
			command_queue,
			clear_kernel,
			3,                     // cl_uint work_dim,
			NULL,                  // const size_t *global_work_offset,
			global_work_size,       // const size_t *global_work_size
			NULL, // local_work_size,      // const size_t *local_work_size,
			0,                     // cl_uint num_events_in_wait_list,
			NULL,                  // const cl_event *event_wait_list,
			NULL));                // cl_event *event

	// TODO wait for it to finish


#ifdef _WIN32
	int end = GetTickCount();
	printf("needed %d ms\n", (end - start));
	fflush(stdout);
#endif
}

template<typename T>
Raycaster<T>::~Raycaster()
{
	printf("~Raycaster()\n");
	fflush(stdout);
	for(int i = 0; i < nChannels_; i++) {
		clReleaseMemObject(texture_[i]);
		clReleaseMemObject(gradients_[i]);
	}
	clReleaseMemObject(background_);
	clReleaseMemObject(d_inverseTransform_);
	clReleaseMemObject(d_result);
	clReleaseSampler(sampler);
	clReleaseSampler(bgsampler);
	delete[] texture_;
	delete[] gradients_;
	delete[] h_result;
	cleanup_opencl();
}

// excplicit template instantiation
template class Raycaster<unsigned char>;
template class Raycaster<unsigned short>;

