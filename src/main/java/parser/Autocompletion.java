package parser;

public abstract class Autocompletion {

	public static final int AUTOCOMPLETION_STRING  = 0;
	public static final int AUTOCOMPLETION_LIST    = 1;
	public static final int AUTOCOMPLETION_REAL    = 2;
	public static final int AUTOCOMPLETION_INTEGER = 3;
	public static final int AUTOCOMPLETION_TRIPLE  = 4;

	public final int type;

	public Autocompletion(int type) {
		this.type = type;
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
	}

	public static class IntegerAutocompletion extends Autocompletion {

		public IntegerAutocompletion() {
			super(Autocompletion.AUTOCOMPLETION_INTEGER);
		}
	}

	public static class RealAutocompletion extends Autocompletion {

		public RealAutocompletion() {
			super(Autocompletion.AUTOCOMPLETION_REAL);
		}
	}

	public static class TripleAutocompletion extends Autocompletion {

		public TripleAutocompletion() {
			super(Autocompletion.AUTOCOMPLETION_TRIPLE);
		}
	}



}
