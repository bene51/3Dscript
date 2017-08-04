package animation2;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import animation2.Keyframe.KeyframeProperty;

public class Timelines {

	private final List<CtrlPoints> timelines = new ArrayList<CtrlPoints>();

	private final int nChannels;

	public static String getName(int i) {
		Keyframe kf = Keyframe.createEmptyKeyframe();
		KeyframeProperty[] props = kf.getRenderingProperties();

		int nonChannelProps = Keyframe.getNumberOfNonChannelProperties();
		int channelProps = Keyframe.getNumberOfChannelProperties();

		if(i < Keyframe.getNumberOfNonChannelProperties())
			return props[i].getLabel();

		int rem = (i - nonChannelProps) % channelProps;
		int cha = (i - nonChannelProps) / channelProps;

		return "Channel " + (cha + 1) + " " + props[nonChannelProps + rem].getLabel();
	}

	public Timelines(int nChannels) {
		this.nChannels = nChannels;
		int nonChannelProps = Keyframe.getNumberOfNonChannelProperties();
		int channelProps = Keyframe.getNumberOfChannelProperties();

		int n = nonChannelProps + nChannels * channelProps;
		for(int i = 0; i < n; i++)
			timelines.add(new CtrlPoints());
	}

	public void getBoundingBox(Point ll, Point ur) {
		ll.set(Integer.MAX_VALUE, Double.POSITIVE_INFINITY);
		ur.set(Integer.MIN_VALUE, Double.NEGATIVE_INFINITY);
		for(CtrlPoints c : timelines)
			c.getBoundingBox(ll, ur);
		if(ll.x == Integer.MAX_VALUE)
			ll.x = 0;
		if(ur.x == Integer.MIN_VALUE)
			ur.x = ll.x + 1;
		if(ll.y == Double.POSITIVE_INFINITY)
			ll.y = 0;
		if(ur.y == Double.NEGATIVE_INFINITY)
			ur.y = ll.y + 1;
		if(ur.x == ll.x)
			ur.x += 1;
		if(ur.y == ll.y)
			ur.y += 1;
	}

	public int getNChannels() {
		return nChannels;
	}

	public static void main(String[] args) {
		toJSON();
	}

	public static void toJSON() {
		Keyframe kf = new Keyframe(1,
				new RenderingSettings[] {
						new RenderingSettings(0, 255, 1, 0, 255, 2),
						new RenderingSettings(0, 255, 1, 0, 255, 2)
				},
				0, // near
				100, // far
				1, // scale,
				1, // dx,
				1, // dy,
				1, // dz,
				0, // angleX,
				0, // double angleY,
				0, // double angleZ,
				0, // int bbx,
				0, // int bby,
				0, // int bbz,
				256, // int bbw,
				256, // int bbh,
				57); // int bbd) {

		Gson gson = new GsonBuilder()
	             .disableHtmlEscaping()
	             .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
	             .setPrettyPrinting()
	             .serializeNulls()
	             .create();
		System.out.println(gson.toJson(kf));
	}

	public void clear() {
		for(CtrlPoints ctrls : timelines)
			ctrls.clear();
	}

	public boolean isEmpty() {
		for(CtrlPoints ctrls : timelines)
			if(ctrls.size() > 0)
				return false;
		return true;
	}

	private void record(int i, int t, double v) {
		if(v != Keyframe.UNSET)
			timelines.get(i).add(t, v);
		else {
			LinePoint lp = timelines.get(i).getPointAt(t);
			timelines.get(i).remove(lp);
		}
	}

	public void recordFrame(Keyframe kf) {
		int i = 0;
		int t = kf.getFrame();

		KeyframeProperty[] props = kf.getRenderingProperties();
		for(KeyframeProperty p : props)
			record(i++, t, p.getValue());
	}

	double getInterpolatedValue(int i, int t, double def) {
		double v = timelines.get(i).getInterpolatedValue(t);
		if(v == Keyframe.UNSET)
			return def;
		return v;
	}

	public Keyframe getInterpolatedFrame(int t, Keyframe def) {
		Keyframe kfr = Keyframe.createEmptyKeyframe(nChannels);
		kfr.setFrame(t);

		KeyframeProperty[] kfrProps = kfr.getRenderingProperties();
		KeyframeProperty[] defProps = def.getRenderingProperties();

		for(int i = 0; i < kfrProps.length; i++)
			kfrProps[i].setValue(getInterpolatedValue(i, t, defProps[i].getValue()));

		return kfr;
	}

	public Keyframe getInterpolatedFrame(int t) {
		Keyframe kfr = Keyframe.createEmptyKeyframe(nChannels);
		kfr.setFrame(t);

		KeyframeProperty[] kfrProps = kfr.getRenderingProperties();

		for(int i = 0; i < kfrProps.length; i++)
			kfrProps[i].setValue(timelines.get(i).getInterpolatedValue(t));

		return kfr;
	}

	private double get(int i, int t) {
		LinePoint lp = timelines.get(i).getPointAt(t);
		if(lp == null)
			return Keyframe.UNSET;
		return lp.y;
	}

	public Keyframe getKeyframeNoInterpol(int t) {
		Keyframe kf = Keyframe.createEmptyKeyframe(nChannels);
		kf.setFrame(t);

		KeyframeProperty[] kfrProps = kf.getRenderingProperties();
		for(int i = 0; i < kfrProps.length; i++)
			kfrProps[i].setValue(get(i, t));

		return kf;
	}

	public CtrlPoints get(int i) {
		return timelines.get(i);
	}

	public int size() {
		return timelines.size();
	}
}
