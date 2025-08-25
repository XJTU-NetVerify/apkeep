package apkeep.exception;

public class BDDNotFalseException extends Exception {

	private static final long serialVersionUID = -2028542801086275275L;

	public BDDNotFalseException(int bdd) {
		super("BDD "+ bdd + " should be completely transferred");
	}
}
