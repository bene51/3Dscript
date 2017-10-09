package textanim;

import java.awt.Color;

import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

public interface Renderer3D {

	public KeywordFactory getKeywordFactory();

	public RenderingState getKeyframe();

	public ImageProcessor render(RenderingState kf);

	public ImagePlus getImage();

	public void setTargetSize(int w, int h);

	public int getTargetWidth();

	public int getTargetHeight();

	public void setTimelapseIndex(int t);

	public void setBackground(Color bg);

	public void setBackground(ColorProcessor bg);
}
