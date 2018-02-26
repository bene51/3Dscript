package povray;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import parser.Keyword;
import textanim.CombinedTransform;
import textanim.IKeywordFactory;
import textanim.IRenderer3D;
import textanim.RenderingState;

public class PovrayRenderer implements IRenderer3D {

	private PovrayRenderingState rs;
	private ImagePlus input;
	private int tgtW;
	private int tgtH;
	private File outputDirectory;

	private static final String POVRAY_CMD = "C:\\Program Files\\POV-Ray\\v3.7-beta\\bin\\pvengine64.exe /Render %s /exit";

	public PovrayRenderer(ImagePlus input, File outputDirectory, int tgtW, int tgtH) {
		this.input = input;
		this.outputDirectory = outputDirectory;
		this.tgtW = tgtW;
		this.tgtH = tgtH;

		float[] pdIn = new float[] {
				(float)input.getCalibration().pixelWidth,
				(float)input.getCalibration().pixelHeight,
				(float)input.getCalibration().pixelDepth
		};

		float[] pdOut = new float[] {pdIn[0], pdIn[0], pdIn[0]};

		float[] rotcenter = new float[] {
				input.getWidth()   * pdIn[0] / 2,
				input.getHeight()  * pdIn[1] / 2,
				input.getNSlices() * pdIn[2] / 2};

		CombinedTransform transformation = new CombinedTransform(pdIn, pdOut, rotcenter);
		this.rs = new PovrayRenderingState(0, transformation);
	}

	@Override
	public IKeywordFactory getKeywordFactory() {
		return new IKeywordFactory() {
			@Override
			public Keyword[] getChannelKeywords() {
				return new Keyword[] {};
			}

			@Override
			public Keyword[] getNonChannelKeywords() {
				return PovrayKeyword.values();
			}
		};
	}

	@Override
	public RenderingState getRenderingState() {
		return rs;
	}

	@Override
	public ImageProcessor render(RenderingState kf) {
		try {
			return doRender(kf);
		} catch(Exception e) {
			throw new RuntimeException("Exception rendering frame", e);
		}
	}

	private ImageProcessor doRender(RenderingState kf) throws IOException, InterruptedException {
		File iniFile = new File(outputDirectory, "scene.ini");
		PrintStream out = new PrintStream(iniFile);
		out.println("Antialias=On");
		out.println("Antialias_Threshold=0.3");
		out.println("Antialias_Depth=2");
		out.println("Input_File_Name=\"scene.pov\"");
		out.println("Output_File_Name=\"scene.png\"");
		out.println("Width=" + tgtW);
		out.println("Height=" + tgtH);
		out.close();

		File povrayFile = new File(outputDirectory, "scene.pov");
		out = new PrintStream(povrayFile);
		out.println(PovrayTemplate.text);
		out.println(PovrayTemplate.getMagnifierAt(
				(float)kf.getNonChannelProperty(PovrayRenderingState.LENS_X),
				(float)kf.getNonChannelProperty(PovrayRenderingState.LENS_Y),
				(float)kf.getNonChannelProperty(PovrayRenderingState.LENS_Z)));
		out.println(createPovrayVolume(kf.getFwdTransform()));
		out.close();

		while(!iniFile.exists())
			Thread.sleep(100);

		System.out.println(String.format(POVRAY_CMD, "scene.ini"));
		Process process = Runtime.getRuntime().exec(String.format(POVRAY_CMD, iniFile.getAbsolutePath(), new String[] {}, outputDirectory));
		process.waitFor();

		File outFile = new File(outputDirectory, "scene.png");
		ImagePlus output = IJ.openImage(outFile.getAbsolutePath());

		return output.getProcessor();
	}

	@Override
	public ImagePlus getImage() {
		return input;
	}

	@Override
	public void setTargetSize(int w, int h) {
		this.tgtW = w;
		this.tgtH = h;
	}

	@Override
	public int getTargetWidth() {
		return tgtW;
	}

	@Override
	public int getTargetHeight() {
		return tgtH;
	}

	@Override
	public void setTimelapseIndex(int t) {
	}

