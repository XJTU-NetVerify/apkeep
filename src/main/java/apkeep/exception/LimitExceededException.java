package apkeep.exception;

/**
 * 
 */
public class LimitExceededException extends javax.naming.LimitExceededException {

	private static final long serialVersionUID = 6913643264576674314L;

	public LimitExceededException(int index, int limit) {
		super("Reach index "+index+" when limit is "+limit);
	}

}
