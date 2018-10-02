package edu.ucla.cs.onr.methodwiper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import soot.*;
import soot.jimple.JasminClass;
import soot.jimple.JimpleBody;
import soot.options.Options;
import soot.util.JasminOutputStream;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static soot.Scene.v;

public class MethodWiperTest {

	private static String getJREJars() {
		String defaultClasspath = v().defaultClassPath();
		String cp;
		if (defaultClasspath.contains(":")) {
			// The JRE in Mac returns multiple JRE jars including rt.jar and jce.jar
			cp = defaultClasspath;
		} else {
			// The JRE in Linux returns rt.jar only
			String jreDirectory = (new File(defaultClasspath)).getParent();
			String jcePath = jreDirectory + File.separator + "jce.jar";
			// add rt.jar and jce.jar to the classpath, as required by Soot
			cp = defaultClasspath + File.pathSeparator + jcePath;
		}

		return cp;
	}

	private static SootClass getSootClassFromResources(String className){
		ClassLoader classLoader = MethodWiperTest.class.getClassLoader();
		File classFile = classFile = new File(classLoader.getResource(className + ".class").getFile());

		final String workingClasspath=classFile.getParentFile().getAbsolutePath();
		Options.v().set_soot_classpath(MethodWiperTest.getJREJars() + File.pathSeparator + workingClasspath);
		Options.v().set_whole_program(true);
		Options.v().set_allow_phantom_refs(true);

		List<String> processDirs = new ArrayList<String>();
		processDirs.add(workingClasspath);
		Options.v().set_process_dir(processDirs);

		SootClass sClass = Scene.v().loadClassAndSupport(className);
		Scene.v().loadNecessaryClasses();


		return sClass;
	}

	private static File createClass(SootClass sootClass){

		// I receive 'Exception thrown: method <init> has no active body' if methods are not retrieved
		for(SootMethod sootMethod : sootClass.getMethods()){
			sootMethod.retrieveActiveBody();
		}

		File fileToReturn = null;
		try {
			String fileName = SourceLocator.v().getFileNameFor(sootClass, Options.output_format_class);
			fileToReturn = new File(fileName);
			OutputStream streamOut = new JasminOutputStream(new FileOutputStream(fileToReturn));
			PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));

