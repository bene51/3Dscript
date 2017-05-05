package animation2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonExporter {

	public static void exportTimelines(Timelines timelines, File f) throws IOException {
		PrintStream out = new PrintStream(new FileOutputStream(f));
		out.println(exportTimelines(timelines));
		out.close();
	}

	public static String exportTimelines(Timelines timelines) {
		JsonExporter exporter = new JsonExporter(timelines);
		JSONKeyframe[] frames = exporter.createKeyframes();
		Gson gson = new GsonBuilder()
				.disableHtmlEscaping()
				.setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
				.setPrettyPrinting()
				// .serializeNulls()
				.create();
		return gson.toJson(frames);
	}

	public static void importTimelines(Timelines timelines, String text) {
		Gson gson = new GsonBuilder()
				.disableHtmlEscaping()
				.setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
				.setPrettyPrinting()
				// .serializeNulls()
				.create();
		JSONKeyframe[] kf = gson.fromJson(text, JSONKeyframe[].class);
		JsonExporter importer = new JsonExporter(timelines);
		importer.importKeyframes(kf);
	}

	public static void importTimelines(Timelines timelines, File file) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line;
		while((line = in.readLine()) != null) {
			sb.append(line);
		}
		in.close();
		importTimelines(timelines, sb.toString());
	}

	public static void main(String[] args) throws IOException {
		File f = new File("C:\\users\\bschmid\\Desktop\\animation.json");
		Timelines timelines = new Timelines(2, 0, 100);
		importTimelines(timelines, f);
	}

	private Timelines timelines;

	private JsonExporter(Timelines timelines) {
		this.timelines = timelines;
	}

	private JSONKeyframe[] createKeyframes() {
		HashSet<Integer> indices = new HashSet<Integer>();
		for(int i = 0; i < timelines.size(); i++) {
			CtrlPoints curve = timelines.get(i);
			for(LinePoint lp : curve)
				indices.add(lp.x);
		}
		Integer[] timepoints = new Integer[indices.size()];
		indices.toArray(timepoints);
		Arrays.sort(timepoints);

		JSONKeyframe[] kf = new JSONKeyframe[timepoints.length];
		for(int i = 0; i < timepoints.length; i++) {
			int t = timepoints[i];
			kf[i] = getKeyframeNoInterpol(t);
		}
		return kf;
	}

	private void importKeyframes(JSONKeyframe[] keyframes) {
		timelines.clear();
		for(JSONKeyframe kf : keyframes)
			recordFrame(kf);
	}

	private void record(int i, int t, double[] v) {
		if(v != null) {
			LinePoint lp = new LinePoint(t, v[0]);
			if(v.length >= 5) {
				lp.c1.set(lp.x + (int)v[1], lp.y + v[2]);
				lp.c2.set(lp.x + (int)v[3], lp.y + v[4]);
			}
			timelines.get(i).add(lp);
		} else {
			LinePoint lp = timelines.get(i).getPointAt(t);
			timelines.get(i).remove(lp);
		}
	}

	private void recordFrame(JSONKeyframe kf) {
		int i = 0;
		int t = kf.frame;

		record(i++, t, kf.dx);
		record(i++, t, kf.dy);
		record(i++, t, kf.dz);

		record(i++, t, kf.angleX);
		record(i++, t, kf.angleY);
		record(i++, t, kf.angleZ);

		record(i++, t, kf.scale);

		record(i++, t, kf.bbx0);
		record(i++, t, kf.bby0);
		record(i++, t, kf.bbz0);
		record(i++, t, kf.bbx1);
		record(i++, t, kf.bby1);
		record(i++, t, kf.bbz1);

		record(i++, t, kf.near);
		record(i++, t, kf.far);

		if(kf.renderingSettings != null) {
			for(int c = 0; c < kf.renderingSettings.length; c++) {
				JSONRenderingSettings rs = kf.renderingSettings[c];
				if(rs != null) {
					record(i++, t, rs.colorMin);
					record(i++, t, rs.colorMax);
					record(i++, t, rs.colorGamma);
					record(i++, t, rs.alphaMin);
					record(i++, t, rs.alphaMax);
					record(i++, t, rs.alphaGamma);
				}
			}
		}
	}

	private double[] get(int i, int t) {
		LinePoint lp = timelines.get(i).getPointAt(t);
		if(lp == null)
			return null;
		if(!hasCtrls(lp))
			return new double[] {lp.y};
		return new double[] {lp.y, lp.c1.x - lp.x, lp.c1.y - lp.y, lp.c2.x - lp.x, lp.c2.y - lp.y};
	}

	private static boolean hasCtrls(LinePoint lp) {
		return lp.x != lp.c1.x || lp.y != lp.c1.y || lp.x != lp.c2.x || lp.y != lp.c2.y;
	}

	private JSONKeyframe getKeyframeNoInterpol(int t) {
		JSONKeyframe kf = new JSONKeyframe(t);
		int i = 0;

		kf.dx = get(i++, t);
		kf.dy = get(i++, t);
		kf.dz = get(i++, t);

		kf.angleX = get(i++, t);
		kf.angleY = get(i++, t);
		kf.angleZ = get(i++, t);

		kf.scale = get(i++, t);

		kf.bbx0 = get(i++, t);
		kf.bby0 = get(i++, t);
		kf.bbz0 = get(i++, t);
		kf.bbx1 = get(i++, t);
		kf.bby1 = get(i++, t);
		kf.bbz1 = get(i++, t);

		kf.near = get(i++, t);
		kf.far  = get(i++, t);

		int nChannels = timelines.getNChannels();
		kf.renderingSettings = new JSONRenderingSettings[nChannels];
		for(int c = 0; c < nChannels; c++) {
			kf.renderingSettings[c] = new JSONRenderingSettings();
			kf.renderingSettings[c].alphaMin   = get(i + 3, t);
			kf.renderingSettings[c].alphaMax   = get(i + 4, t);
			kf.renderingSettings[c].alphaGamma = get(i + 5, t);
			kf.renderingSettings[c].colorMin   = get(i + 0, t);
			kf.renderingSettings[c].colorMax   = get(i + 1, t);
			kf.renderingSettings[c].colorGamma = get(i + 2, t);
			i += 6;
		}
		return kf;
	}

	private static final class JSONRenderingSettings {
		private double[] alphaMin, alphaMax, alphaGamma;
		private double[] colorMin, colorMax, colorGamma;
	}

	private static final class JSONKeyframe {

		private int frame;

		private JSONRenderingSettings[] renderingSettings;
		private double[] near, far;

		private double[] scale;
		private double[] dx, dy, dz;
		private double[] angleX, angleY, angleZ;
		private double[] bbx0, bby0, bbz0, bbx1, bby1, bbz1;

		JSONKeyframe(int t) {
			frame = t;
		}
	}
}
