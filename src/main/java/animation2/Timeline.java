package animation2;

import java.util.ArrayList;
import java.util.Iterator;

public class Timeline implements Iterable<Segment> {
	
	public static void main(String[] args) {
		int fromFrame = 230;
		int zStartT0 = 0;
		int zStartT1 = 1668;
		int nFrames = 10;
		double smoothness = 0;
		
		Timeline tl = new Timeline();
		tl.addSegment(new Segment(fromFrame, zStartT0, fromFrame + nFrames - 1, zStartT1, smoothness));
		
		for(int f = 230; f < 240; f++)
			System.out.println("#" + f + ": " + tl.getInterpolatedValue(f));
	}

	private final ArrayList<Segment> segments = new ArrayList<Segment>();

	public void addSegment(Segment seg) {
		segments.add(seg);
	}

	@Override
	public Iterator<Segment> iterator() {
		return segments.iterator();
	}

	public Segment get(int i) {
		return segments.get(i);
	}

	public int size() {
		return segments.size();
	}

	public void moveSegmentStart(int i, double x, double y) {
		Segment s = segments.get(i);
		double sm = s.getSmoothness();
		s = new Segment(x, y, s.p2x, s.p2y);
		s.setSmoothness(sm);
		segments.set(i, s);
		if(i > 0) {
			s = segments.get(i - 1);
			sm = s.getSmoothness();
			s = new Segment(s.p1x, s.p1y, x, y);
			s.setSmoothness(sm);
			segments.set(i - 1, s);
		}
	}

	public void moveSegmentEnd(int i, double x, double y) {
		Segment s = segments.get(i);
		double sm = s.getSmoothness();
		s = new Segment(s.p1x, s.p1y, x, y);
		s.setSmoothness(sm);
		segments.set(i, s);
		if(i < size() - 1) {
			s = segments.get(i + 1);
			sm = s.getSmoothness();
			s = new Segment(x, y, s.p2x, s.p2y);
			s.setSmoothness(sm);
			segments.set(i + 1, s);
		}
	}

	public void replaceSegment(int idx, Segment segment) {
		segments.set(idx, segment);
		if(idx > 0) {
			Segment prev = segments.get(idx - 1);
			segments.set(idx - 1, new Segment(prev.p1x, prev.p1y, prev.p2x, segment.p1y, prev.getSmoothness()));
		}
		if(idx < size() - 1) {
			Segment next = segments.get(idx + 1);
			segments.set(idx + 1, new Segment(next.p1x, segment.p2y, next.p2x, next.p2y, next.getSmoothness()));
		}
	}

	public void removeSegment(int i) {
		segments.remove(i);
	}

	public void insertSegment(Segment segment) {
		if(segments.size() == 0) {
			segments.add(segment);
			return;
		}
		if(segment.p2x < segments.get(0).p1x) {
			segments.add(0, segment);
			return;
		}
		for(int i = 1; i < segments.size(); i++) {
			Segment prev = segments.get(i - 1);
			Segment next = segments.get(i);
			System.out.println("insert: " + segment);
			System.out.println("  prev = " + prev);
			System.out.println("  next = " + next);
			if(segment.p1x > prev.p2x && segment.p1x < next.p1x && segment.p2x > next.p1x) {
				throw new RuntimeException("Overlapping segments");
			}
			if(segment.p2x < next.p1x && segment.p2x > prev.p2x && segment.p1x < prev.p2x) {
				throw new RuntimeException("Overlapping segments");
			}
			if(segment.p1x > prev.p2x && segment.p2x < next.p1x) {
				segments.set(i - 1, new Segment(prev.p1x, prev.p1y, prev.p2x, segment.p1y, prev.getSmoothness()));
				segments.set(i, new Segment(next.p1x, segment.p2y, next.p2x, next.p2y, next.getSmoothness()));
				segments.add(i, segment);
				return;
			}
		}
		if(segment.p1x > segments.get(size() - 1).p2x) {
			segments.add(segment);
		} else {
			throw new RuntimeException("Don't know were to insert segment " + segment);
		}
	}

	public int getSegment(double x) {
		if(segments.size() == 0)
			return -1;
		for (int i = 0; i < segments.size(); i++) {
			Segment si = segments.get(i);
			if (x >= si.p1x && x <= si.p2x)
				return i;
		}
		return -1;
	}

	public double getInterpolatedValue(double x) {
		if (segments.size() == 0)
			return 0;

		for (int i = 0; i < segments.size(); i++) {
			Segment si = segments.get(i);
			if (x < si.p2x)
				return si.getInterpolatedValue(x);
		}
		return segments.get(segments.size() - 1).p2y;
	}
}