			JasminClass jasminClass = new soot.jimple.JasminClass(sootClass);
			jasminClass.print(writerOut);
			writerOut.flush();
			streamOut.close();

		}catch(Exception e){
			System.err.println("Exception thrown: " + e.getMessage());
			System.exit(1);
		}

		assert(fileToReturn != null);

		return fileToReturn;
	}

	private static String runClass(SootClass sootClass){
		File classFile = MethodWiperTest.createClass(sootClass);

		String cmd = "java -cp "+classFile.getParentFile().getAbsolutePath() + " "
			+ classFile.getName().replaceAll(".class","");

		Process p =null;
		StringBuilder output = new StringBuilder();
		try {
			p = Runtime.getRuntime().exec(cmd);
			p.waitFor();

			BufferedReader brInputStream = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader brErrorStream = new BufferedReader(new InputStreamReader(p.getErrorStream()));

			String line;
			while((line = brInputStream.readLine())!=null){
				output.append(line + System.lineSeparator());
			}
			brInputStream.close();

			while((line = brErrorStream.readLine()) != null){
				output.append(line + System.lineSeparator());
			}
			brErrorStream.close();

			//} catch(IOException e InterruptedException ie){
		}catch(Exception e){
			System.err.println("Exception thrown when trying to run the following script:");
			StringBuilder sb = new StringBuilder();
			System.err.println(cmd);
			System.err.println("The following error was thrown: ");
			System.err.println(e.getMessage());
			System.exit(1);
		}

		return output.toString();
	}

	@Test
	void sanityCheck() {
		SootClass sootClass = getSootClassFromResources("Test1");
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	void wipeMethodTest_Test1_staticVoidMethodNoParams(){
		SootClass sootClass = getSootClassFromResources("Test2");
		MethodWiper.wipeMethod(sootClass.getMethodByName("staticVoidMethodNoParams"));
		String output = runClass(sootClass);

		String expected = "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	void wipeMethodTest_staticIntMethodNoParams() {
		SootClass sootClass = getSootClassFromResources("Test3");
		MethodWiper.wipeMethod(sootClass.getMethodByName("staticIntMethodNoParams"));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	void wipeMethodTest_staticStringMethodNoParam() {
		SootClass sootClass = getSootClassFromResources("Test4");
		MethodWiper.wipeMethod(sootClass.getMethodByName("staticStringMethodNoParams"));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	void wipeMethodTest_staticDoubleMethodNoParams() {
		SootClass sootClass = getSootClassFromResources("Test5");
		MethodWiper.wipeMethod(sootClass.getMethodByName("staticDoubleMethodNoParams"));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	void wipeMethodTest_staticVoidMethodTwoParams() {
		SootClass sootClass = getSootClassFromResources("Test6");
		MethodWiper.wipeMethod(sootClass.getMethodByName("staticVoidMethodTwoParams"));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	void wipeMethodTest_staticIntMethodTwoParams() {
		SootClass sootClass = getSootClassFromResources("Test7");
		MethodWiper.wipeMethod(sootClass.getMethodByName("staticIntMethodTwoParams"));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	void wipeMethodTest_methodNoParams() {
		SootClass sootClass = getSootClassFromResources("Test8");
		MethodWiper.wipeMethod(sootClass.getMethodByName("methodNoParams"));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	void wipeMethodTest_intMethodNoParams() {
		SootClass sootClass = getSootClassFromResources("Test9");
		MethodWiper.wipeMethod(sootClass.getMethodByName("intMethodNoParams"));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	void wipeMethodTest_intMethodTwoParams() {
		SootClass sootClass = getSootClassFromResources("Test10");
		MethodWiper.wipeMethod(sootClass.getMethodByName("intMethodTwoParams"));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	void wipeMethodTest_staticBooleanMethodNoParams(){
		SootClass sootClass = getSootClassFromResources("Test11");
		MethodWiper.wipeMethod(sootClass.getMethodByName("staticBooleanMethodNoParams"));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	void wipeMethodTest_staticCharMethodNoParams(){
		SootClass sootClass = getSootClassFromResources("Test12");
		MethodWiper.wipeMethod(sootClass.getMethodByName("staticCharMethodNoParams"));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	void wipeMethodTest_staticByteMethodNoParams(){
		SootClass sootClass = getSootClassFromResources("Test13");
		MethodWiper.wipeMethod(sootClass.getMethodByName("staticByteMethodNoParams"));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	void wipeMethodTest_staticShortMethodNoParams(){
		SootClass sootClass = getSootClassFromResources("Test14");
		MethodWiper.wipeMethod(sootClass.getMethodByName("staticShortMethodNoParams"));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	void wipeMethodAndInsertRuntimeException_TestWithoutMessage(){
		SootClass sootClass = getSootClassFromResources("Test15");
		MethodWiper.wipeMethodAndInsertRuntimeException(sootClass.getMethodByName("intMethodTwoParams"));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "Exception in thread \"main\" java.lang.RuntimeException" + System.lineSeparator() +
			"\tat Test15.intMethodTwoParams(Test15.java)" + System.lineSeparator() +
			"\tat Test15.main(Test15.java)" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	void wipeMethodAndInsertRuntimeException_TestWithMessage(){
		SootClass sootClass = getSootClassFromResources("Test15");
		MethodWiper.wipeMethodAndInsertRuntimeException(sootClass.getMethodByName("intMethodTwoParams"), "TEST");
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "Exception in thread \"main\" java.lang.RuntimeException: TEST" + System.lineSeparator() +
			"\tat Test15.intMethodTwoParams(Test15.java)" + System.lineSeparator() +
			"\tat Test15.main(Test15.java)" + System.lineSeparator();

		assertEquals(expected, output);
	}


}
