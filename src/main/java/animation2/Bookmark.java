package animation2;

import renderer3d.ExtendedRenderingState;

public class Bookmark {

	private final ExtendedRenderingState renderingState;

	private String name;

	public Bookmark(String name, ExtendedRenderingState rs) {
		this.name = name;
		this.renderingState = rs;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ExtendedRenderingState getRenderingState() {
		return renderingState;
	}

	@Override
	public String toString() {
		return name;
	}
}
