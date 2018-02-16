package renderer3d;

public class RenderingSettings {

	public float colorMin;
	public float colorMax;
	public float colorGamma;

	public float alphaMin;
	public float alphaMax;
	public float alphaGamma;

	public float weight = 1;

	public boolean useLight = true;

	public float k_o = 1;
	public float k_d = 0;
	public float k_s = 0;
	public float shininess = 5;

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
		this.useLight   = s.useLight;
		this.k_o        = s.k_o;
		this.k_d        = s.k_d;
		this.k_s        = s.k_s;
		this.shininess  = s.shininess;
	}
}
