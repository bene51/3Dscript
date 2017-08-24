package animation2;

public class RenderingSettings {

	@RenderingProperty(label = "Color Min")
	public float colorMin;
	@RenderingProperty(label = "Color Max")
	public float colorMax;
	@RenderingProperty(label = "Color Gamma")
	public float colorGamma;

	@RenderingProperty(label = "Alpha Min")
	public float alphaMin;
	@RenderingProperty(label = "Alpha Max")
	public float alphaMax;
	@RenderingProperty(label = "Alpha Gamma")
	public float alphaGamma;

	@RenderingProperty(label = "Weight")
	public float weight = 1;

	public RenderingSettings(
			float colorMin, float colorMax, float colorGamma,
			float alphaMin, float alphaMax, float alphaGamma) {
		this(colorMin, colorMax, colorGamma, alphaMin, alphaMax, alphaGamma, 1);
	}

	public RenderingSettings(
			float colorMin, float colorMax, float colorGamma,
			float alphaMin, float alphaMax, float alphaGamma,
			float weight) {
		this.alphaMin   = alphaMin;
		this.alphaMax   = alphaMax;
		this.alphaGamma = alphaGamma;
		this.colorMin   = colorMin;
		this.colorMax   = colorMax;
		this.colorGamma = colorGamma;
		this.weight     = weight;
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
		this.weight     = s.weight;
	}
}
