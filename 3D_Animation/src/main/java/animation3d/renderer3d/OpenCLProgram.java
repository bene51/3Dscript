package animation3d.renderer3d;

public class OpenCLProgram {

//	public static final boolean useLights = false;

	public static final int GRADIENT_MODE_ONTHEFLY            = 0;
	public static final int GRADIENT_MODE_TEXTURE             = 1;
	public static final int GRADIENT_MODE_DOWNSAMPLED_TEXTURE = 2;

//	public static final int GRADIENT_MODE = GRADIENT_MODE_ONTHEFLY;
//	public static final int GRADIENT_MODE = GRADIENT_MODE_TEXTURE;
	public static final int GRADIENT_MODE = GRADIENT_MODE_DOWNSAMPLED_TEXTURE;

	public static void main(String[] args) {
		System.out.println(makeSource(2, false, true, false, new boolean[] {false, false}, new boolean[] {true, false}));
//		System.out.println(makeSourceForMIP(2, false));
	}

	@SuppressWarnings("unused")
	private static String makeCommonSource(int channels) {
		String source =
				"#pragma OPENCL EXTENSION cl_khr_3d_image_writes : enable\n" +
		        /* ****************************************************************
				 * intersects()
				 * ****************************************************************/
				"bool\n" +
				"intersects(float3 bb0, float3 bb1, float3 r0, float3 rd, float *i0, float *i1) {\n" +
				"\n" +
				"	int3 parallel = fabs(rd) < (float3)(10e-5);\n" +
				"	if(any(parallel && (r0 < bb0 || r0 > bb1)))\n" +
				"		return false;\n" +
				"\n" +
				"	float3 T1 = (bb0 - r0) / rd;\n" +
				"	float3 T2 = (bb1 - r0) / rd;\n" +
				"\n" +
				"	float3 Tnear = min(T1, T2);\n" +
				"	float3 Tfar = max(T1, T2);\n" +
				"\n" +
				"	*i0 = max(Tnear.x, max(Tnear.y, Tnear.z));\n" +
				"	*i1 = min(Tfar.x, min(Tfar.y, Tfar.z));\n" +
				"\n" +
				"	return (*i1 > *i0);\n" +
				"}\n" +
				"\n" +
				"\n" +
				"inline float3 multiplyMatrixVector(float16 m, float4 v)\n" +
				"{\n" +
				"    return (float3) (\n" +
				"	dot(m.s0123, v),\n" +
				"	dot(m.s4567, v),\n" +
				"	dot(m.s89AB, v)\n" +
				"    );\n" +
				"}\n" +
				"\n" +
		        /* ****************************************************************
				 * white()
				 * ****************************************************************/
				"kernel void\n" +
				"white(\n" +
				"		__write_only image3d_t texture,\n" +
				"		int3 data_size,\n" +
				"		int bitsPerSample)\n" +
				"{\n" +
				"	int x = get_global_id(0);\n" +
				"	int y = get_global_id(1);\n" +
				"	int z = get_global_id(2);\n" +
				"\n" +
				"	if(x < data_size.x && y < data_size.y && z < data_size.z)\n" +
				"		write_imagef(texture, (int4)(x, y, z, 0), (float4)(1));\n" +
				"}\n" +
				"\n";
		        /* ****************************************************************
				 * downsampled()
				 * ****************************************************************/
if(GRADIENT_MODE == GRADIENT_MODE_DOWNSAMPLED_TEXTURE) {
				// TODO take hasLUT into account
				source = source +
				"inline float downsampled(__read_only image3d_t texture, sampler_t sampler, int x, int y, int z)\n" +
				"{\n" +
				"		float x2 = x * 2.0f + 0.5f;\n" +
				"		float y2 = y * 2.0f + 0.5f;\n" +
				"		float z2 = z * 2.0f + 0.5f;\n" +
				"		return     (read_imagef(texture, sampler, (float4)(x2,     y2,     z2,     0)).x +\n" +
		        "					read_imagef(texture, sampler, (float4)(x2 + 1, y2,     z2,     0)).x +\n" +
		        "					read_imagef(texture, sampler, (float4)(x2,     y2 + 1, z2,     0)).x +\n" +
		        "					read_imagef(texture, sampler, (float4)(x2 + 1, y2 + 1, z2,     0)).x +\n" +
		        "					read_imagef(texture, sampler, (float4)(x2,     y2,     z2 + 1, 0)).x +\n" +
		        "					read_imagef(texture, sampler, (float4)(x2 + 1, y2,     z2 + 1, 0)).x +\n" +
		        "					read_imagef(texture, sampler, (float4)(x2,     y2 + 1, z2 + 1, 0)).x +\n" +
		        "					read_imagef(texture, sampler, (float4)(x2 + 1, y2 + 1, z2 + 1, 0)).x) / 8.0f;\n" +
				"}\n" +
				"\n";
} else if(GRADIENT_MODE == GRADIENT_MODE_TEXTURE) {
				source = source +
				"inline float downsampled(__read_only image3d_t texture, sampler_t sampler, int x, int y, int z)\n" +
				"{\n" +
				"		return read_imagef(texture, sampler, (float4)(x + 0.5f, y + 0.5f, z + 0.5f, 0)).x;\n" +
				"}\n" +
				"\n";
}
				source = source +
				"inline float sampled(__read_only image3d_t texture, sampler_t sampler, int x, int y, int z)\n" +
				"{\n" +
				"		return read_imagef(texture, sampler, (float4)(x + 0.5f, y + 0.5f, z + 0.5f, 0)).x;\n" +
				"}\n" +
				"\n";

if(GRADIENT_MODE == GRADIENT_MODE_TEXTURE || GRADIENT_MODE == GRADIENT_MODE_DOWNSAMPLED_TEXTURE) {
				/* ****************************************************************
				 * erode()
				 * ****************************************************************/
				source = source +
				"kernel void\n" +
				"erode(\n" +
				"			__read_only image3d_t image,\n" +
				"			sampler_t sampler,\n" +
				"			__write_only image3d_t out,\n" +
				"			int3 out_size)\n" +
				"{\n" +
				"	int x = get_global_id(0);\n" +
				"	int y = get_global_id(1);\n" +
				"	int z = get_global_id(2);\n" +
				"\n" +
				"	if(x < out_size.x && y < out_size.y && z < out_size.z) {\n" +
				"		bool a000 = sampled(image, sampler, x - 1, y - 1, z - 1) > 0;\n" +
				"		bool a001 = sampled(image, sampler, x,     y - 1, z - 1) > 0;\n" +
				"		bool a002 = sampled(image, sampler, x + 1, y - 1, z - 1) > 0;\n" +
				"		bool a010 = sampled(image, sampler, x - 1, y,     z - 1) > 0;\n" +
				"		bool a011 = sampled(image, sampler, x,     y,     z - 1) > 0;\n" +
				"		bool a012 = sampled(image, sampler, x + 1, y,     z - 1) > 0;\n" +
				"		bool a020 = sampled(image, sampler, x - 1, y + 1, z - 1) > 0;\n" +
				"		bool a021 = sampled(image, sampler, x,     y + 1, z - 1) > 0;\n" +
				"		bool a022 = sampled(image, sampler, x + 1, y + 1, z - 1) > 0;\n" +
				"		bool a100 = sampled(image, sampler, x - 1, y - 1, z) > 0;\n" +
				"		bool a101 = sampled(image, sampler, x,     y - 1, z) > 0;\n" +
				"		bool a102 = sampled(image, sampler, x + 1, y - 1, z) > 0;\n" +
				"		bool a110 = sampled(image, sampler, x - 1, y,     z) > 0;\n" +
				"		float a111 = sampled(image, sampler, x,     y,     z);\n" +
				"		bool a112 = sampled(image, sampler, x + 1, y,     z) > 0;\n" +
				"		bool a120 = sampled(image, sampler, x - 1, y + 1, z) > 0;\n" +
				"		bool a121 = sampled(image, sampler, x,     y + 1, z) > 0;\n" +
				"		bool a122 = sampled(image, sampler, x + 1, y + 1, z) > 0;\n" +
				"		bool a200 = sampled(image, sampler, x - 1, y - 1, z + 1) > 0;\n" +
				"		bool a201 = sampled(image, sampler, x,     y - 1, z + 1) > 0;\n" +
				"		bool a202 = sampled(image, sampler, x + 1, y - 1, z + 1) > 0;\n" +
				"		bool a210 = sampled(image, sampler, x - 1, y,     z + 1) > 0;\n" +
				"		bool a211 = sampled(image, sampler, x,     y,     z + 1) > 0;\n" +
				"		bool a212 = sampled(image, sampler, x + 1, y,     z + 1) > 0;\n" +
				"		bool a220 = sampled(image, sampler, x - 1, y + 1, z + 1) > 0;\n" +
				"		bool a221 = sampled(image, sampler, x,     y + 1, z + 1) > 0;\n" +
				"		bool a222 = sampled(image, sampler, x + 1, y + 1, z + 1) > 0;\n" +
				"\n" +
				"		bool b =   (a000 && a001 && a002 && a010 && a011 && a012 && a020 && a021 && a022 &&\n" +
				"					a100 && a101 && a102 && a110 &&         a112 && a120 && a121 && a122 &&\n" +
				"					a200 && a201 && a202 && a210 && a211 && a212 && a220 && a221 && a222);\n" +
				"		float v = 0;\n" +
				"		if(b)\n" +
				"			v = a111;\n" +
				"\n" +
				"		write_imagef(out, (int4)(x, y, z, 0), a111 - v);\n" +
				"	}\n" +
				"}\n" +
				"\n";
				/* ****************************************************************
				 * boxfilter()
				 * ****************************************************************/
				source = source +
				"kernel void\n" +
				"boxfilter(\n" +
				"			__read_only image3d_t image,\n" +
				"			sampler_t sampler,\n" +
				"			__write_only image3d_t out,\n" +
				"			int3 out_size)\n" +
				"{\n" +
				"	int x = get_global_id(0);\n" +
				"	int y = get_global_id(1);\n" +
				"	int z = get_global_id(2);\n" +
				"\n" +
				"	if(x < out_size.x && y < out_size.y && z < out_size.z) {\n" +
				"		float a000 = sampled(image, sampler, x - 1, y - 1, z - 1);\n" +
				"		float a001 = sampled(image, sampler, x,     y - 1, z - 1);\n" +
				"		float a002 = sampled(image, sampler, x + 1, y - 1, z - 1);\n" +
				"		float a010 = sampled(image, sampler, x - 1, y,     z - 1);\n" +
				"		float a011 = sampled(image, sampler, x,     y,     z - 1);\n" +
				"		float a012 = sampled(image, sampler, x + 1, y,     z - 1);\n" +
				"		float a020 = sampled(image, sampler, x - 1, y + 1, z - 1);\n" +
				"		float a021 = sampled(image, sampler, x,     y + 1, z - 1);\n" +
				"		float a022 = sampled(image, sampler, x + 1, y + 1, z - 1);\n" +
				"		float a100 = sampled(image, sampler, x - 1, y - 1, z);\n" +
				"		float a101 = sampled(image, sampler, x,     y - 1, z);\n" +
				"		float a102 = sampled(image, sampler, x + 1, y - 1, z);\n" +
				"		float a110 = sampled(image, sampler, x - 1, y,     z);\n" +
				"		float a111 = sampled(image, sampler, x,     y,     z);\n" +
				"		float a112 = sampled(image, sampler, x + 1, y,     z);\n" +
				"		float a120 = sampled(image, sampler, x - 1, y + 1, z);\n" +
				"		float a121 = sampled(image, sampler, x,     y + 1, z);\n" +
				"		float a122 = sampled(image, sampler, x + 1, y + 1, z);\n" +
				"		float a200 = sampled(image, sampler, x - 1, y - 1, z + 1);\n" +
				"		float a201 = sampled(image, sampler, x,     y - 1, z + 1);\n" +
				"		float a202 = sampled(image, sampler, x + 1, y - 1, z + 1);\n" +
				"		float a210 = sampled(image, sampler, x - 1, y,     z + 1);\n" +
				"		float a211 = sampled(image, sampler, x,     y,     z + 1);\n" +
				"		float a212 = sampled(image, sampler, x + 1, y,     z + 1);\n" +
				"		float a220 = sampled(image, sampler, x - 1, y + 1, z + 1);\n" +
				"		float a221 = sampled(image, sampler, x,     y + 1, z + 1);\n" +
				"		float a222 = sampled(image, sampler, x + 1, y + 1, z + 1);\n" +
				"\n" +
				"		float v = (a000 + a001 + a002 + a010 + a011 + a012 + a020 + a021 + a022 +\n" +
				"					a100 + a101 + a102 + a110 + a111 + a112 + a120 + a121 + a122 +\n" +
				"					a200 + a201 + a202 + a210 + a211 + a212 + a220 + a221 + a222) / 27.0f;\n" +
				"\n" +
				"		write_imagef(out, (int4)(x, y, z, 0), v);\n" +
				"	}\n" +
				"}\n" +
				"\n";
		        /* ****************************************************************
				 * calculateGradients()
				 * ****************************************************************/
				source = source +
				"kernel void\n" +
				"calculateGradients(\n" +
				"			__read_only image3d_t image,\n" +
				"			sampler_t sampler,\n" +
				"			__write_only image3d_t gradients,\n" +
				"			int3 grad_size,\n" +
				"			float dzByDx)\n" +
				"{\n" +
				"	int x = get_global_id(0);\n" +
				"	int y = get_global_id(1);\n" +
				"	int z = get_global_id(2);\n" +
				"\n" +
				"	if(x < grad_size.x && y < grad_size.y && z < grad_size.z) {\n" +
				"		float a000 = downsampled(image, sampler, x - 1, y - 1, z - 1);\n" +
				"		float a001 = downsampled(image, sampler, x,     y - 1, z - 1);\n" +
				"		float a002 = downsampled(image, sampler, x + 1, y - 1, z - 1);\n" +
				"		float a010 = downsampled(image, sampler, x - 1, y,     z - 1);\n" +
				"		float a011 = downsampled(image, sampler, x,     y,     z - 1);\n" +
				"		float a012 = downsampled(image, sampler, x + 1, y,     z - 1);\n" +
				"		float a020 = downsampled(image, sampler, x - 1, y + 1, z - 1);\n" +
				"		float a021 = downsampled(image, sampler, x,     y + 1, z - 1);\n" +
				"		float a022 = downsampled(image, sampler, x + 1, y + 1, z - 1);\n" +
				"		float a100 = downsampled(image, sampler, x - 1, y - 1, z);\n" +
				"		float a101 = downsampled(image, sampler, x,     y - 1, z);\n" +
				"		float a102 = downsampled(image, sampler, x + 1, y - 1, z);\n" +
				"		float a110 = downsampled(image, sampler, x - 1, y,     z);\n" +
				"		float a111 = downsampled(image, sampler, x,     y,     z);\n" +
				"		float a112 = downsampled(image, sampler, x + 1, y,     z);\n" +
				"		float a120 = downsampled(image, sampler, x - 1, y + 1, z);\n" +
				"		float a121 = downsampled(image, sampler, x,     y + 1, z);\n" +
				"		float a122 = downsampled(image, sampler, x + 1, y + 1, z);\n" +
				"		float a200 = downsampled(image, sampler, x - 1, y - 1, z + 1);\n" +
				"		float a201 = downsampled(image, sampler, x,     y - 1, z + 1);\n" +
				"		float a202 = downsampled(image, sampler, x + 1, y - 1, z + 1);\n" +
				"		float a210 = downsampled(image, sampler, x - 1, y,     z + 1);\n" +
				"		float a211 = downsampled(image, sampler, x,     y,     z + 1);\n" +
				"		float a212 = downsampled(image, sampler, x + 1, y,     z + 1);\n" +
				"		float a220 = downsampled(image, sampler, x - 1, y + 1, z + 1);\n" +
				"		float a221 = downsampled(image, sampler, x,     y + 1, z + 1);\n" +
				"		float a222 = downsampled(image, sampler, x + 1, y + 1, z + 1);\n" +
				"\n" +
				"		float dx = (a002 + a012 + a022 + a102 + a112 + a122 + a202 + a212 + a222) -\n" +
				"					(a000 + a010 + a020 + a100 + a110 + a120 + a200 + a210 + a220);\n" +
				"		float dy = (a020 + a021 + a022 + a120 + a121 + a122 + a220 + a221 + a222) -\n" +
				"					(a000 + a001 + a002 + a100 + a101 + a102 + a200 + a201 + a202);\n" +
				"		float dz = (a200 + a201 + a202 + a210 + a211 + a212 + a220 + a221 + a222) -\n" +
				"					(a000 + a001 + a002 + a010 + a011 + a012 + a020 + a021 + a022);\n" +
				"		int4 v = convert_int4(round(255 * normalize((float4)(dx, dy, dz * dzByDx, 0))));" +
				"		write_imagei(gradients, (int4)(x, y, z, 0), v);\n" +
				"	}\n" +
				"}\n" +
				"\n";
}
		return source;
	}

