package animation3d.renderer3d;

public enum RenderingAlgorithm {

	INDEPENDENT_TRANSPARENCY,
	COMBINED_TRANSPARENCY,
	MAXIMUM_INTENSITY;

	/**
	 * returns
	 * - MAXIMUM_INTENSITY if d < 0,
	 * - INDEPENDENT_TRANSPARENCY if round(d) == 0 and
	 * - COMBINED_TRANSPARENCY if round(d) == 1
	 */
	static RenderingAlgorithm fromDouble(double d) {
		if(d < 0) return MAXIMUM_INTENSITY;
		int di = (int)Math.round(d);
		if(di == 0) return INDEPENDENT_TRANSPARENCY;
		return COMBINED_TRANSPARENCY;
	}

	double toDouble() {
		switch(this) {
			case INDEPENDENT_TRANSPARENCY: return 0;
			case MAXIMUM_INTENSITY: return -1;
			case COMBINED_TRANSPARENCY: return 1;
		}
		return 0;
	}

	static double getCombinedAlphaWeight(double d) {
		if(d < 0) return 0;
		if(d > 1) return 1;
		return d;
	}
}
