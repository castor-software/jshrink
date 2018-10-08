public class Test5 {

	public Test5(){
		int i=0;
		i++;
	}

	public static void main(String[] args) {
		Test5.staticVoidMethodNoParams();
		int i1 = Test5.staticIntMethodNoParams();
		String s1 = Test5.staticStringMethodNoParams();
		Double d1 = Test5.staticDoubleMethodNoParams();
		Test5.staticVoidMethodTwoParams(10, 20);
		int i2 = Test5.staticIntMethodTwoParams(10, 20);

		Test5 test1 = new Test5();
		test1.methodNoParams();
		int i3 = test1.intMethodNoParams();
		int i4 = test1.intMethodTwoParams(10, 20);

		Test5.staticBooleanMethodNoParams();
		Test5.staticCharMethodNoParams();
		Test5.staticByteMethodNoParams();
		Test5.staticShortMethodNoParams();
	}

	public static void staticVoidMethodNoParams() {
		System.out.println("staticVoidMethodNoParams touched");
	}

	public static int staticIntMethodNoParams() {
		System.out.println("staticIntMethodNoParams touched");
		return 10;
	}

	public static String staticStringMethodNoParams() {
		System.out.println("staticStringMethodNoParams touched");
		return "FooBar";
	}

	public static Double staticDoubleMethodNoParams() {
		System.out.println("staticDoubleMethodNoParams touched");
		return new Double(10.10);
	}

	public static void staticVoidMethodTwoParams(int one, int two) {
		System.out.println("staticVoidMethodTwoParams touched");
	}

	public static int staticIntMethodTwoParams(int one, int two) {
		System.out.println("staticIntMethodTwoParams touched");
		return one + two;
	}

	public void methodNoParams() {
		System.out.println("methodNoParams touched");
	}

	public int intMethodNoParams() {
		System.out.println("intMethodNoParams touched");
		return 20;
	}

	public int intMethodTwoParams(int one, int two) {
		System.out.println("intMethodTwoParams touched");
		return one + two;
	}

	public static boolean staticBooleanMethodNoParams(){
		System.out.println("staticBooleanMethodNoParams touched");
		return true;
	}

	public static char staticCharMethodNoParams(){
		System.out.println("staticCharMethodNoParams touched");
		return 'c';
	}

	public static Byte staticByteMethodNoParams(){
		System.out.println("staticByteMethodNoParams touched");
		return 1;
	}

	public static Short staticShortMethodNoParams(){
		System.out.println("staticShortMethodNoParams touched");
		return 3;
	}
}