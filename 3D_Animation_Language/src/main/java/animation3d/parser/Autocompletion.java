package animation3d.parser;

public abstract class Autocompletion {

	public static final int AUTOCOMPLETION_STRING    = 0;
	public static final int AUTOCOMPLETION_LIST      = 1;
	public static final int AUTOCOMPLETION_REAL      = 2;
	public static final int AUTOCOMPLETION_INTEGER   = 3;
	public static final int AUTOCOMPLETION_TUPLE     = 4;
	public static final int AUTOCOMPLETION_TRIPLE    = 5;
	public static final int AUTOCOMPLETION_QUADRUPLE = 6;

	public final int type;

	public Autocompletion(int type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return getClass().getName();
	}

	public static class ChoiceAutocompletion extends Autocompletion {

		private String[] options;
		private int insertionPosition;

		public ChoiceAutocompletion(int insertionPosition, String... options) {
			super(AUTOCOMPLETION_LIST);
			this.options = options;
			this.insertionPosition = insertionPosition;
		}

		public int getInsertionPosition() {
			return insertionPosition;
		}

		public String[] getOptions() {
			return options;
		}

		@Override
		public String toString() {
			if(options.length == 0)
				return "empty options"; // this should probably not happen
			if(options.length == 1)
				return "'" + options[0] + "'";
			StringBuilder sb = new StringBuilder();
			sb.append('\'').append(options[0]).append('\'');
			for(int i = 1; i < options.length - 1; i++)
				sb.append(", ").append('\'').append(options[i]).append('\'');
			sb.append(" or ").append('\'').append(options[options.length - 1]).append('\'');
			return sb.toString();
		}
	}


	public static class StringAutocompletion extends Autocompletion {
		private String string;
		private int insertionPosition;

		public StringAutocompletion(int insertionPosition, String string) {
			super(AUTOCOMPLETION_STRING);
			this.string = string;
			this.insertionPosition = insertionPosition;
		}

		public int getInsertionPosition() {
			return insertionPosition;
		}

		public String getString() {
			return string;
		}

		@Override
		public String toString() {
			return "'" + string + "'";
		}
	}

	public static class IntegerAutocompletion extends Autocompletion {

		private final String desc;

		public IntegerAutocompletion(String desc) {
			super(Autocompletion.AUTOCOMPLETION_INTEGER);
			this.desc = desc;
		}

		public String getDescription() {
			return desc;
		}

		@Override
		public String toString() {
			return "an integer number " + desc;
		}
	}

	public static class RealAutocompletion extends Autocompletion {

		private final String desc;

		public RealAutocompletion(String desc) {
			super(Autocompletion.AUTOCOMPLETION_REAL);
			this.desc = desc;
		}

		public String getDescription() {
			return desc;
		}

		@Override
		public String toString() {
			return "a real number " + desc;
		}
	}

	public static class TupleAutocompletion extends Autocompletion {

		private final String desc1;
		private final String desc2;

		public TupleAutocompletion(String desc1, String desc2) {
			super(Autocompletion.AUTOCOMPLETION_TUPLE);
			this.desc1 = desc1;
			this.desc2 = desc2;
		}

		public String getDescription(int i) {
			switch(i) {
			case 0: return desc1;
			case 1: return desc2;
			}
			return null;
		}

		@Override
		public String toString() {
			return "(" + desc1 + ", " + desc2 + ")";
		}
	}

	public static class TripleAutocompletion extends Autocompletion {

		private final String desc1;
		private final String desc2;
		private final String desc3;

		public TripleAutocompletion(String desc1, String desc2, String desc3) {
			super(Autocompletion.AUTOCOMPLETION_TRIPLE);
			this.desc1 = desc1;
			this.desc2 = desc2;
			this.desc3 = desc3;
		}

		public String getDescription(int i) {
			switch(i) {
			case 0: return desc1;
			case 1: return desc2;
			case 2: return desc3;
			}
			return null;
		}

		@Override
		public String toString() {
			return "(" + desc1 + ", " + desc2 + ", " + desc3 + ")";
		}
	}

	public static class QuadrupleAutocompletion extends Autocompletion {

		private final String desc1;
		private final String desc2;
		private final String desc3;
		private final String desc4;

		public QuadrupleAutocompletion(String desc1, String desc2, String desc3, String desc4) {
			super(Autocompletion.AUTOCOMPLETION_QUADRUPLE);
			this.desc1 = desc1;
			this.desc2 = desc2;
			this.desc3 = desc3;
			this.desc4 = desc4;
		}

		public String getDescription(int i) {
			switch(i) {
			case 0: return desc1;
			case 1: return desc2;
			case 2: return desc3;
			case 3: return desc4;
			}
			return null;
		}

		@Override
		public String toString() {
			return "(" + desc1 + ", " + desc2 + ", " + desc3 + ", " + desc4 + ")";
		}
	}
}
