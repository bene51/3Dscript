package renderer3d;

public class OpenCLProgram {

	public static final boolean useLights = true;

	public static void main(String[] args) {
		System.out.println(makeSource(2, false, true));
	}

	private static String makeCommonSource(int channels) {
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
				"\n";
		return source;
	}

	public static String makeSourceForMIP(int channels, boolean backgroundTexture) {
		String source = makeCommonSource(channels) +
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
				"		float zstart, float zend,\n";
			for(int c = 0; c < channels; c++) {
				source = source +
				"		float alphamin" + c + ", float alphamax" + c + ", float alphagamma" + c + ",\n" +
				"		float colormin" + c + ", float colormax" + c + ", float colorgamma" + c + ",\n" +
				"		float weight" + c + ",\n";
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
				"		float3 r0 = multiplyMatrixVector(inverseTransform, (float4)(x, y, 0, 1));\n" +
				"		float inear = 0;\n" +
				"		float ifar = 0;\n" +
				"\n";
			if(backgroundTexture) {
				source = source +
				"\n" +
				"		float2 p = (float2)((float)x / target_size.x, (float)y / target_size.y);\n" +
				"		uint4 background = read_imageui(bgtexture, bgsampler, p);\n" +
				"\n";
			}
				source = source +
				"		int3 bb0 = data_origin;\n" +
				"		int3 bb1 = data_origin + data_size;\n" +
				"		bool hits = intersects(convert_float3(bb0), convert_float3(bb1), r0, inc, &inear, &ifar);\n" +
				"		int idx_out = y * target_size.x + x;\n" +
				"		if(!hits) {\n" +
				"			d_result[idx_out] = (unsigned int)((background.x << 16) | (background.y << 8) | background.z); \n" +
				"			return;\n" +
				"		}\n" +
				"		inear = fmax(inear, zstart);\n" +
				"		ifar  = fmin(ifar, zend);\n" +
				"\n" +
				"\n" +
				"		float3 p0 = r0 + inear * inc;\n" +
				"\n" +
				"		int n = (int)floor(fdim(ifar, inear));\n" +
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
						"\n" +
				"				if(rAlphaColor" + c + ".y > color" + c + ") {\n" +
				"					color" + c + " = rAlphaColor" + c + ".y;\n" +
				"					alpha" + c + " = rAlphaColor" + c + ".x;\n" +
				"				}\n";
			}
			source = source +
				"			}\n" +
				"			p0 = p0 + inc;\n" +
				"		}\n";
			for(int c = 0; c < channels; c++) {
				source = source +
				"		color" + c + " = color" + c + " * weight" + c + ";\n";
			}
			source = source +
				"		unsigned int out_r = " + sumOfProducts("color", "rgb", ".x", channels) + ";\n" +
				"		unsigned int out_g = " + sumOfProducts("color", "rgb", ".y", channels) + ";\n" +
				"		unsigned int out_b = " + sumOfProducts("color", "rgb", ".z", channels) + ";\n" +
				"		float alpha = clamp(" + sum("alpha", channels) + ", 0.0f, 1.0f);\n" +
				"		out_r = (unsigned int)(clamp(alpha * out_r + (1 - alpha) * background.x, 0.0f, 255.0f));\n" +
				"		out_g = (unsigned int)(clamp(alpha * out_g + (1 - alpha) * background.y, 0.0f, 255.0f));\n" +
				"		out_b = (unsigned int)(clamp(alpha * out_b + (1 - alpha) * background.z, 0.0f, 255.0f));\n" +
				"		d_result[idx_out] = (unsigned int)((out_r << 16) | (out_g << 8) | out_b);\n" +
				"	}\n" +
				"}";
			System.out.println(source);
			return source;
	}

