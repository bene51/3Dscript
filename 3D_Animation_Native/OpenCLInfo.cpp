#ifdef _WIN32
#define _CRT_SECURE_NO_DEPRECATE
#endif

#include "OpenCLInfo.h"

#include <stdio.h>
#include <string.h>
#include <iostream>
#include <fstream>

using namespace std;

static void print_data_type(cl_channel_type type, ofstream& log)
{
	switch(type) {
	case CL_SNORM_INT8: log << "CL_SNORM_INT8"; break;
	case CL_SNORM_INT16: log << "CL_SNORM_INT16"; break;
	case CL_UNORM_INT8: log << "CL_UNORM_INT8"; break;
	case CL_UNORM_INT16: log << "CL_UNORM_INT16"; break;
	case CL_UNORM_SHORT_565: log << "CL_UNORM_SHORT_565"; break;
	case CL_UNORM_SHORT_555: log << "CL_UNORM_SHORT_555"; break;
	case CL_UNORM_INT_101010: log << "CL_UNORM_INT_101010"; break;
	case CL_SIGNED_INT8: log << "CL_SIGNED_INT8"; break;
	case CL_SIGNED_INT16: log << "CL_SIGNED_INT16"; break;
	case CL_SIGNED_INT32: log << "CL_SIGNED_INT32"; break;
	case CL_UNSIGNED_INT8: log << "CL_UNSIGNED_INT8"; break;
	case CL_UNSIGNED_INT16: log << "CL_UNSIGNED_INT16"; break;
	case CL_UNSIGNED_INT32: log << "CL_UNSIGNED_INT32"; break;
	case CL_HALF_FLOAT: log << "CL_HALF_FLOAT"; break;
	case CL_FLOAT: log << "CL_FLOAT"; break;
	case CL_UNORM_INT24: log << "CL_UNORM_INT24"; break;
	default: log << "unknown"; break;
	}
}

static void print_channel_order(cl_channel_order order, ofstream& log)
{
	switch(order) {
	case CL_R: log << "CL_R"; break;
	case CL_A: log << "CL_A"; break;
	case CL_RG: log << "CL_RG"; break;
	case CL_RA: log << "CL_RA"; break;
	case CL_RGB: log << "CL_RGB"; break;
	case CL_RGBA: log << "CL_RGBA"; break;
	case CL_BGRA: log << "CL_BGRA"; break;
	case CL_ARGB: log << "CL_ARGB"; break;
	case CL_INTENSITY: log << "CL_INTENSITY"; break;
	case CL_LUMINANCE: log << "CL_LUMINANCE"; break;
	case CL_Rx: log << "CL_Rx"; break;
	case CL_RGx: log << "CL_RGx"; break;
	case CL_RGBx: log << "CL_RGBx"; break;
	case CL_DEPTH: log << "CL_DEPTH"; break;
	case CL_DEPTH_STENCIL: log << "CL_DEPTH_STENCIL"; break;
	default: log << "unknown"; break;
	}
}

