#ifdef _WIN32
#define _CRT_SECURE_NO_DEPRECATE
#endif

#include "Raycaster.h"
#include "ChannelInfo.h"

#include <stdio.h>
// #include <string.h>
#include <math.h>

#ifdef _WIN32
#include <windows.h>
#endif

// #include "RaycasterKernels.h"
#include "OpenCLUtils.h"
#include "OpenCLInfo.h"

#define GRADIENT_MODE_ONTHEFLY           0
#define GRADIENT_MODE_TEXTURE            1
#define GRADIENT_MODE_DOWNSAMPED_TEXTURE 2

#define GRADIENT_MODE GRADIENT_MODE_DOWNSAMPED_TEXTURE
// #define GRADIENT_MODE GRADIENT_MODE_TEXTURE
// #define GRADIENT_MODE GRADIENT_MODE_ONTHEFLY

static const char *
loadText(const char *file)
{
	FILE *fp;
	char *source_str;
	size_t program_size;

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
	clReleaseKernel(box_kernel);
	clReleaseKernel(erode_kernel);
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
        if (clGetPlatformIDs(1, &platform_id, NULL)!= CL_SUCCESS)
		clexception("Unable to get platform id");

        // try to get a supported GPU device
        if (clGetDeviceIDs(platform_id, CL_DEVICE_TYPE_GPU, 1, &device_id, NULL) != CL_SUCCESS)
		clexception("Cannot find any OpenCL capable GPU. Does your graphics card support OpenCL? You might want to try installing the newest drivers.");

        // context properties list - must be terminated with 0
	cl_context_properties properties[3];
        properties[0]= CL_CONTEXT_PLATFORM;
        properties[1]= (cl_context_properties) platform_id;
        properties[2]= 0;

        // create a context with the GPU device
        context = clCreateContext(properties, 1, &device_id, NULL, NULL, &err);
	checkOpenCLErrors(err);

	OpenCLInfo info;
	info.printInfo(context, device_id);


        // create command queue using the context and device
        command_queue = clCreateCommandQueue(context, device_id, 0, &err);
	checkOpenCLErrors(err);

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
	colorLUT_ = new cl_mem[nChannels];

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
	cl_image_format format = {CL_R, chtype};
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
		colorLUT_[channel] = NULL;
		gradients_[channel] = NULL;
	}


	lisampler = clCreateSampler(context,
	                CL_FALSE,         // cl_bool normalized_coords,
			CL_ADDRESS_CLAMP, // cl_addressing_mode addressing_mode,
			CL_FILTER_LINEAR, // cl_filter_mode filter_mode,
			&err);
	checkOpenCLErrors(err);

	nnsampler = clCreateSampler(context,
	                CL_FALSE,         // cl_bool normalized_coords,
			CL_ADDRESS_CLAMP, // cl_addressing_mode addressing_mode,
			CL_FILTER_NEAREST, // cl_filter_mode filter_mode,
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
//	cl_int err = 0;
//	if(background_ != NULL)
//		clearBackground();
//
//	cl_channel_type chtype = CL_UNSIGNED_INT8; // CL_UNORM_INT8;
//
//	cl_image_format format = {CL_RGBA, chtype};
//	cl_image_desc desc = {
//			CL_MEM_OBJECT_IMAGE2D,
//			w, h, 1,
//			0, // image_array_size
//			0, // image_row_pitch
//			0, // image_slice_pitch
//			0, // num_mip_levels
//			0, // num_samples
//			NULL}; // buffer (must be NULL)
//	background_ = clCreateImage(context, CL_MEM_READ_ONLY, &format, &desc, NULL, &err);
//	checkOpenCLErrors(err);
//
//	// copy data to 2D array
//	size_t origin[3] = {0, 0, 0};
//	size_t region[3] = {w, h, 1};
//	checkOpenCLErrors(clEnqueueWriteImage(
//		command_queue,
//		background_,
//		CL_TRUE, // blocking
//		origin,
//		region,
//		0, // input_row_pitch
//		0, // input_slice_pitch
//		data,
//		0,
//		NULL,
//		NULL));
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
}