	public static String makeSource(int channels, boolean backgroundTexture, boolean combinedAlpha) {
		String source = makeCommonSource(channels) +
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
	        "	rAlphaColor.x = 1 - pow(1 - rAlphaColor.x, alphacorr);\n" +
	        "	return rAlphaColor;\n" +
	        "}\n" +
	        "\n" +
	        "float4\n" +
	        "grad(float3 p0,\n" +
	        "        __read_only image3d_t texture,\n" +
	        "        sampler_t sampler,\n" +
	        "        float maxv,\n" +
	        "        float2 minAlphaColor,\n" +
	        "        float2 dAlphaColor,\n" +
	        "        float2 gammaAlphaColor,\n" +
	        "        float alphacorr) {\n" +
//	        "	float2 dx = sample((float3)(p0.x + 2, p0.y, p0.z), texture, sampler, maxv, minAlphaColor, dAlphaColor, gammaAlphaColor, alphacorr) -\n" +
//	        "                alphaColor0;\n" +
//	        "	float2 dy = sample((float3)(p0.x, p0.y + 2, p0.z), texture, sampler, maxv, minAlphaColor, dAlphaColor, gammaAlphaColor, alphacorr) -\n" +
//	        "                alphaColor0;\n" +
//	        "	float2 dz = sample((float3)(p0.x, p0.y, p0.z + 2), texture, sampler, maxv, minAlphaColor, dAlphaColor, gammaAlphaColor, alphacorr) -\n" +
//	        "                alphaColor0;\n" +
	        "	float2 dx = sample((float3)(p0.x + 1, p0.y, p0.z), texture, sampler, maxv, minAlphaColor, dAlphaColor, gammaAlphaColor, alphacorr) -\n" +
	        "                sample((float3)(p0.x - 1, p0.y, p0.z), texture, sampler, maxv, minAlphaColor, dAlphaColor, gammaAlphaColor, alphacorr);\n" +
	        "	float2 dy = sample((float3)(p0.x, p0.y + 1, p0.z), texture, sampler, maxv, minAlphaColor, dAlphaColor, gammaAlphaColor, alphacorr) -\n" +
	        "                sample((float3)(p0.x, p0.y - 1, p0.z), texture, sampler, maxv, minAlphaColor, dAlphaColor, gammaAlphaColor, alphacorr);\n" +
	        "	float2 dz = sample((float3)(p0.x, p0.y, p0.z + 1), texture, sampler, maxv, minAlphaColor, dAlphaColor, gammaAlphaColor, alphacorr) -\n" +
	        "                sample((float3)(p0.x, p0.y, p0.z - 1), texture, sampler, maxv, minAlphaColor, dAlphaColor, gammaAlphaColor, alphacorr);\n" +
//	        "	float4 grad = (float4)(dx.x * dx.y, dy.x * dy.y, dz.x * dz.y, 0);\n" +
	        "	float4 grad = (float4)(dx.y, dy.y, dz.y, 0);\n" +
	        "	return normalize(grad);\n" +
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
			"		float zstart, float zend,\n";
		for(int c = 0; c < channels; c++) {
			source = source +
			"		float alphamin" + c + ", float alphamax" + c + ", float alphagamma" + c + ",\n" +
			"		float colormin" + c + ", float colormax" + c + ", float colorgamma" + c + ",\n" +
			"		float weight" + c + ",\n" +
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
			"		float3 r0 = multiplyMatrixVector(inverseTransform, (float4)(x, y, 0, 1));\n" +
			"		float inear = 0;\n" +
			"		float ifar = 0;\n" +
			"\n";
		if(backgroundTexture) {
			source = source +
			"\n" +
			"		float2 p = (float2)((float)x / target_size.x, (float)y / target_size.y);\n" +
			"		uint4 background = read_imageui(bgtexture, bgsampler, p);\n" +
			"\n";
		}
		source = source +
			"		int3 bb0 = data_origin;\n" +
			"		int3 bb1 = data_origin + data_size;\n" +
			"		bool hits = intersects(convert_float3(bb0), convert_float3(bb1), r0, inc, &inear, &ifar);\n" +
			"		int idx_out = y * target_size.x + x;\n" +
			"		if(!hits) {\n" +
			"			d_result[idx_out] = (unsigned int)((background.x << 16) | (background.y << 8) | background.z); \n" +
			"			return;\n" +
			"		}\n" +
			"		inear = fmax(inear, zstart);\n" +
			"		ifar  = fmin(ifar, zend);\n" +
			"\n" +
			"\n" +
			"		float3 p0 = r0 + inear * inc;\n" +
			"\n" +
			"		int n = (int)floor(fdim(ifar, inear));\n" +
//			"		if(x == 0 && y == 0) printf(\"n = %d\\n\", n);\n" +
			"		unsigned int maxv = (1 << bitsPerSample);\n" +
			"";
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
			source = source +
			"		float color" + c + " = 0;\n" +
			"		float alpha" + c + " = 0;\n" +
			"		float2 minAlphaColor" + c + "   = (float2)(alphamin" + c + ", colormin" + c + ");\n" +
			"		float2 maxAlphaColor" + c + "   = (float2)(alphamax" + c + ", colormax" + c + ");\n" +
			"		float2 gammaAlphaColor" + c + " = (float2)(alphagamma" + c + ", colorgamma" + c + ");\n" +
			"		float2 dAlphaColor" + c + "     = maxAlphaColor" + c + " - minAlphaColor" + c + ";\n";
		}
		source = source + "\n";
		if(useLights) {
			source = source +
			"		float4 li = (float4)(" + light[0] + ", " + light[1] + ", " + light[2] + ", 0);\n" +
			"		li = normalize((float4)(multiplyMatrixVector(inverseTransform, li), 0));\n" +
			"		float4 ha = (float4)(" + ha[0]    + ", " + ha[1]    + ", " + ha[2]    + ", 0);\n" +
			"		ha = normalize((float4)(multiplyMatrixVector(inverseTransform, ha), 0));\n" +
			"		float ko, kd, ks, shininess;\n";
		}
		source = source +
			"		for(int step = 0; step < n; step++) {\n" +
			"			if(all(p0 >= convert_float3(bb0) && p0 < convert_float3(bb1))) {\n" +
			"\n";
		for(int c = 0; c < channels; c++) {
			source = source +
//			"				bool dbg = (x == 128 && y == 128 && p0.z == 30);\n" +
			"				ko = light" + c + ".x;\n" +
			"				kd = light" + c + ".y;\n" +
			"				ks = light" + c + ".z;\n" +
			"				shininess = light" + c + ".w;\n" +
			"				float2 rAlphaColor" + c + " = sample(p0, texture" + c + ", sampler, maxv, minAlphaColor" + c + ", dAlphaColor" + c + ", gammaAlphaColor" + c + ", alphacorr);\n";
			if(!useLights)
				continue;
			source = source +
			"				float4 grad" + c + " = grad(p0, texture" + c + ", sampler, maxv, minAlphaColor" + c + ", dAlphaColor" + c + ", gammaAlphaColor" + c + ", alphacorr);\n" +
//			"				grad" + c + " = (float4)(multiplyMatrixVector(inverseTransform, grad" + c + "), 0);\n" +
//			"				if(dbg) {\n" +
//			"					printf(\"grad = %f, %f, %f\\n\", grad" + c + ".x, grad" + c + ".y, grad" + c + ".z);\n" +
//			"					printf(\"li = %f, %f, %f\\n\", li.x, li.y, li.z);\n" +
//			"					printf(\"ha = %f, %f, %f\\n\", ha.x, ha.y, ha.z);\n" +
//			"					printf(\"kd = %f\\n\", kd);\n" +
//			"					printf(\"ks = %f\\n\", ks);\n" +
//			"					printf(\"tmpd = %f\\n\", tmpd);\n" +
//			"					printf(\"tmph = %f\\n\", tmph);\n" +
//			"				}\n" +
			"				rAlphaColor" + c + ".y = \n" +
			"										ko * rAlphaColor" + c + ".y +\n" +
			"										kd * fmax((float)0, dot(li, grad" + c + ")) +\n" +
			"										ks * fmax((float)0, pow(dot(ha, grad" + c + "), shininess));\n";
		}
		source = source +
			"\n" +
			"				// mix(x, y, a) = x + (y-x)a = (1-a)x + ay for 0 <= a <= 1\n" +
			"				// mad(a, b, c) = a * b + c;\n" +
			"\n" +
			"				// color = color + (1 - alpha) * alphar * colorr;\n" +
			"				// alpha = alpha + (1 - alpha) * alphar;\n";
		for(int c = 0; c < channels; c++) {
			source = source + "\n";
			if(combinedAlpha) {
				source = source +
			"				float a" + c + " = (" + sum("alpha", channels) + ");\n";
			} else {
				source = source +
			"				float a" + c + " = alpha" + c + ";\n";
			}
			source = source +
			"				float tmp" + c + " = weight" + c + " * (1 - a" + c + ") * rAlphaColor" + c + ".x;\n" +
			"				color" + c + " = mad(rAlphaColor" + c + ".y, tmp" + c + ", color" + c + ");\n" +
			"				alpha" + c + " = alpha" + c + " + tmp" + c + ";\n";
		}
		source = source +
			"			}\n" +
			"			p0 = p0 + inc;\n" +
			"		}\n";
		for(int c = 0; c < channels; c++) {
			source = source +
			"		color" + c + " = color" + c + " * weight" + c + ";\n" +
			"		alpha" + c + " = alpha" + c + " * weight" + c + ";\n";
		}
		source = source +
					// color0 + rgb0.x + color1 * rgb1.x + ...
			"		unsigned int out_r = " + sumOfProducts("color", "rgb", ".x", channels) + ";\n" +
			"		unsigned int out_g = " + sumOfProducts("color", "rgb", ".y", channels) + ";\n" +
			"		unsigned int out_b = " + sumOfProducts("color", "rgb", ".z", channels) + ";\n" +
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