	private String createPovrayVolume(CombinedTransform transform) {
		int w = input.getWidth();
		int h = input.getHeight();
		int d = input.getNSlices();
		double pw = input.getCalibration().pixelWidth;
		double ph = input.getCalibration().pixelHeight;
		double pd = input.getCalibration().pixelDepth;

		double dw = w * pw;
		double dh = h * ph;
		double dd = d * pd;

		double sx = 1;
		double sy = dh / dw;
		double sz = dd / dw;

		float[] rot = transform.getRotation();
		float[] translation = transform.getTranslation();

		float dirx = Math.abs(rot[2]);
		float diry = Math.abs(rot[6]);
		float dirz = Math.abs(rot[10]);

		String volume = "";

		if(dirz >= dirx && dirz >= diry)
			volume = "xyvolume";
		else if(diry >= dirx && diry >= dirz)
			volume = "xzvolume";
		else if(dirx >= diry && dirx >= dirz)
			volume = "zyvolume";

		float[] eulers = transform.guessEulerAnglesDegree();

		float scale = transform.getScale();
		float dx = translation[0];
		float dy = translation[1];
		float dz = translation[2];
		float ax = -eulers[0];
		float ay = 	eulers[1];
		float az = -eulers[2];

		scale = 3;
		dy = 1.9f;
		dz = 4;

		String s =
				"#declare xyvolume =\n" +
				"union {\n" +
				"#for (I, 0, " + (d - 1) + ", 1) \n" +
				"box { <0, 0, 0>,<1, 1, 0.01> hollow\n" +
				"    texture {\n" +
				"        pigment{ \n" +
				"            // color Red\n" +
				"            image_map {png concat(\"xy/\", str(I, -4, 0), \".png\")}\n" +
				"        }\n" +
				"    }\n" +
				"    finish {ambient 0.2}\n" +
				"    scale <" + sx + ", " + sy + ", 1>\n" +
				"    translate<-0.5 * " + sx + ", -0.5 * " + sy + ", (-" + (d / 2.0) + " + I) * " + (pd / dw) + ">\n" +
				"}\n" +
				"#end\n" +
				"}\n\n" +

				"#declare zyvolume =\n" +
				"union {\n" +
				"#for (I, 0, " + (w - 1) + ", 1) \n" +
				"box { <0, 0, 0>,<1, 1, 0.01> hollow\n" +
				"    texture {\n" +
				"        pigment{ \n" +
				"            color Red\n" +
				"            // image_map {png concat(\"zy/\", str(I, -4, 0), \".png\")}\n" +
				"        }\n" +
				"    }\n" +
				"    finish {ambient 0.2}\n" +
				"    rotate <0, -90, 0>\n" +
				"    scale <1, " + sy + ", " + sz + ">\n" +
				"    translate<(+" + (w/2.0) + " - I) * " + (pw / dw) + ", -0.5 * " + sy + ", -0.5 * " + sz + ">\n" +
				"}\n" +
				"#end\n" +
				"}\n\n" +

				"#declare xzvolume =\n" +
				"union {\n" +
				"#for (I, 0, " + (h - 1) + ", 1) \n" +
				"box { <0, 0, 0>,<1, 1, 0.01> hollow\n" +
				"    texture {\n" +
				"        pigment{ \n" +
				"            color Red\n" +
				"            // image_map {png concat(\"xz/\", str(I, -4, 0), \".png\")}\n" +
				"        }\n" +
				"    }\n" +
				"    finish {ambient 0.5}\n" +
				"    rotate <-90, 0, 0>\n" +
				"    scale <" + sx + ", 1, " + sz + ">\n" +
				"    translate<-0.5 * " + sx + ", (+" + (h/2.0) + " - I) * " + (ph / dw) + ", -0.5 * " + sz + ">\n" +
				"}\n" +
				"#end\n" +
				"}\n\n" +

				"object {" + volume + "\n" +
				"    scale " + scale + "\n" +
				"    rotate<" + ax + ", 0, 0>\n" +
				"    rotate<0, " + ay + ", 0>\n" +
				"    rotate<0, 0, " + az + ">\n" +
				"    translate<" + dx + ", " + dy + ", " + dz + ">\n" +
				"}\n\n" +
				"/*\n" +
				"sphere {<0, 2, 0> 2\n" +
				"    texture { pigment { rgbt <20, 20, 20, 0.99>}}\n" +
				"    finish {ambient 0 reflection 0.1}\n" +
				"}*/\n";
		return s;
	}

	public static void main(String[] args) throws Exception {
		ImagePlus imp = IJ.openImage("D:\\povray\\transparencytest\\original\\DD_lyse_1.subtracted.small.rgb.tif");
		PovrayRenderer pr = new PovrayRenderer(imp, new File("D:\\povray\\transparencytest\\original"), 640, 480);
		pr.doRender(pr.getRenderingState());
	}
}
