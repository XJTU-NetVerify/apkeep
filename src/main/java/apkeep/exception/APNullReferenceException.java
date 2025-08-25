package apkeep.exception;

public class APNullReferenceException extends Exception {

	private static final long serialVersionUID = -6703323477306941428L;

	public APNullReferenceException(int ap) {
		super("AP "+ap+" not hold by any port");
	}
}
