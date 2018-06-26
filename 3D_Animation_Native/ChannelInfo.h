#ifndef __CHANNEL_INFO_H__
#define __CHANNEL_INFO_H__
class ChannelInfo
{
private:

public:
	float cmin, cmax, cgamma;
	float amin, amax, agamma;

	float weight;

	int r, g, b;

	int useLight;
	float k_o, k_d, k_s, shininess;

	int bbx0, bby0, bbz0, bbx1, bby1, bbz1;

	float zStart;
	float zEnd;

	ChannelInfo(
			float cmin, float cmax, float cgamma,
			float amin, float amax, float agamma,
			float weight,
			int r, int g, int b,
			int bbx0, int bby0, int bbz0,
			int bbx1, int bby1, int bbz1,
			float zStart, float zEnd,
			int useLight,
			float k_o, float k_d, float k_s, float shininess)
	{
		setContrast(cmin, cmax, cgamma, amin, amax, agamma);
		setWeight(weight);
		setColor(r, g, b);
		setLight(useLight, k_o, k_d, k_s, shininess);
		setBoundingBox(bbx0, bby0, bbz0, bbx1, bby1, bbz1);
		setNearAndFar(zStart, zEnd);
	}

	void setContrast(
			float cmin, float cmax, float cgamma,
			float amin, float amax, float agamma)
	{
		this->cmin = cmin;
		this->cmax = cmax;
		this->cgamma = cgamma;
		this->amin = amin;
		this->amax = amax;
		this->agamma = agamma;
	}

	void setColor(int r, int g, int b)
	{
		this->r = r;
		this->g = g;
		this->b = b;
	}

	void setWeight(float weight)
	{
		this->weight = weight;
	}

	void setBoundingBox(int bbx0, int bby0, int bbz0,
			int bbx1, int bby1, int bbz1)
	{
		this->bbx0 = bbx0;
		this->bby0 = bby0;
		this->bbz0 = bbz0;
		this->bbx1 = bbx1;
		this->bby1 = bby1;
		this->bbz1 = bbz1;
	}

	void setNearAndFar(float zStart, float zEnd)
	{
		this->zStart = zStart;
		this->zEnd  = zEnd;
	}

	void setLight(int useLight,
			float object,
			float diffuse,
			float specular,
			float shininess)
	{
		this->useLight = useLight;
		this->k_o = object;
		this->k_d = diffuse;
		this->k_s = specular;
		this->shininess = shininess;
	}
};
#endif