	@SuppressWarnings("unused")
	public static String makeSource(int channels, boolean backgroundTexture, boolean combinedAlpha, boolean mip, boolean[] useLights, boolean[] colorLUT) {
		boolean useLightsAtAll = false;
		for(int i = 0; i < useLights.length; i++) {
			if(useLights[i]) {
				useLightsAtAll = true;
				break;
			}
		}
		String source = makeCommonSource(channels) +
		    /* ****************************************************************
			 * sample()
			 * ****************************************************************/
			"inline float2\n" +
			"sample(float3 p0,\n" +
			"		__read_only image3d_t texture,\n" +
			"		sampler_t sampler,\n" +
			"		float maxv,\n" +
			"		float2 minAlphaColor,\n" +
			"		float2 dAlphaColor,\n" +
			"		float2 gammaAlphaColor,\n" +
			"		float alphacorr) {\n" +
	        "	float v0 = maxv * read_imagef(texture, sampler, (float4)(p0 + 0.5f, 0)).x;\n" +
	        "	float2 rAlphaColor = pow(\n" +
	        "	        clamp((v0 - minAlphaColor) / dAlphaColor, 0.0f, 1.0f),\n" +
	        "	        gammaAlphaColor);\n" +
	        "\n" +
	        "	// Opacity correction:\n" +
	        "	rAlphaColor.x = 1 - pow(clamp(1 - rAlphaColor.x, 0.0f, 1.0f), alphacorr);\n" +
	        "	return rAlphaColor;\n" +
	        "}\n" +
	        "\n";
			/* ****************************************************************
			 * sampleWithColorLUT()
			 * ****************************************************************/
			source = source +
			"inline float4\n" +
			"sampleWithColorLUT(float3 p0,\n" +
			"		__read_only image3d_t texture,\n" +
			"		sampler_t sampler,\n" +
			"		float maxv,\n" +
			"		image1d_buffer_t textureLUT,\n" +
			"		bool dbg) {\n" +
			"	float v0 = maxv * read_imagef(texture, sampler, (float4)(p0 + 0.5f, 0)).x;\n" +
			"	if(v0 == 0)\n" +
			"		return (float4)(0.0f);\n" +
			"	int idx = clamp((uint)round(v0), (uint)0, (uint)maxv);\n" +
			"	if(dbg)\n" +
			"		printf(\"v0 = %f, idx = %d\", v0, idx);\n" +
			"	float4 color = (float4)(1.0f, convert_float3(read_imageui(textureLUT, idx).xyz) / 255.0f);\n" +
			"	return color;\n" +
			"}\n" +
			"\n";
	        /* ****************************************************************
			 * grad()
			 * ****************************************************************/
if(GRADIENT_MODE == GRADIENT_MODE_ONTHEFLY) {
			source = source +
			"float4\n" +
	        "grad(float3 p0,\n" +
	        "        __read_only image3d_t texture,\n" +
	        "        sampler_t sampler,\n" +
	        "        float maxv,\n" +
	        "        float2 minAlphaColor,\n" +
	        "        float2 dAlphaColor,\n" +
	        "        float2 gammaAlphaColor,\n" +
	        "        float alphacorr) {\n" +
	        "	float3 p = p0 + 0.5f;\n" +
	        "	float dx = 255 * (read_imagef(texture, sampler, (float4)(p.x + 1, p.y, p.z, 0)).x -\n" +
	        "					read_imagef(texture, sampler, (float4)(p.x - 1, p.y, p.z, 0)).x);\n" +
	        "	float dy = 255 * (read_imagef(texture, sampler, (float4)(p.x, p.y + 1, p.z, 0)).x -\n" +
	        "					read_imagef(texture, sampler, (float4)(p.x, p.y - 1, p.z, 0)).x);\n" +
	        "	float dz = 255 * (read_imagef(texture, sampler, (float4)(p.x, p.y, p.z + 1, 0)).x -\n" +
	        "					read_imagef(texture, sampler, (float4)(p.x, p.y, p.z - 1, 0)).x);\n" +
			"	float4 grad = (float4)(dx, dy, dz, 0);\n" +
	        "	return normalize(grad);\n" +
			"}\n" +
	        "\n";
} else if(GRADIENT_MODE == GRADIENT_MODE_DOWNSAMPLED_TEXTURE) {
			source = source +
			"float4\n" +
			"grad(float3 p0,\n" +
	        "        __read_only image3d_t grad,\n" +
	        "        sampler_t sampler) {\n" +
	        "	float3 p = p0 / 2.0f + 0.5f;\n" +
	        "	float4 gradient = convert_float4(read_imagei(grad, sampler, (float4)(p.x, p.y, p.z, 0)));\n" +
	        "	return normalize(gradient);\n" +
			"}\n" +
	        "\n";
} else if(GRADIENT_MODE == GRADIENT_MODE_TEXTURE) {
			source = source +
			"float4\n" +
			"grad(float3 p0,\n" +
		    "        __read_only image3d_t grad,\n" +
		    "        sampler_t sampler) {\n" +
		    "	float3 p = p0 + 0.5f;\n" +
		    "	float4 gradient = convert_float4(read_imagei(grad, sampler, (float4)(p.x, p.y, p.z, 0)));\n" +
		    "	return normalize(gradient);\n" +
			"}\n" +
		    "\n";
}

	        /* ****************************************************************
			 * raycastKernel()
			 * ****************************************************************/
			source = source +
			"kernel void\n" +
			"raycastKernel(\n";
		for(int c = 0; c < channels; c++) {
			source = source +
			"		__read_only image3d_t texture" + c + ",";
			if(GRADIENT_MODE != GRADIENT_MODE_ONTHEFLY && useLights[c])
				source = source + " __read_only image3d_t gradient" + c + ",";
			source = source + " int3 rgb" + c + ",";
			if(colorLUT[c]) {
				source = source + "__read_only image1d_buffer_t textureLUT" + c + ",";
			}
			source = source + "\n";
		}
		source = source +
			"		sampler_t lisampler,\n" +
			"		sampler_t nnsampler,\n" +
			"		__global unsigned int *d_result,\n" +
			"		int2 target_size,\n" +
			"		const float16 inverseTransform,\n";
		for(int c = 0; c < channels; c++) {
			source = source +
			"		float alphamin" + c + ", float alphamax" + c + ", float alphagamma" + c + ",\n" +
			"		float colormin" + c + ", float colormax" + c + ", float colorgamma" + c + ",\n" +
			"		float weight" + c + ",\n" +
			"		int3 bb0" + c + ",\n" +
			"		int3 bb1" + c + ",\n" +
			"		float zstart" + c + ", float zend" + c + ",\n" +
			"		float4 light" + c + ",\n";
		}
		source = source +
			"		float alphacorr,\n" +
			"		float3 inc,\n";
		if(backgroundTexture)
			source = source +
			"		__read_only image2d_t bgtexture, sampler_t bgsampler,\n";
		else
			source = source +
			"		int3 background,\n";
		source = source +
			"		int bitsPerSample)\n" +
			"{\n" +
			"	int x = get_global_id(0);\n" +
			"	int y = get_global_id(1);\n" +
			"\n" +
			"	if(x < target_size.x && y < target_size.y) {\n" +
//			"		bool dbg = (x == 128 && y == 128);\n" +
			"		unsigned int maxv = (1 << bitsPerSample) - 1;\n" +
			"\n" +
			"		float3 r0 = multiplyMatrixVector(inverseTransform, (float4)(x, y, 0, 1));\n" +
			"		int idx_out = y * target_size.x + x;\n" +
			"\n";
		if(backgroundTexture) {
			source = source +
			"\n" +
			"		float2 p = (float2)((float)x / target_size.x, (float)y / target_size.y);\n" +
			"		uint4 background = read_imageui(bgtexture, bgsampler, p);\n" +
			"\n";
		}
		source = source +
			"		bool hits   = false;\n" +
			"		float inear = 1e5;\n" +
			"		float ifar  = -1e5;\n" +
			"\n";
		for(int c = 0; c < channels; c++) {
			source = source +
			"		float inear" + c + " = 0;\n" +
			"		float ifar" + c + "  = 0;\n" +
			"		hits = intersects(convert_float3(bb0" + c + "), convert_float3(bb1" + c + "), r0, inc, &inear" + c + ", &ifar" + c + ") || hits;\n" +
			"		inear" + c + " = fmax(inear" + c + ", zstart" + c + ");\n" +
			"		ifar" + c + "  = fmin(ifar" + c + ", zend" + c + ");\n" +
			"		inear  = fmin(inear, inear" + c + ");\n" +
			"		ifar   = fmax(ifar, ifar" + c + ");\n" +
//			"		if(dbg) {\n" +
//			"			printf(\"zstart" + c + " = %f\\n\", zstart" + c + ");\n" +
//			"			printf(\"zend" + c + " = %f\\n\", zend" + c + ");\n" +
//			"			printf(\"bb0" + c + " = %d, %d, %d\\n\", bb0" + c + ".x, bb0" + c + ".y, bb0" + c + ".z);\n" +
//			"			printf(\"bb1" + c + " = %d, %d, %d\\n\", bb1" + c + ".x, bb1" + c + ".y, bb1" + c + ".z);\n" +
//			"			printf(\"inear" + c + " = %f\\n\", inear" + c + ");\n" +
//			"			printf(\"ifar" + c + " = %f\\n\", ifar" + c + ");\n" +
//			"			printf(\"inc = (%f, %f, %f)\\n\", inc.x, inc.y, inc.z);\n" +
//			"		}\n" +
			"\n";
		}
		source = source +
			"		if(!hits) {\n" +
			"			d_result[idx_out] = (unsigned int)((background.x << 16) | (background.y << 8) | background.z); \n" +
			"			return;\n" +
			"		}\n" +
			"\n" +
			"		float3 p0 = r0 + inear * inc;\n" +
			"\n";
		final float[] light = new float[] {1, 1, 1};
		float tmp = (float)Math.sqrt(light[0] * light[0] + light[1] * light[1] + light[2] * light[2]);
		light[0] /= tmp;
		light[1] /= tmp;
		light[2] /= tmp;

		// halfway vector H = (L + V) / abs(L + V), L = light, V = view = (0, 0, 1)
		final float[] ha = new float[] {
				light[0],
				light[1],
				light[2] + 1};
		tmp = (float)Math.sqrt(ha[0] * ha[0] + ha[1] * ha[1] + ha[2] * ha[2]);
		ha[0] /= tmp;
		ha[1] /= tmp;
		ha[2] /= tmp;
		for(int c = 0; c < channels; c++) {
			if(!colorLUT[c]) {
			source = source +
			"		float color" + c + " = 0;\n";
			} else {
			source = source +
			"		float3 color" + c + " = (float3)(0);\n";
			}
			source = source +
			"		float alpha" + c + " = 0;\n" +
			"		float2 minAlphaColor" + c + "   = (float2)(alphamin" + c + ", colormin" + c + ");\n" +
			"		float2 maxAlphaColor" + c + "   = (float2)(alphamax" + c + ", colormax" + c + ");\n" +
			"		float2 gammaAlphaColor" + c + " = (float2)(alphagamma" + c + ", colorgamma" + c + ");\n" +
			"		float2 dAlphaColor" + c + "     = maxAlphaColor" + c + " - minAlphaColor" + c + ";\n" +
			"		float4 no" + c + " = (float4)(0, 0, 0, 0);" +
			"\n\n";
		}
		boolean useAnyLight = false;
		for(int c = 0; c < channels; c++) {
			if(useLights[c]) {
				useAnyLight = true;
				break;
			}
		}

		source = source +
			"		bool dbg = x == 80 && y == 170;\n";


		source = source +
			"		for(int step = floor(inear); step < ifar; step++) {\n" +
			"\n" +
			"			// mix(x, y, a) = x + (y-x)a = (1-a)x + ay for 0 <= a <= 1\n" +
			"			// mad(a, b, c) = a * b + c;\n" +
			"\n" +
			"			// color = color + (1 - alpha) * alphar * colorr;\n" +
			"			// alpha = alpha + (1 - alpha) * alphar;\n" +
			"\n";
		for(int c = 0; c < channels; c++) {
			source = source +
			"			bool ch" + c + " = all(p0 >= convert_float3(bb0" + c + ") && p0 < convert_float3(bb1" + c + ")) &&\n" +
			"					step >= inear" + c + " &&\n" +
			"					step <= ifar" + c + ";\n" +
//			"			if(dbg && step == 20) {\n" +
//			"				printf(\"ch" + c + " = %d\\n\", ch" + c + ");\n" +
//			"			}\n" +
			"			if(ch" + c + " && alpha" + c + " < 0.99f) {\n";
//			"			if(ch" + c + ") {\n";
			if(colorLUT[c]) {
				source = source +
			"				float4 rAlphaColor" + c + " = sampleWithColorLUT(p0,\n" +
			"						texture" + c + ", nnsampler, maxv, textureLUT" + c + ", dbg);\n" +
			"				if(dbg) {\n" +
			"					float3 col = (float3)(rAlphaColor" + c + ".yzw);\n" +
			"					printf(\"color = %f, %f, %f\\n\", col.x, col.y, col.z);\n" +
//			"					for(int i = 0; i < 10; i++) {\n" +
//			"						uint4 col = read_imageui(textureLUT" + c + ", i);\n" +
//			"						printf(\"color[i] = %d, %d, %d, %d\\n\", col.s0, col.s1, col.s2, col.s3);\n" +
//			"					}\n" +
			"					\n" +
			"				}\n";
			}
			else {
				source = source +
			"				float2 rAlphaColor" + c + " = sample(p0,\n" +
			"						texture" + c + ", lisampler, maxv,\n" +
			"						minAlphaColor" + c + ", dAlphaColor" + c + ", gammaAlphaColor" + c + ", alphacorr);\n";
			}
			source = source + "\n";

			if(mip) {
			if(colorLUT[c]) {
				source = source +
			"				color" + c + " = max(color" + c + ", rAlphaColor" + c + ".yzw);\n" +
			"				alpha" + c + " = max(alpha" + c + ", rAlphaColor" + c + ".x);\n" +
			"			}\n" +
			"\n";
			}
			else {
				source = source +
			"				if(rAlphaColor" + c + ".y > color" + c + ") {\n" +
			"					color" + c + " = rAlphaColor" + c + ".y;\n" +
			"					alpha" + c + " = rAlphaColor" + c + ".x;\n" +
			"				}\n" +
			"			}\n" +
			"\n";
			}
				continue;
			}

			source = source + "\n";
			if(combinedAlpha) {
				source = source +
			"				float a" + c + " = (" + sum("alpha", channels) + ") / " + 1 + ";\n";
			} else {
				source = source +
			"				float a" + c + " = alpha" + c + ";\n";
			}
			source = source +
			"				float tmp" + c + " = weight" + c + " * (1 - a" + c + ") * rAlphaColor" + c + ".x;\n";
			if(useLights[c]) {
				if(GRADIENT_MODE == GRADIENT_MODE_ONTHEFLY) {
					source = source +
			"				float4 grad" + c + " = grad(p0,\n" +
			"						texture" + c + ", lisampler, maxv,\n" +
			"						minAlphaColor" + c + ", dAlphaColor" + c + ", gammaAlphaColor" + c + ", alphacorr);\n";
				}
				else {
					source = source +
			"				float4 grad" + c + " = grad(p0, gradient" + c + ", lisampler);\n";
				}
					source = source +
			"				no" + c + " = mad(tmp" + c + ", grad" + c + ", no" + c + ");\n";
			}
			if(colorLUT[c]) {
			source = source +
			"				color" + c + " = mad(rAlphaColor" + c + ".yzw, tmp" + c + ", color" + c + ");\n";
			}
			else {
			source = source +
			"				color" + c + " = mad(rAlphaColor" + c + ".y, tmp" + c + ", color" + c + ");\n";
			}
			source = source +
			"				alpha" + c + " = alpha" + c + " + tmp" + c + ";\n" +
			"			}\n" +
			"\n";
		}
		source = source +
			"			p0 = p0 + inc;\n" +
			"		}\n";
		if(useLightsAtAll) {
		source = source +
			"		float4 li = (float4)(" + light[0] + ", " + light[1] + ", " + light[2] + ", 0);\n" +
			"		li = normalize((float4)(multiplyMatrixVector(inverseTransform, li), 0));\n" +
			"		float4 ha = (float4)(" + ha[0]    + ", " + ha[1]    + ", " + ha[2]    + ", 0);\n" +
			"		ha = normalize((float4)(multiplyMatrixVector(inverseTransform, ha), 0));\n" +
			"		float ko, kd, ks, shininess;\n" +
			"\n";
		}
		source = source +
			"		unsigned int out_r = 0;\n" +
			"		unsigned int out_g = 0;\n" +
			"		unsigned int out_b = 0;\n" +
			"\n";
		for(int c = 0; c < channels; c++) {
			if(useLights[c]) {
			source = source +
			"		if(dbg) {\n" +
			"			float4 notmp = no" + c + ";\n"+
			"			printf(\"no = %f, %f, %f\\n\", notmp.x, notmp.y, notmp.z);\n" +
			"		}\n" +
			"		ko = light" + c + ".x;\n" +
			"		kd = light" + c + ".y;\n" +
			"		ks = light" + c + ".z;\n" +
			"		shininess = light" + c + ".w;\n" +
			"\n";
			source = source +
			"\n" +
//				"				if(dbg) {\n" +
//				"					printf(\"grad = %f, %f, %f\\n\", grad" + c + ".x, grad" + c + ".y, grad" + c + ".z);\n" +
//				"					printf(\"li = %f, %f, %f\\n\", li.x, li.y, li.z);\n" +
//				"					printf(\"ha = %f, %f, %f\\n\", ha.x, ha.y, ha.z);\n" +
//				"					printf(\"kd = %f\\n\", kd);\n" +
//				"					printf(\"ks = %f\\n\", ks);\n" +
//				"					printf(\"tmpd = %f\\n\", tmpd);\n" +
//				"					printf(\"tmph = %f\\n\", tmph);\n" +
//				"				}\n" +
			"		if(dbg) {\n" +
			"			float3 ctmp = color" + c + ";\n"+
			"			printf(\"colorbefore = %f, %f, %f\\n\", ctmp.x, ctmp.y, ctmp.z);\n" +
			"		}\n" +
			"		color" + c + " = \n" +
			"			ko * color" + c + " +\n" +
			"			kd * fmax((float)0, dot(li, no" + c + ")) +\n" +
			"			ks * fmax((float)0, pow(dot(ha, no" + c + "), shininess));\n" +
			"		if(dbg) {\n" +
			"			float3 ctmp = color" + c + ";\n"+
			"			printf(\"colorafter = %f, %f, %f\\n\", ctmp.x, ctmp.y, ctmp.z);\n" +
			"		}\n" +			"\n";
				}
			source = source +
			"		color" + c + " = color" + c + " * weight" + c + ";\n" +
			"		alpha" + c + " = alpha" + c + " * weight" + c + ";\n" +
			"\n";
			if(colorLUT[c]) {
			source = source +
			"		out_r = out_r + 255 * color" + c + ".x;\n" +
			"		out_g = out_g + 255 * color" + c + ".y;\n" +
			"		out_b = out_b + 255 * color" + c + ".z;\n" +
			"\n";
			}
			else {
			source = source +
			"		out_r = out_r + color" + c + " * rgb" + c + ".x;\n" +
			"		out_g = out_g + color" + c + " * rgb" + c + ".y;\n" +
			"		out_b = out_b + color" + c + " * rgb" + c + ".z;\n" +
			"\n";
			}
		}
		source = source +
			"		float alpha = clamp(" + sum("alpha", channels) + ", 0.0f, 1.0f);\n" +
			"		out_r = (unsigned int)(clamp(alpha * out_r + (1 - alpha) * background.x, 0.0f, 255.0f));\n" +
			"		out_g = (unsigned int)(clamp(alpha * out_g + (1 - alpha) * background.y, 0.0f, 255.0f));\n" +
			"		out_b = (unsigned int)(clamp(alpha * out_b + (1 - alpha) * background.z, 0.0f, 255.0f));\n" +
			"		d_result[idx_out] = (unsigned int)((out_r << 16) | (out_g << 8) | out_b);\n" +
			"	}\n" +
			"}";
		return source;
	}

	// returns "a0 + a1 + a2"
	private static String sum(String prefix, int nChannels) {
		String ret = new String();
		for(int c = 0; c < nChannels - 1; c++)
			ret += prefix + c + " + ";
		ret += prefix + (nChannels - 1);
		return ret;
	}

	// returns "a0 * b0.x + a1 * b1.x + a2 * b2.x"
	private static String sumOfProducts(String prefix1, String prefix2, String postfix2, int nChannels) {
		String ret = new String();
		for(int c = 0; c < nChannels - 1; c++) {
			ret += prefix1 + c + " * " + prefix2 + c + postfix2 + " + ";
		}
		ret += prefix1 + (nChannels - 1) + " * " + prefix2 + (nChannels - 1) + postfix2;
		return ret;
	}
}
