package animation2;

import ij.ImagePlus;
import renderer3d.CudaRaycaster;
import renderer3d.Transform;

public class RenderingThread {

	private Thread thread;
	private final Object lock = new Object();
	private boolean shutdown = false;

	private CudaRaycaster raycaster;

	final ImagePlus out;

	private Event event;

	static class Event {
		Event(RenderingSettings[] settings, float[] fwd, float[] inv, float near, float far) {
			valid = true;
			this.near = near;
			this.far = far;
			renderingSettings = new RenderingSettings[settings.length];
			for(int i = 0; i < settings.length; i++)
				renderingSettings[i] = new RenderingSettings(settings[i]);

			System.arraycopy(inv, 0, this.inverseTransform, 0, 12);
			System.arraycopy(fwd, 0, this.forwardTransform, 0, 12);
		}

		private RenderingSettings[] renderingSettings;
		private float[] inverseTransform = Transform.fromIdentity(null);
		private float[] forwardTransform = Transform.fromIdentity(null);
		private float near;
		private float far;
		private boolean valid;
		private int tgtW = -1, tgtH = -1;
		private int bbx0 = -1, bby0 = -1, bbz0 = -1, bbx1 = -1, bby1 = -1, bbz1 = -1;
		private int imaget = -1;
	}

	public RenderingThread(ImagePlus image, final RenderingSettings[] settings, final float[] fwd, final float[] inv, final float[] nearfar, final float zStep) {

		this.event = new Event(settings, fwd, inv, nearfar[0], nearfar[1]);

		raycaster = new CudaRaycaster(image, image.getWidth(), image.getHeight(), zStep);
		out = raycaster.renderAndCompose(event.forwardTransform, event.inverseTransform, settings, nearfar[0], nearfar[1]);
		out.show();

		thread = new Thread() {
			@Override
			public void run() {
				loop(settings, fwd, inv, nearfar);
			}
		};
		thread.start();
	}

	public CudaRaycaster getRaycaster() {
		return raycaster;
	}

	public void loop(RenderingSettings[] settings, float[] fwd, float[] inv, float[] nearfar) {
		Event e = new Event(settings, fwd, inv, nearfar[0], nearfar[1]);
		while(!shutdown) {
			poll(e);
			render(e);
		}
		CudaRaycaster.close();
	}

	public void push(RenderingSettings[] s, float[] fwd, float[] inv, float[] nearfar) {
		push(s, fwd, inv, nearfar, -1, -1, -1, -1, -1, -1, -1, -1, -1);
	}

	public void push(RenderingSettings[] s, float[] fwd, float[] inv, float[] nearfar, int tgtW, int tgtH) {
		push(s, fwd, inv, nearfar, tgtW, tgtH, -1, -1, -1, -1, -1, -1, -1);
	}

	public void push(RenderingSettings[] s, float[] fwd, float[] inv, float[] nearfar,
			int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1) {
		push(s, fwd, inv, nearfar, -1, -1, bbx0, bby0, bbz0, bbx1, bby1, bbz1, -1);
	}

	public void push(RenderingSettings[] s, float[] fwd, float[] inv, float[] nearfar,
			int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1, int imaget) {
		push(s, fwd, inv, nearfar, -1, -1, bbx0, bby0, bbz0, bbx1, bby1, bbz1, imaget);
	}

	public void push(RenderingSettings[] s, float[] fwd, float[] inv, float[] nearfar, int w, int h,
			int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1, int imaget) {
		synchronized(lock) {
			System.out.println("push");
			for(int i = 0; i < s.length; i++)
				event.renderingSettings[i].set(s[i]);
			event.near = nearfar[0];
			event.far = nearfar[1];
			event.valid = true;
			event.tgtW = w;
			event.tgtH = h;
			event.bbx0 = bbx0;
			event.bby0 = bby0;
			event.bbz0 = bbz0;
			event.bbx1 = bbx1;
			event.bby1 = bby1;
			event.bbz1 = bbz1;
			event.imaget = imaget;
			System.arraycopy(inv, 0, event.inverseTransform, 0, 12);
			System.arraycopy(fwd, 0, event.forwardTransform, 0, 12);
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
			for(int i = 0; i < ret.renderingSettings.length; i++)
				ret.renderingSettings[i].set(event.renderingSettings[i]);
			System.arraycopy(event.inverseTransform, 0, ret.inverseTransform, 0, 12);
			System.arraycopy(event.forwardTransform, 0, ret.forwardTransform, 0, 12);
			ret.near = event.near;
			ret.far = event.far;
			ret.tgtW = event.tgtW;
			ret.tgtH = event.tgtH;
			ret.bbx0 = event.bbx0;
			ret.bby0 = event.bby0;
			ret.bbz0 = event.bbz0;
			ret.bbx1 = event.bbx1;
			ret.bby1 = event.bby1;
			ret.bbz1 = event.bbz1;
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
		if(e.bbx0 != -1) {
			raycaster.setBBox(e.bbx0, e.bby0, e.bbz0, e.bbx1, e.bby1, e.bbz1);
			e.bbx0 = e.bby0 = e.bbz0 = e.bbx1 = e.bby1 = e.bbz1 = -1;

		}
		ImagePlus input = raycaster.getImage();
		int before = input.getT();
		if(e.imaget != -1) {
			input.setT(e.imaget);
			if(input.getT() != before)
				raycaster.setImage(input);
		}
		out.setProcessor(raycaster.renderAndCompose(e.forwardTransform, e.inverseTransform, e.renderingSettings, e.near, e.far).getProcessor());
	}
}