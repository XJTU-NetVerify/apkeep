package apkeep.exception;

public class ElementNotFoundException extends Exception {

	private static final long serialVersionUID = -3388469272962687357L;

	public ElementNotFoundException(String name) {
		super("Element "+name+" not exist");
	}
}
