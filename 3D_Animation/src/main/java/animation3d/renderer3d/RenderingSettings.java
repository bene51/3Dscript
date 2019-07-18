package animation3d.renderer3d;

public class RenderingSettings {

	public float colorMin;
	public float colorMax;
	public float colorGamma;

	public float alphaMin;
	public float alphaMax;
	public float alphaGamma;

	public float weight = 1;

	public boolean useLight = false;
	public boolean useLUT = false;

	public float k_o = 1;
	public float k_d = 0;
	public float k_s = 0;
	public float shininess = 5;

	public int bbx0, bby0, bbz0, bbx1, bby1, bbz1;
	public float near, far;

	public RenderingSettings(
			float colorMin, float colorMax, float colorGamma,
			float alphaMin, float alphaMax, float alphaGamma,
			float weight,
			int bbx0, int bby0, int bbz0,
			int bbx1, int bby1, int bbz1,
			float near, float far) {
		this.alphaMin   = alphaMin;
		this.alphaMax   = alphaMax;
		this.alphaGamma = alphaGamma;
		this.colorMin   = colorMin;
		this.colorMax   = colorMax;
		this.colorGamma = colorGamma;
		this.weight     = weight;
		this.bbx0       = bbx0;
		this.bby0       = bby0;
		this.bbz0       = bbz0;
		this.bbx1       = bbx1;
		this.bby1       = bby1;
		this.bbz1       = bbz1;
		this.near       = near;
		this.far        = far;
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
		this.bbx0       = s.bbx0;
		this.bby0       = s.bby0;
		this.bbz0       = s.bbz0;
		this.bbx1       = s.bbx1;
		this.bby1       = s.bby1;
		this.bbz1       = s.bbz1;
		this.near       = s.near;
		this.far        = s.far;
		this.useLight   = s.useLight;
		this.k_o        = s.k_o;
		this.k_d        = s.k_d;
		this.k_s        = s.k_s;
		this.shininess  = s.shininess;
		this.useLUT     = s.useLUT;
	}
}
