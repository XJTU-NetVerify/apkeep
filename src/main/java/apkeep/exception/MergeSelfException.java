package apkeep.exception;

public class MergeSelfException extends Exception {

	private static final long serialVersionUID = -7990569328962992339L;

	public MergeSelfException(Object ap) {
		super("Merge AP "+ap+" into itself");
	}
}
