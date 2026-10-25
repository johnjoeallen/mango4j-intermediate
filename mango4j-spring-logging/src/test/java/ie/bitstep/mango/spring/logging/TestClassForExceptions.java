package ie.bitstep.mango.spring.logging;

public class TestClassForExceptions {

	public void doException() {
		do1();
	}

	public void doExceptionWithCause() {
		doWithCause1();
	}

	public void doExceptionWithSuppressed() {
		doWithSuppressed1();
	}


	private void do1() {
		do2();
	}

	private void do2() {
		throw new RuntimeException("Oops");
	}

	private void doWithCause1() {
		doWithCause2();
	}

	private void doWithCause2() {
		try {
			do1();
		} catch (Exception ex) {
			throw new RuntimeException("Failed", ex);
		}
	}

	private void doWithSuppressed1() {
		doWithSuppressed2();
	}

	private void doWithSuppressed2() {
		try {
			do1();
		} catch (Exception ex) {
			RuntimeException e = new RuntimeException("Failed");
			e.addSuppressed(ex);
			throw e;
		}
	}

}