template<typename T>
void
Raycaster<T>::calculateGradients(int channel, float dzByDx)
{
	bool do_erode = false;
	bool do_smooth = false;
	cl_mem smoothed = NULL;
	cl_int err;

	if(do_smooth) {
		cl_image_format format = {CL_R, CL_FLOAT};
		cl_image_desc desc = {
			CL_MEM_OBJECT_IMAGE3D,
			(size_t)(dataWidth_), (size_t)(dataHeight_), (size_t)(dataDepth_),
			0, // image_array_size
			0, // image_row_pitch
			0, // image_slice_pitch
			0, // num_mip_levels
			0, // num_samples
			NULL}; // buffer (must be NULL)

		smoothed = clCreateImage(context, CL_MEM_READ_WRITE, &format, &desc, NULL, &err);
		checkOpenCLErrors(err);
		smooth(texture_[channel], smoothed);
	}
	else {
		smoothed = texture_[channel];
	}

	// bool do_erode = true;
	cl_mem eroded = NULL;
	if(do_erode) {
		cl_channel_type chtype = sizeof(T) == 1 ? CL_UNORM_INT8 : CL_UNORM_INT16;
		cl_image_format format = {CL_R, chtype};
		cl_image_desc desc = {
			CL_MEM_OBJECT_IMAGE3D,
			(size_t)(dataWidth_), (size_t)(dataHeight_), (size_t)(dataDepth_),
			0, // image_array_size
			0, // image_row_pitch
			0, // image_slice_pitch
			0, // num_mip_levels
			0, // num_samples
			NULL}; // buffer (must be NULL)

		cl_int err;
		eroded = clCreateImage(context, CL_MEM_READ_WRITE, &format, &desc, NULL, &err);
		checkOpenCLErrors(err);
		erode(texture_[channel], eroded);
		erode(eroded, texture_[channel]);
		clReleaseMemObject(eroded);
		// clReleaseMemObject(texture_[channel]);
		// texture_[channel] = eroded;
	}


#if GRADIENT_MODE == GRADIENT_MODE_DOWNSAMPED_TEXTURE
	cl_int3 grad_size = {dataWidth_ / 2, dataHeight_ / 2, dataDepth_ / 2};
#elif GRADIENT_MODE == GRADIENT_MODE_TEXTURE
	cl_int3 grad_size = {dataWidth_, dataHeight_, dataDepth_};
#elif GRADIENT_MODE == GRADIENT_MODE_ONTHEFLY
	cl_int3 grad_size = {dataWidth_, dataHeight_, dataDepth_};
#endif


	// TODO reuse
	if(gradients_[channel] != NULL)
		clearGradients(channel);

	// initialize gradient textures
	cl_image_format gformat = {CL_RGBA, CL_SIGNED_INT8};
	cl_image_desc gdesc = {
		CL_MEM_OBJECT_IMAGE3D,
		(size_t)(grad_size.x), (size_t)(grad_size.y), (size_t)(grad_size.z),
		0, // image_array_size
		0, // image_row_pitch
		0, // image_slice_pitch
		0, // num_mip_levels
		0, // num_samples
		NULL}; // buffer (must be NULL)

	gradients_[channel] = clCreateImage(context, CL_MEM_READ_WRITE, &gformat, &gdesc, NULL, &err);
	printf("Created gradient for channel %d\n", channel);
	checkOpenCLErrors(err);

	checkOpenCLErrors(clSetKernelArg(grad_kernel, 0, sizeof(cl_mem), &smoothed));
	checkOpenCLErrors(clSetKernelArg(grad_kernel, 1, sizeof(cl_sampler), &lisampler));
	checkOpenCLErrors(clSetKernelArg(grad_kernel, 2, sizeof(cl_mem), &gradients_[channel]));
	checkOpenCLErrors(clSetKernelArg(grad_kernel, 3, sizeof(cl_int3), &grad_size));
	checkOpenCLErrors(clSetKernelArg(grad_kernel, 4, sizeof(cl_float), &dzByDx));

	const size_t global_work_size[3] = {
		grad_size.x,
		grad_size.y,
		grad_size.z,
	};

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

	if(do_smooth)
		clReleaseMemObject(smoothed);
}

