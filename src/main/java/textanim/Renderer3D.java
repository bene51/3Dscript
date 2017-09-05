package textanim;

import java.awt.Color;

import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import renderer3d.Keyframe;

public interface Renderer3D {

	public Keyframe getKeyframe();

	public ImageProcessor render(Keyframe kf);

	public ImagePlus getImage();

	public void setTargetSize(int w, int h);

	public int getTargetWidth();

	public int getTargetHeight();

	public void setTimelapseIndex(int t);

	public void setBackground(Color bg);

	public void setBackground(ColorProcessor bg);

	public void setZStep(double zStep);
}
