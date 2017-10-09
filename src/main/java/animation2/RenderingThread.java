package animation2;

import ij.ImagePlus;
import ij.measure.Calibration;
import renderer3d.CudaRaycaster;
import renderer3d.ExtendedRenderingState;
import renderer3d.Renderer3DAdapter;

public class RenderingThread {

	private Thread thread;
	private final Object lock = new Object();
	private boolean shutdown = false;

	private Renderer3DAdapter raycaster;

	final ImagePlus out;

	private Event event;

	static class Event {

		private ExtendedRenderingState rs;
		private int tgtW = -1;
		private int tgtH = -1;
		private int imaget = -1;
		private boolean valid;

		Event(ExtendedRenderingState rs) {
			valid = true;
			this.rs = rs.clone();
		}
	}

	public RenderingThread(Renderer3DAdapter raycaster) {
		this.raycaster = raycaster;
		final ExtendedRenderingState rs = raycaster.getRenderingState();
		this.event = new Event(rs);
		out = new ImagePlus("3D Animation", raycaster.render(rs));
		// TODO
		Calibration cal = out.getCalibration();
		cal.setUnit(raycaster.getImage().getCalibration().getUnit());
		rs.getFwdTransform().adjustOutputCalibration(cal);
		out.show();

		thread = new Thread() {
			@Override
			public void run() {
				loop(rs);
			}
		};
		thread.start();
	}

	public void loop(ExtendedRenderingState rs) {
		Event e = new Event(rs);
		while(!shutdown) {
			poll(e);
			render(e);
		}
		CudaRaycaster.close();
	}

	public void push(ExtendedRenderingState rs, int w, int h, int imaget) {
		synchronized(lock) {
			event.rs.setFrom(rs);
			event.valid = true;
			event.tgtW = w;
			event.tgtH = h;
			event.imaget = imaget;
		}
		synchronized(this) {
			notifyAll();
		}
	}

	public Event poll(Event ret) {
		if(!event.valid) {
			synchronized(this) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		synchronized(lock) {
			ret.rs.setFrom(event.rs);
			ret.tgtW = event.tgtW;
			ret.tgtH = event.tgtH;
			ret.imaget = event.imaget;
			event.valid = false;
		}
		return ret;
	}

	public void shutdown() {
		shutdown = true;
		synchronized(this) {
			notifyAll();
		}
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void render(Event e) {
		if(e.tgtW != -1 && e.tgtH != -1) {
			raycaster.setTgtSize(e.tgtW, e.tgtH);
			e.tgtW = e.tgtH = -1;

		}
		ImagePlus input = raycaster.getImage();
		int before = input.getT();
		if(e.imaget != -1) {
			input.setT(e.imaget);
			if(input.getT() != before)
				raycaster.setImage(input);
		}
		out.setProcessor(raycaster.render(e.rs));
	}
}