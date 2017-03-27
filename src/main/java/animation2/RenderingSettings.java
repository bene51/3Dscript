package animation2;

public class RenderingSettings {

	public float alphaMin, alphaMax, alphaGamma;
	public float colorMin, colorMax, colorGamma;

	public RenderingSettings(
			float alphaMin, float alphaMax, float alphaGamma,
			float colorMin, float colorMax, float colorGamma) {
		this.alphaMin   = alphaMin;
		this.alphaMax   = alphaMax;
		this.alphaGamma = alphaGamma;
		this.colorMin   = colorMin;
		this.colorMax   = colorMax;
		this.colorGamma = colorGamma;
	}

	public RenderingSettings(RenderingSettings s) {
		set(s);
	}

	public void set(RenderingSettings s) {
		this.alphaMin   = s.alphaMin;
		this.alphaMax   = s.alphaMax;
		this.alphaGamma = s.alphaGamma;
		this.colorMin   = s.colorMin;
		this.colorMax   = s.colorMax;
		this.colorGamma = s.colorGamma;
	}
}
