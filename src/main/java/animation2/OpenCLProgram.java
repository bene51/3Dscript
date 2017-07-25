package animation2;

public class OpenCLProgram {

	public static String makeSource(int channels) {
		String source =
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
			"\n" +
			"kernel void\n" +
			"raycastKernel(\n";
		for(int c = 0; c < channels; c++) {
			source = source +
			"		__read_only image3d_t texture" + c + ", int3 rgb" + c + ",\n";
		}
		source = source +
			"		sampler_t sampler,\n" +
			"		int3 data_origin,\n" +
			"		int3 data_size,\n" +
			"		__global unsigned int *d_result,\n" +
			"		int2 target_size,\n" +
			"		const float16 inverseTransform,\n" +
			"		float zstart, float zend, float zStep,\n";
		for(int c = 0; c < channels; c++) {
			source = source +
			"		float alphamin" + c + ", float alphamax" + c + ", float alphagamma" + c + ",\n" +
			"		float colormin" + c + ", float colormax" + c + ", float colorgamma" + c + ",\n";
		}
		source = source +
			"		float alphastop,\n" +
			"		float3 dir,\n" +
			"		float3 inc,\n" +
			"		int3 background,\n" +
			"		int bitsPerSample)\n" +
			"{\n" +
			"	int x = get_global_id(0);\n" +
			"	int y = get_global_id(1);\n" +
			"\n" +
			"	if(x < target_size.x && y < target_size.y) {\n" +
			"		float3 r0 = multiplyMatrixVector(inverseTransform, (float4)(x, y, 0, 1));\n" +
			"		float inear = 0;\n" +
			"		float ifar = 0;\n" +
			"\n" +
			"		int3 bb0 = data_origin;\n" +
			"		int3 bb1 = data_origin + data_size;\n" +
			"		bool hits = intersects(convert_float3(bb0), convert_float3(bb1), r0, dir, &inear, &ifar);\n" +
			"		int idx_out = y * target_size.x + x;\n" +
			"		if(!hits) {\n" +
			"			d_result[idx_out] = (unsigned int)((background.x << 16) | (background.y << 8) | background.z); \n" +
			"			return;\n" +
			"		}\n" +
			"		inear = fmax(inear, zstart);\n" +
			"		ifar  = fmin(ifar, zend);\n" +
			"\n" +
			"\n" +
			"		float3 p0 = r0 + inear * dir;\n" +
			"\n" +
			"		int n = (int)floor(fdim(ifar, inear) / zStep);\n" +
			"		unsigned int maxv = (1 << bitsPerSample);\n" +
			"";
		for(int c = 0; c < channels; c++) {
			source = source +
			"		float color" + c + " = 0;\n" +
			"		float alpha" + c + " = 0;\n" +
			"		float2 minAlphaColor" + c + "   = (float2)(alphamin" + c + ", colormin" + c + ");\n" +
			"		float2 maxAlphaColor" + c + "   = (float2)(alphamax" + c + ", colormax" + c + ");\n" +
			"		float2 gammaAlphaColor" + c + " = (float2)(alphagamma" + c + ", colorgamma" + c + ");\n" +
			"		float2 dAlphaColor" + c + "     = maxAlphaColor" + c + " - minAlphaColor" + c + ";\n";
		}
		source = source + "\n" +
			"		for(int step = 0; step < n; step++) {\n" +
			"			if(all(p0 >= convert_float3(bb0) && p0 < convert_float3(bb1))) {\n" +
			"\n";
		for(int c = 0; c < channels; c++) {
			source = source +
			"				float v" + c + " = maxv * read_imagef(texture" + c + ", sampler, (float4)(p0 + 0.5f, 0)).x + 0.5;\n" +
			"				float2 rAlphaColor" + c + " = pow(\n" +
			"					clamp((v" + c + " - minAlphaColor" + c + ") / dAlphaColor" + c + ", 0.0f, 1.0f),\n" +
			"					gammaAlphaColor" + c + ");\n\n";
		}
		source = source +
			"\n" +
			"				// mix(x, y, a) = x + (y-x)a = (1-a)x + ay for 0 <= a <= 1\n" +
			"				// mad(a, b, c) = a * b + c;\n" +
			"\n" +
			"				// color = color + (1 - alpha) * alphar * colorr;\n" +
			"				// alpha = alpha + (1 - alpha) * alphar;\n";
		for(int c = 0; c < channels; c++) {
			source = source +
			"				float a" + c + " = (" + sum("alpha", channels) + ");\n" +
			"\n" +
			"				float tmp" + c + " = (1 - a" + c + ") * rAlphaColor" + c + ".x;\n" +
			"				color" + c + " = mad(rAlphaColor" + c + ".y, tmp" + c + ", color" + c + ");\n" +
			"				alpha" + c + " = alpha" + c + " + tmp" + c + ";\n";
		}
		source = source +
			"			}\n" +
			"			p0 = p0 + inc;\n" +
			"		}\n" +
			"		unsigned int out_r = " + sumOfProducts("color", "rgb", ".x", channels) + ";\n" +
			"		unsigned int out_g = " + sumOfProducts("color", "rgb", ".y", channels) + ";\n" +
			"		unsigned int out_b = " + sumOfProducts("color", "rgb", ".z", channels) + ";\n" +
			"		float alpha = " + sum("alpha", channels) + ";\n" +
			"		out_r = (unsigned int)(clamp(alpha * out_r + (1 - alpha) * background.x, 0.0f, 255.0f));\n" +
			"		out_g = (unsigned int)(clamp(alpha * out_g + (1 - alpha) * background.y, 0.0f, 255.0f));\n" +
			"		out_b = (unsigned int)(clamp(alpha * out_b + (1 - alpha) * background.z, 0.0f, 255.0f));\n" +
			"		d_result[idx_out] = (unsigned int)((out_r << 16) | (out_g << 8) | out_b);\n" +
			"	}\n" +
			"}";
		System.out.println(source);
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
		for(int c = 0; c < nChannels - 1; c++)
			ret += prefix1 + c + " * " + prefix2 + c + postfix2 + " + ";
		ret += prefix1 + (nChannels - 1) + " * " + prefix2 + (nChannels - 1) + postfix2;
		return ret;
	}
}