void
OpenCLInfo::printInfo(cl_context context, cl_device_id device)
{
	ofstream log("OpenCL-log.txt");

	char valueString[1024];
	memset(valueString, 0, 1024);
	cl_bool valueBool;
	cl_ulong valueULong;
	cl_uint valueUInt;
	size_t valueSize;


	size_t size_ret;
	cl_int err;

	err = clGetDeviceInfo(device, CL_DEVICE_AVAILABLE, sizeof(valueBool), &valueBool, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DEVICE_AVAILABLE" << endl;
	else
		log << "CL_DEVICE_AVAILABLE: " << valueBool << endl;

	err = clGetDeviceInfo(device, CL_DEVICE_COMPILER_AVAILABLE, sizeof(valueBool), &valueBool, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DEVICE_COMPILER_AVAILABLE" << endl;
	else
		log << "CL_DEVICE_COMPILER_AVAILABLE: " << valueBool << endl;


	err = clGetDeviceInfo(device, CL_DEVICE_ENDIAN_LITTLE, sizeof(valueBool), &valueBool, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DEVICE_ENDIAN_LITTLE" << endl;
	else
		log << "CL_DEVICE_ENDIAN_LITTLE: " << valueBool << endl;


	err = clGetDeviceInfo(device, CL_DEVICE_EXTENSIONS, sizeof(valueString), &valueString, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DEVICE_EXTENSIONS" << endl;
	else
		log << "CL_DEVICE_EXTENSIONS: " << valueString << endl;

	err = clGetDeviceInfo(device, CL_DEVICE_GLOBAL_MEM_SIZE, sizeof(valueULong), &valueULong, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DEVICE_GLOBAL_MEM_SIZE err = " << err << endl;
	else
		log << "CL_DEVICE_GLOBAL_MEM_SIZE: " << valueULong << endl;


	err = clGetDeviceInfo(device, CL_DEVICE_IMAGE_SUPPORT, sizeof(valueBool), &valueBool, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DEVICE_IMAGE_SUPPORT" << endl;
	else
		log << "CL_DEVICE_IMAGE_SUPPORT: " << valueBool << endl;


	err = clGetDeviceInfo(device, CL_DEVICE_IMAGE2D_MAX_HEIGHT, sizeof(valueSize), &valueSize, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DEVICE_IMAGE2D_MAX_HEIGHT" << endl;
	else
		log << "CL_DEVICE_IMAGE2D_MAX_HEIGHT: " << valueSize << endl;


	err = clGetDeviceInfo(device, CL_DEVICE_IMAGE2D_MAX_WIDTH, sizeof(valueSize), &valueSize, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DEVICE_IMAGE2D_MAX_WIDTH" << endl;
	else
		log << "CL_DEVICE_IMAGE2D_MAX_WIDTH: " << valueSize << endl;


	err = clGetDeviceInfo(device, CL_DEVICE_IMAGE3D_MAX_DEPTH, sizeof(valueSize), &valueSize, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DEVICE_IMAGE3D_MAX_DEPTH" << endl;
	else
		log << "CL_DEVICE_IMAGE3D_MAX_DEPTH: " << valueSize << endl;

	err = clGetDeviceInfo(device, CL_DEVICE_IMAGE3D_MAX_HEIGHT, sizeof(valueSize), &valueSize, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DEVICE_IMAGE3D_MAX_HEIGHT" << endl;
	else
		log << "CL_DEVICE_IMAGE3D_MAX_HEIGHT: " << valueSize << endl;

	err = clGetDeviceInfo(device, CL_DEVICE_IMAGE3D_MAX_WIDTH, sizeof(valueSize), &valueSize, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DEVICE_IMAGE3D_MAX_WIDTH" << endl;
	else
		log << "CL_DEVICE_IMAGE3D_MAX_WIDTH: " << valueSize << endl;


	err = clGetDeviceInfo(device, CL_DEVICE_LOCAL_MEM_SIZE, sizeof(valueULong), &valueULong, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DEVICE_LOCAL_MEM_SIZE" << endl;
	else
		log << "CL_DEVICE_LOCAL_MEM_SIZE: " << valueULong << endl;


	err = clGetDeviceInfo(device, CL_DEVICE_MAX_COMPUTE_UNITS, sizeof(valueUInt), &valueUInt, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DEVICE_MAX_COMPUTE_UNITS" << endl;
	else
		log << "CL_DEVICE_MAX_COMPUTE_UNITS: " << valueUInt << endl;

	err = clGetDeviceInfo(device, CL_DEVICE_MAX_MEM_ALLOC_SIZE, sizeof(valueULong), &valueULong, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DEVICE_MAX_MEM_ALLOC_SIZE" << endl;
	else
		log << "CL_DEVICE_MAX_MEM_ALLOC_SIZE: " << valueULong << endl;

	err = clGetDeviceInfo(device, CL_DEVICE_MAX_SAMPLERS, sizeof(valueUInt), &valueUInt, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DEVICE_MAX_SAMPLERS" << endl;
	else
		log << "CL_DEVICE_MAX_SAMPLERS: " << valueUInt << endl;


	err = clGetDeviceInfo(device, CL_DEVICE_MAX_WORK_GROUP_SIZE, sizeof(valueSize), &valueSize, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DEVICE_MAX_WORK_GROUP_SIZE" << endl;
	else
		log << "CL_DEVICE_MAX_WORK_GROUP_SIZE: " << valueSize << endl;

	err = clGetDeviceInfo(device, CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS, sizeof(valueUInt), &valueUInt, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS" << endl;
	else
		log << "CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS: " << valueUInt << endl;


	size_t *wis = new size_t[valueUInt];
	err = clGetDeviceInfo(device, CL_DEVICE_MAX_WORK_ITEM_SIZES, valueUInt * sizeof(size_t), wis, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DEVICE_MAX_WORK_ITEM_SIZES" << endl;
	else {
		for(unsigned int i = 0; i < valueUInt; i++)
			log << "CL_DEVICE_MAX_WORK_ITEM_SIZES[" << i << "]: " << wis[i] << endl;
	}
	delete[] wis;


	memset(valueString, 0, 1024);
	err = clGetDeviceInfo(device, CL_DEVICE_NAME, sizeof(valueString), &valueString, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DEVICE_NAME" << endl;
	else
		log << "CL_DEVICE_NAME:" << valueString << endl;

	memset(valueString, 0, 1024);
	err = clGetDeviceInfo(device, CL_DEVICE_PROFILE, sizeof(valueString), &valueString, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DEVICE_PROFILE" << endl;
	else
		log << "CL_DEVICE_PROFILE:" << valueString << endl;



	cl_device_type type;
	err = clGetDeviceInfo(device, CL_DEVICE_TYPE, sizeof(type), &type, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DEVICE_TYPE" << endl;
	else {
		if(type == CL_DEVICE_TYPE_GPU)
			log << "CL_DEVICE_TYPE: " << valueString << endl;
		else
			log << "CL_DEVICE_TYPE is NOT GPU" << endl;
	}



	memset(valueString, 0, 1024);
	err = clGetDeviceInfo(device, CL_DEVICE_VENDOR, sizeof(valueString), &valueString, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DEVICE_VENDOR" << endl;
	else
		log << "CL_DEVICE_VENDOR: " << valueString << endl;


	memset(valueString, 0, 1024);
	err = clGetDeviceInfo(device, CL_DEVICE_VERSION, sizeof(valueString), &valueString, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DEVICE_VERSION" << endl;
	else
		log << "CL_DEVICE_VERSION: " << valueString << endl;

	memset(valueString, 0, 1024);
	err = clGetDeviceInfo(device, CL_DRIVER_VERSION, sizeof(valueString), &valueString, &size_ret);
	if(err != CL_SUCCESS)
		log << "Unable to query CL_DRIVER_VERSION" << endl;
	else
		log << "CL_DRIVER_VERSION: " << valueString << endl;



	 cl_image_format supported_formats[100];
	 cl_uint n_formats;

	 /* This is for the input volume */
	 cl_mem_object_type image_type = CL_MEM_OBJECT_IMAGE3D;
	 cl_mem_flags flags = CL_MEM_READ_WRITE;
	 err = clGetSupportedImageFormats (context,
		flags,
		image_type,
		100,
		supported_formats,
		&n_formats);
	bool found8 = false;
	bool found16 = false;
	bool found = false;
	log << "Supported formats: " << endl;
	for(unsigned int i = 0; i < n_formats; i++) {
		cl_image_format f = supported_formats[i];
		print_channel_order(f.image_channel_order, log);
		log << ", ";
		print_data_type(f.image_channel_data_type, log);
		log << endl;
		if(f.image_channel_order == CL_R && f.image_channel_data_type == CL_UNORM_INT8)
			found8 = true;
		if(f.image_channel_order == CL_R && f.image_channel_data_type == CL_UNORM_INT16)
			found16 = true;
		if(f.image_channel_order == CL_RGBA && f.image_channel_data_type == CL_SIGNED_INT8)
			found = true;
	}
	if(!found8)
		log << "8-bit image format not supported by your OpenCL implementation" << endl;
	else
		log << "8-bit image format supported by your OpenCL implementation" << endl;
	if(!found16)
		log << "16-bit image format not supported by your OpenCL implementation" << endl;
	else
		log << "16-bit image format supported by your OpenCL implementation" << endl;
	if(!found)
		log << "Signed int8 RGBA image format not supported by your OpenCL implementation" << endl;
	else
		log << "Signed int8 RGBA image format supported by your OpenCL implementation" << endl;

	log.close();
}


