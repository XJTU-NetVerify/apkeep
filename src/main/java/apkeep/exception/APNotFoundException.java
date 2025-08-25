package apkeep.exception;

public class APNotFoundException extends Exception {
	
	private static final long serialVersionUID = -7863357390049272618L;

	/**
	 * @param message
	 */
	public APNotFoundException(int ap) {
		super("AP "+ap +" not exist");
	}
	
	public static void main(String[] args) throws APNotFoundException {
		throw new APNotFoundException(5);
	}
}
