package directory.exceptions;

public class RemoteException extends RuntimeException{

	private static final long serialVersionUID = -7384293668578068189L;

	public RemoteException() {
		super();
	}
	
	public RemoteException(String msg) {
		super(msg);
	}

}
