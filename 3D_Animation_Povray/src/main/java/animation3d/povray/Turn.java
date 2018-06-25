package animation3d.povray;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

public class Turn {

	public static ImagePlus toZY(ImagePlus image) {
		int w = image.getWidth();
		int h = image.getHeight();
		int d = image.getNSlices();

		ImageProcessor[] slices = new ImageProcessor[d];
		for(int z = 0; z < d; z++) {
			int idx = image.getStackIndex(image.getC(), z + 1, image.getT());
			slices[z] = image.getStack().getProcessor(idx);
		}

		// x = z, y = y, z = -x
		int neww = d, newh = h, newd = w;
		ImageStack stack = new ImageStack(neww, newh);
		for(int z = 0; z < newd; z++) {
			int oldX = newd - z - 1;
			ImageProcessor ip = image.getProcessor().createProcessor(neww, newh);
			for(int y = 0; y < newh; y++) {
				int oldY = y;
				for(int x = 0; x < neww; x++) {
					int oldZ = x;
					ip.setf(x, y, slices[oldZ].getf(oldX, oldY));
				}
			}
			stack.addSlice(ip);
		}
		ImagePlus ret = new ImagePlus(image.getTitle(), stack);
		Calibration cal0 = image.getCalibration();
		Calibration cal1 = cal0.copy();
		cal1.pixelWidth  = cal0.pixelDepth;
		cal1.pixelHeight = cal0.pixelHeight;
		cal1.pixelDepth  = cal0.pixelWidth;
		ret.setCalibration(cal1);
		return ret;
	}

	public static ImagePlus toXZ(ImagePlus image) {
		int w = image.getWidth();
		int h = image.getHeight();
		int d = image.getNSlices();

		ImageProcessor[] slices = new ImageProcessor[d];
		for(int z = 0; z < d; z++) {
			int idx = image.getStackIndex(image.getC(), z + 1, image.getT());
			slices[z] = image.getStack().getProcessor(idx);
		}

		// x = x, y = z, z = -y
		int neww = w, newh = d, newd = h;
		ImageStack stack = new ImageStack(neww, newh);
		for(int z = 0; z < newd; z++) {
			int oldY = newd - z - 1;
			ImageProcessor ip = image.getProcessor().createProcessor(neww, newh);
			for(int y = 0; y < newh; y++) {
				int oldZ = y;
				for(int x = 0; x < neww; x++) {
					int oldX = x;
					ip.setf(x, y, slices[oldZ].getf(oldX, oldY));
				}
			}
			stack.addSlice(ip);
		}
		ImagePlus ret = new ImagePlus(image.getTitle(), stack);
		Calibration cal0 = image.getCalibration();
		Calibration cal1 = cal0.copy();
		cal1.pixelWidth  = cal0.pixelWidth;
		cal1.pixelHeight = cal0.pixelDepth;
		cal1.pixelDepth  = cal0.pixelHeight;
		ret.setCalibration(cal1);
		return ret;
	}
}