template<typename T>
void
Raycaster<T>::clearGradients(int channel)
{
	if(gradients_[channel] != NULL) {
		clReleaseMemObject(gradients_[channel]);
		gradients_[channel] = NULL;
	}
}

template<typename T>
void
Raycaster<T>::smooth(cl_mem in, cl_mem out)
{
	cl_int3 out_size = {dataWidth_, dataHeight_, dataDepth_};

	checkOpenCLErrors(clSetKernelArg(box_kernel, 0, sizeof(cl_mem), &in));
	checkOpenCLErrors(clSetKernelArg(box_kernel, 1, sizeof(cl_sampler), &lisampler));
	checkOpenCLErrors(clSetKernelArg(box_kernel, 2, sizeof(cl_mem), &out));
	checkOpenCLErrors(clSetKernelArg(box_kernel, 3, sizeof(cl_int3), &out_size));

	const size_t global_work_size[3] = {
		dataWidth_,
		dataHeight_,
		dataDepth_
	};

	checkOpenCLErrors(clEnqueueNDRangeKernel(
			command_queue,
			box_kernel,
			3,                     // cl_uint work_dim,
			NULL,                  // const size_t *global_work_offset,
			global_work_size,       // const size_t *global_work_size
			NULL, // local_work_size,      // const size_t *local_work_size,
			0,                     // cl_uint num_events_in_wait_list,
			NULL,                  // const cl_event *event_wait_list,
			NULL));                // cl_event *event
}

template<typename T>
void
Raycaster<T>::erode(cl_mem in, cl_mem out)
{
	cl_int3 out_size = {dataWidth_, dataHeight_, dataDepth_};

	checkOpenCLErrors(clSetKernelArg(erode_kernel, 0, sizeof(cl_mem), &in));
	checkOpenCLErrors(clSetKernelArg(erode_kernel, 1, sizeof(cl_sampler), &lisampler));
	checkOpenCLErrors(clSetKernelArg(erode_kernel, 2, sizeof(cl_mem), &out));
	checkOpenCLErrors(clSetKernelArg(erode_kernel, 3, sizeof(cl_int3), &out_size));

	const size_t global_work_size[3] = {
		dataWidth_,
		dataHeight_,
		dataDepth_
	};

	checkOpenCLErrors(clEnqueueNDRangeKernel(
			command_queue,
			erode_kernel,
			3,                     // cl_uint work_dim,
			NULL,                  // const size_t *global_work_offset,
			global_work_size,       // const size_t *global_work_size
			NULL, // local_work_size,      // const size_t *local_work_size,
			0,                     // cl_uint num_events_in_wait_list,
			NULL,                  // const cl_event *event_wait_list,
			NULL));                // cl_event *event
}

template<typename T>
void
Raycaster<T>::clearColorLUT(int channel)
{
	if(colorLUT_[channel] != NULL) {
		clReleaseMemObject(colorLUT_[channel]);
		colorLUT_[channel] = NULL;
	}
}

