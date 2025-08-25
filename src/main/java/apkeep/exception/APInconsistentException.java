package apkeep.exception;

public class APInconsistentException extends Exception {

	private static final long serialVersionUID = -7412195785278307971L;

	public APInconsistentException(String msg) {
		super("AP inconsistent after "+msg);
	}
}
