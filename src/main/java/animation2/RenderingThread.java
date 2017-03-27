package animation2;

import ij.ImagePlus;

public class RenderingThread {

	private Thread thread;
	private final Object lock = new Object();
	private boolean shutdown = false;

	private CudaRaycaster raycaster;

	final ImagePlus out;

	private Event event;

	static class Event {
		Event(RenderingSettings[] settings, float[] inverseTransform, float near, float far) {
			valid = true;
			this.near = near;
			this.far = far;
			renderingSettings = new RenderingSettings[settings.length];
			for(int i = 0; i < settings.length; i++)
				renderingSettings[i] = new RenderingSettings(settings[i]);

			System.arraycopy(inverseTransform, 0, this.inverseTransform, 0, 12);
		}

		private RenderingSettings[] renderingSettings;
		private float[] inverseTransform = Transform.fromIdentity(null);
		private float near;
		private float far;
		private boolean valid;
		private int tgtW = -1, tgtH = -1;
		private int bbx = -1, bby = -1, bbz = -1, bbw = -1, bbh = -1, bbd = -1;
	}

	public RenderingThread(ImagePlus image, final RenderingSettings[] settings, final float[] inv, final float[] nearfar) {

		this.event = new Event(settings, inv, nearfar[0], nearfar[1]);

		raycaster = new CudaRaycaster(image, image.getWidth(), image.getHeight(), 1);
		out = raycaster.renderAndCompose(event.inverseTransform, settings, nearfar[0], nearfar[1]);
		out.show();

		thread = new Thread() {
			@Override
			public void run() {
				loop(settings, inv, nearfar);
			}
		};
		thread.start();
	}

	public CudaRaycaster getRaycaster() {
		return raycaster;
	}

	public void loop(RenderingSettings[] settings, float[] inverseTransform, float[] nearfar) {
		Event e = new Event(settings, inverseTransform, nearfar[0], nearfar[1]);
		while(!shutdown) {
			poll(e);
			render(e);
		}
		CudaRaycaster.close();
	}

	public void push(RenderingSettings[] s, float[] transform, float[] nearfar) {
		push(s, transform, nearfar, -1, -1, -1, -1, -1, -1, -1, -1);
	}

	public void push(RenderingSettings[] s, float[] transform, float[] nearfar, int tgtW, int tgtH) {
		push(s, transform, nearfar, tgtW, tgtH, -1, -1, -1, -1, -1, -1);
	}

	public void push(RenderingSettings[] s, float[] transform, float[] nearfar,
			int bbx, int bby, int bbz, int bbw, int bbh, int bbd) {
		push(s, transform, nearfar, -1, -1, bbx, bby, bbz, bbw, bbh, bbd);
	}

	public void push(RenderingSettings[] s, float[] transform, float[] nearfar, int w, int h,
			int bbx, int bby, int bbz, int bbw, int bbh, int bbd) {
		synchronized(lock) {
			System.out.println("push");
			for(int i = 0; i < s.length; i++)
				event.renderingSettings[i].set(s[i]);
			event.near = nearfar[0];
			event.far = nearfar[1];
			event.valid = true;
			event.tgtW = w;
			event.tgtH = h;
			event.bbx = bbx;
			event.bby = bby;
			event.bbz = bbz;
			event.bbw = bbw;
			event.bbh = bbh;
			event.bbd = bbd;
			System.arraycopy(transform, 0, event.inverseTransform, 0, 12);
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
			ret.near = event.near;
			ret.far = event.far;
			ret.tgtW = event.tgtW;
			ret.tgtH = event.tgtH;
			ret.bbx = event.bbx;
			ret.bby = event.bby;
			ret.bbz = event.bbz;
			ret.bbw = event.bbw;
			ret.bbh = event.bbh;
			ret.bbd = event.bbd;
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
		if(e.bbx != -1) {
			raycaster.setBBox(e.bbx, e.bby, e.bbz, e.bbw, e.bbh, e.bbd);
			e.bbx = e.bby = e.bbz = e.bbw = e.bbh = e.bbd = -1;

		}
		out.setProcessor(raycaster.renderAndCompose(e.inverseTransform, e.renderingSettings, e.near, e.far).getProcessor());
	}
}