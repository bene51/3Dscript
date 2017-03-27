package animation2;

import ij.ImagePlus;

public interface ImageSource {

	public ImagePlus getInputImage(int t);

	public boolean hasChanged(int t);

}