template<typename T>
void
Raycaster<T>::setColorLUT(int channel, const unsigned int * const lut, int l)
{
	// int l = 2 << bitsPerSample_;
	cl_int err = 0;
	// TODO reuse
	if(colorLUT_[channel] != NULL)
		clearColorLUT(channel);

	cl_channel_type chtype = CL_UNSIGNED_INT8;

	cl_image_format format = {CL_RGBA, chtype};
	cl_mem tmp = clCreateBuffer(context,
			CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR | CL_MEM_HOST_WRITE_ONLY,
			l * sizeof(unsigned int),
			(void *)lut,
			&err);
	checkOpenCLErrors(err);

	cl_image_desc desc = {
			CL_MEM_OBJECT_IMAGE1D_BUFFER,
			l, 1, 1,
			0,
			0,
			0,
			0,
			0,
			tmp};
	// colorLUT_[channel] = clCreateImage(context, CL_MEM_READ_ONLY, &format, &desc, (void *)lut, &err);
	colorLUT_[channel] = clCreateImage(context, CL_MEM_READ_ONLY, &format, &desc, NULL, &err);
	checkOpenCLErrors(err);
}

template<typename T>
void
Raycaster<T>::setKernel(const char *programSource)
{
	printf("%s\n", programSource);
	fflush(stdout);
	if(program != NULL) {
		clReleaseProgram(program);
#if GRADIENT_MODE != GRADIENT_MODE_ONTHEFLY
		clReleaseKernel(clear_kernel);
		clReleaseKernel(kernel);
		clReleaseKernel(grad_kernel);
#endif
	}

	cl_int err;
        program = clCreateProgramWithSource(context,1,(const char **) &programSource, NULL, &err);
	checkOpenCLErrors(err);

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
		clexception("Error building OpenCL kernel. Please start from the command line to see detailed error messages");
	}

        // specify which kernel from the program to execute
        kernel = clCreateKernel(program, "raycastKernel", &err);
	checkOpenCLErrors(err);
	printf("kernel 'raycastKernel' built successfully\n");

        clear_kernel = clCreateKernel(program, "white", &err);
	checkOpenCLErrors(err);
	printf("kernel 'white' built successfully\n");

#if GRADIENT_MODE != GRADIENT_MODE_ONTHEFLY
        grad_kernel = clCreateKernel(program, "calculateGradients", &err);
	checkOpenCLErrors(err);
	printf("kernel 'calculateGradients' built successfully\n");
	box_kernel = clCreateKernel(program, "boxfilter", &err);
	checkOpenCLErrors(err);
	printf("kernel 'boxfilter' built successfully\n");
	erode_kernel = clCreateKernel(program, "erode", &err);
	checkOpenCLErrors(err);
	printf("kernel 'erode' built successfully\n");
#endif
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
		if(gradients_[c]) {
			checkOpenCLErrors(clSetKernelArg(kernel, argIdx++,  sizeof(cl_mem), &gradients_[c]));
		}
#endif
		checkOpenCLErrors(clSetKernelArg(kernel, argIdx++,  sizeof(cl_int3), &rgb));
		if(colorLUT_[c]) {
			checkOpenCLErrors(clSetKernelArg(kernel, argIdx++,  sizeof(cl_mem), &colorLUT_[c]));
		}
	}
	checkOpenCLErrors(clSetKernelArg(kernel, argIdx++, sizeof(cl_sampler), &lisampler));
	checkOpenCLErrors(clSetKernelArg(kernel, argIdx++, sizeof(cl_sampler), &nnsampler));
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

#ifdef _WIN32
	int end = GetTickCount();
	printf("needed %d ms\n", (end - start));
#endif
}

template<typename T>
Raycaster<T>::~Raycaster()
{
	printf("~Raycaster()\n");
	fflush(stdout);
	for(int i = 0; i < nChannels_; i++) {
		clReleaseMemObject(texture_[i]);
		clearGradients(i);
	}
	clReleaseMemObject(background_);
	clReleaseMemObject(d_inverseTransform_);
	clReleaseMemObject(d_result);
	clReleaseSampler(lisampler);
	clReleaseSampler(nnsampler);
	clReleaseSampler(bgsampler);
	delete[] texture_;
	delete[] gradients_;
	delete[] h_result;
	cleanup_opencl();
}

// excplicit template instantiation
template class Raycaster<unsigned char>;
template class Raycaster<unsigned short>;

