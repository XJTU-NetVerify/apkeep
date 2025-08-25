package apkeep.exception;

import java.util.Set;

public class APSetNotFoundException extends Exception {

	private static final long serialVersionUID = 9145930457521805790L;

	public APSetNotFoundException(Set<Integer> aps) {
		super("AP set "+aps+" not completely contained");
	}
}
