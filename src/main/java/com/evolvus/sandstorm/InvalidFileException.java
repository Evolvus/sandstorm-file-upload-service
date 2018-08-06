package com.evolvus.sandstorm;

/**
 * 
 * @author EVOLVUS\shrimank
 *
 */
public class InvalidFileException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2776450355349830013L;

	public InvalidFileException() {
		super();
	}

	public InvalidFileException(String message) {
		super(message);
	}

	public InvalidFileException(Throwable cause) {
		super(cause);

	}

	public InvalidFileException(String message, Throwable cause) {
		super(message, cause);

	}

	public InvalidFileException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);

	}

}
