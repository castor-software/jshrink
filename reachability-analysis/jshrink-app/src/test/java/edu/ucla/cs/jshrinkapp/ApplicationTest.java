package edu.ucla.cs.jshrinkapp;

import edu.ucla.cs.jshrinklib.classcollapser.ClassCollapserData;
import edu.ucla.cs.jshrinklib.methodinliner.InlineData;
import edu.ucla.cs.jshrinklib.reachability.*;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import soot.G;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ApplicationTest {

	private static Optional<File> simpleTestProject = Optional.empty();
	private static Optional<File> moduleTestProject = Optional.empty();
	private static Optional<File> reflectionTestProject = Optional.empty();
	private static Optional<File> junitProject = Optional.empty();
	private static Optional<File> nettySocketIOProject = Optional.empty();
	private static Optional<File> classCollapserProject = Optional.empty();
	private static Optional<File> lambdaProject = Optional.empty();
	private static Optional<File> dynamicDispatchingProject = Optional.empty();
	private static Optional<File> logDirectory = Optional.empty();

	private static File getOptionalFile(Optional<File> optionalFile, String resources){
		if(optionalFile.isPresent()){
			return optionalFile.get();
		}
		ClassLoader classLoader = ApplicationTest.class.getClassLoader();
		File f = new File(classLoader.getResource(resources).getFile());

		try {
			File copy = File.createTempFile("test-project-", "");
			copy.delete();
			copy.mkdir();

			FileUtils.copyDirectory(f, copy);

			optionalFile = Optional.of(copy);
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}

		return optionalFile.get();
	}

	private static File getSimpleTestProjectDir(){
		return getOptionalFile(simpleTestProject, "simple-test-project");
	}

	private boolean jarIntact(){
		if(simpleTestProject.isPresent()){
			File f = new File(simpleTestProject.get().getAbsolutePath()
				+ File.pathSeparator + "libs" + File.pathSeparator + "standard-stuff-library.jar");
			return f.exists() && !f.isDirectory();
		}

		return true;
    }

	private File getModuleProjectDir() {
		return getOptionalFile(moduleTestProject, "module-test-project");
	}

	private File getReflectionProjectDir(){
		return getOptionalFile(reflectionTestProject, "reflection-test-project");
	}

	private File getJunitProjectDir(){
		return getOptionalFile(junitProject, "junit4");
	}

	private File getNettySocketIOProjectDir() {
		return getOptionalFile(nettySocketIOProject, "netty-socketio");
	}

	private File getGeccoProjectDir() {
		return getOptionalFile(nettySocketIOProject, "gecco");
	}

	private File getLogDirectory(){
		if(logDirectory.isPresent()){
			return this.getLogDirectory();
		}

		try {
			File lDirectory = File.createTempFile("log_directory_", "");
			lDirectory.delete();

			logDirectory = Optional.of(lDirectory);
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}

		return logDirectory.get();
	}

	private File getClassCollapserDir(){
		return getOptionalFile(classCollapserProject, "classcollapser"
			+ File.separator + "simple-collapse-example");
	}

	private File getLambdaAppProject(){
		return getOptionalFile(lambdaProject, "lambda-test-project");
	}

	private File getDynamicDispatchingProject(){
		return getOptionalFile(dynamicDispatchingProject, "dynamic-dispatching-test-project");
	}

	private File getTamiFlexJar(){
		File toReturn = new File(
				ApplicationTest.class.getClassLoader().getResource(
					"tamiflex" + File.separator + "poa-2.0.3.jar").getFile());
		return toReturn;
	}

	@After
	public void rectifyChanges() {
		simpleTestProject = Optional.empty();
		moduleTestProject = Optional.empty();
		reflectionTestProject = Optional.empty();
		junitProject = Optional.empty();
		classCollapserProject = Optional.empty();
		lambdaProject = Optional.empty();
		logDirectory = Optional.empty();
		G.reset();
	}


	private boolean isPresent(Set<MethodData> methodsRemoved, String className, String methodName){
		for(MethodData methodData : methodsRemoved){
			if(methodData.getClassName().equals(className) && methodData.getName().equals(methodName)){
				return true;
			}
		}

		return false;
	}

	/*
	@Test
	public void test(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project /home/bobbyrbruce/Desktop/nifty ");
		arguments.append("--main-entry ");
		arguments.append("--public-entry ");
		arguments.append("--test-entry ");
		arguments.append("--verbose ");
		arguments.append("--use-spark ");


		Application.main(arguments.toString().split("\\s+"));
	}
	*/

	@Test
	public void mainTest_targetMainEntryPoint(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project " + getSimpleTestProjectDir().getAbsolutePath() + " ");
		arguments.append("--main-entry ");
		arguments.append("--remove-classes ");
		arguments.append("--remove-methods ");
		arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");


		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;


		assertTrue(Application.removedMethod);
		assertFalse(Application.wipedMethodBody);
		assertFalse(Application.wipedMethodBodyWithExceptionNoMessage);
		assertFalse(Application.wipedMethodBodyWithExceptionAndMessage);

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff", "doNothing"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuff", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "subMethodUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethod"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethodCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassNeverTouched"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertFalse(isPresent(methodsRemoved, "Main", "compare"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertTrue(classesRemoved.contains("StandardStuffSub"));
		assertEquals(2, classesRemoved.size());

		assertTrue(jarIntact());
	}

	@Test
	public void mainTest_targetMainEntryPoint_withSpark(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project " + getSimpleTestProjectDir().getAbsolutePath() + " ");
		arguments.append("--main-entry ");
		arguments.append("--use-spark ");
		arguments.append("--remove-methods ");
		arguments.append("--remove-classes ");
		arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");


		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

		assertTrue(Application.removedMethod);
		assertFalse(Application.wipedMethodBody);
		assertFalse(Application.wipedMethodBodyWithExceptionNoMessage);
		assertFalse(Application.wipedMethodBodyWithExceptionAndMessage);

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff", "doNothing"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuff", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "subMethodUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethod"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethodCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassNeverTouched"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertFalse(isPresent(methodsRemoved, "Main", "compare"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertTrue(classesRemoved.contains("StandardStuffSub"));
		assertEquals(2, classesRemoved.size());

		assertTrue(jarIntact());
	}

	@Test
	public void mainTest_targetTestEntryPoints(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project " + getSimpleTestProjectDir().getAbsolutePath() + " ");
		arguments.append("--test-entry ");
		arguments.append("--remove-methods ");
		arguments.append("--remove-classes ");
		arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

		assertTrue(Application.removedMethod);
		assertFalse(Application.wipedMethodBody);
		assertFalse(Application.wipedMethodBodyWithExceptionNoMessage);
		assertFalse(Application.wipedMethodBodyWithExceptionAndMessage);

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff", "doNothing"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuff", "protectedAndUntouched"));
		assertFalse(isPresent(methodsRemoved, "StandardStuffSub", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "subMethodUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethod"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethodCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassNeverTouched"));
		assertTrue(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		//(Method is untouched by too small to remove)
//		assertTrue(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertTrue(isPresent(methodsRemoved,"Main","main"));
		assertFalse(isPresent(methodsRemoved, "Main$1", "compare"));


		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass"));
		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertEquals(2, classesRemoved.size());

        assertTrue(jarIntact());
	}

	@Test
	public void mainTest_targetPublicEntryPoints(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project " + getSimpleTestProjectDir().getAbsolutePath() + " ");
		arguments.append("--public-entry ");
		arguments.append("--include-exception ");
		arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classRemoved = Application.removedClasses;

		assertFalse(Application.removedMethod);
		assertFalse(Application.wipedMethodBody);
		assertTrue(Application.wipedMethodBodyWithExceptionNoMessage);
		assertFalse(Application.wipedMethodBodyWithExceptionAndMessage);

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff", "doNothing"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuff", "protectedAndUntouched"));
		assertFalse(isPresent(methodsRemoved, "StandardStuffSub", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "subMethodUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethod"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethodCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassNeverTouched"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertFalse(isPresent(methodsRemoved, "Main", "compare"));
		assertTrue(isPresent(methodsRemoved, "edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertEquals(0, classRemoved.size());

        assertTrue(jarIntact());
	}

	@Test
	public void mainTest_targetAllEntryPoints(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project " + getSimpleTestProjectDir().getAbsolutePath() + " ");
		arguments.append("--public-entry ");
		arguments.append("--main-entry ");
		arguments.append("--test-entry ");
		arguments.append("--include-exception \"message_removed\" ");
		arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classRemoved = Application.removedClasses;

		assertFalse(Application.removedMethod);
		assertFalse(Application.wipedMethodBody);
		assertFalse(Application.wipedMethodBodyWithExceptionNoMessage);
		assertTrue(Application.wipedMethodBodyWithExceptionAndMessage);

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff", "doNothing"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuff", "protectedAndUntouched"));
		assertFalse(isPresent(methodsRemoved, "StandardStuffSub", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "subMethodUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethod"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethodCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassNeverTouched"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertFalse(isPresent(methodsRemoved, "Main", "compare"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertEquals(0, classRemoved.size());

        assertTrue(jarIntact());
	}

	@Test
	public void mainTest_targetAllEntryPoints_withTamiFlex(){
		/*
		Note: There is actually no reflection in this target, i just want to ensure reflection isn't making anything
		crash.
		 */
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project " + getSimpleTestProjectDir().getAbsolutePath() + " ");
		arguments.append("--public-entry ");
		arguments.append("--main-entry ");
		arguments.append("--test-entry ");
		arguments.append("--tamiflex " + getTamiFlexJar().getAbsolutePath() + " ");
		arguments.append("--include-exception \"message_removed\" ");
		arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classRemoved = Application.removedClasses;

		assertFalse(Application.removedMethod);
		assertFalse(Application.wipedMethodBody);
		assertFalse(Application.wipedMethodBodyWithExceptionNoMessage);
		assertTrue(Application.wipedMethodBodyWithExceptionAndMessage);

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff", "doNothing"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuff", "protectedAndUntouched"));
		assertFalse(isPresent(methodsRemoved, "StandardStuffSub", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "subMethodUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethod"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethodCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassNeverTouched"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertFalse(isPresent(methodsRemoved, "Main", "compare"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertEquals(0, classRemoved.size());

		assertTrue(jarIntact());
	}


	@Ignore @Test //Ignoring this test right now as it's failing (we think it's a bug in Spark callgraph analysis)
	public void mainTest_targetCustomEntryPoint(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project " + getSimpleTestProjectDir().getAbsolutePath() + " ");
		arguments.append("--custom-entry <StandardStuff: public void publicAndTestedButUntouched()> ");
		arguments.append("--remove-classes ");
		arguments.append("--remove-methods ");
		arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

		assertTrue(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff", "doNothing"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuff", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "subMethodUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethod"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethodCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassNeverTouched"));
		assertTrue(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertTrue(isPresent(methodsRemoved,"Main","main"));
		assertTrue(isPresent(methodsRemoved, "Main", "compare"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass"));
		assertEquals(2, classesRemoved.size());

        assertTrue(jarIntact());
	}

	@Test
	public void mavenTest_mainMethodEntry_withOutTamiFlex(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getModuleProjectDir().getAbsolutePath() + "\" ");
		arguments.append("--main-entry ");
		arguments.append("--remove-classes ");
		arguments.append("--remove-methods ");
		arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertTrue(isPresent(methodsRemoved, "StandardStuff", "touchedViaReflection"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));

		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass2"));
		assertFalse(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass"));
		assertFalse(classesRemoved.contains("StandardStuff"));

		assertTrue(jarIntact());
	}

	@Test 
//	@Ignore
	public void mavenTest_mainMethodEntry_withTamiFlex(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getModuleProjectDir().getAbsolutePath() + "\" ");
		arguments.append("--main-entry ");
		arguments.append("--test-entry "); //Note: when targeting Maven, we always implicitly target test entry due to TamiFlex
		arguments.append("--tamiflex " + getTamiFlexJar().getAbsolutePath() + " ");
		arguments.append("--remove-classes ");
		arguments.append("--remove-methods ");
		arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertFalse(isPresent(methodsRemoved, "StandardStuff", "touchedViaReflection"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));

		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));

		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass2"));
		assertFalse(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass"));
		assertFalse(classesRemoved.contains("StandardStuff"));

		assertTrue(jarIntact());
	}

	@Test @Ignore //We don't support "--ignore-classes" for now
	public void ignoreClassTest(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project " + getSimpleTestProjectDir().getAbsolutePath() + " ");
		arguments.append("--main-entry ");
		arguments.append("--ignore-classes edu.ucla.cs.onr.test.LibraryClass edu.ucla.cs.onr.test.UnusedClass ");
		arguments.append("--remove-classes ");
		arguments.append("--remove-methods ");
		arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");

		try {
			Method method = ApplicationTest.class.getMethod("ignoreClassTest");
			Object o = method.invoke(null);
		}catch(Exception e){

		}

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "subMethodUntouched"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertFalse(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertFalse(isPresent(methodsRemoved, "Main", "compare"));
		assertFalse(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertTrue(classesRemoved.contains("StandardStuffSub"));
		assertEquals(1, classesRemoved.size());

		assertTrue(jarIntact());
	}

	@Test
	public void reflectionTest_mainMethodEntry_withTamiFlex(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getReflectionProjectDir().getAbsolutePath() + "\" ");
		arguments.append("--main-entry ");
		arguments.append("--test-entry "); //Note: when targeting Maven, we always implicitly target test entry due to TamiFlex
		arguments.append("--tamiflex " + getTamiFlexJar().getAbsolutePath() + " ");
		arguments.append("--remove-classes ");
		arguments.append("--remove-methods ");
		arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertFalse(isPresent(methodsRemoved, "ReflectionStuff", "touchedViaReflection"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass2"));
		assertFalse(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass"));
		assertFalse(classesRemoved.contains("ReflectionStuff"));
		assertFalse(classesRemoved.contains("StandardStuff"));

		assertTrue(jarIntact());
	}

	@Test
	public void reflectionTest_mainMethodEntry_withoutTamiFlex(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getReflectionProjectDir().getAbsolutePath() + "\" ");
		arguments.append("--main-entry ");
		arguments.append("--test-entry "); //Note: when targeting Maven, we always implicitly target test entry due to TamiFlex
		arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertTrue(isPresent(methodsRemoved, "ReflectionStuff", "touchedViaReflection"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertEquals(0, classesRemoved.size());

		assertTrue(jarIntact());
	}

	@Test
	public void junit_test(){
		//This tests ensures that all test cases pass before and after the tool is run
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getJunitProjectDir().getAbsolutePath() + "\" ");
		arguments.append("--main-entry ");
		arguments.append("--test-entry ");
		arguments.append("--remove-methods ");
		arguments.append("--test-output ");
		arguments.append("--tamiflex " + getTamiFlexJar().getAbsolutePath() + " ");
		arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");

		Application.main(arguments.toString().split("\\s+"));

		assertEquals(Application.testOutputBefore.getRun(), Application.testOutputAfter.getRun());
		assertEquals(Application.testOutputBefore.getErrors(), Application.testOutputAfter.getErrors());
		assertEquals(Application.testOutputBefore.getFailures(), Application.testOutputAfter.getFailures());
		assertEquals(Application.testOutputBefore.getSkipped(), Application.testOutputAfter.getSkipped());
	}

	@Test
	public void junit_test_log_in_home_directory(){
		//This tests ensures that all test cases pass before and after the tool is run
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getJunitProjectDir().getAbsolutePath() + "\" ");
		arguments.append("--main-entry ");
		arguments.append("--test-entry ");
		arguments.append("--remove-methods ");
		arguments.append("--test-output ");

		Application.main(arguments.toString().split("\\s+"));

		File expected = new File(System.getProperty("user.home") + File.separator + "jshrink_output");
		assertTrue(expected.exists());
		assertTrue(new File(expected.getAbsolutePath() + File.separator + "log.dat").exists());
		assertTrue(new File(expected.getAbsolutePath() + File.separator + "test_output_before.dat").exists());
		assertTrue(new File(expected.getAbsolutePath() + File.separator + "test_output_after.dat").exists());

		expected.delete();
	}

	@Test
	public void junit_test_methodinliner(){
		//This tests ensures that all test cases pass before and after the tool is run
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getJunitProjectDir().getAbsolutePath() + "\" ");
		arguments.append("--main-entry ");
		arguments.append("--test-entry ");
		arguments.append("--skip-method-removal ");
		arguments.append("--inline ");
		arguments.append("--test-output ");
		arguments.append("--tamiflex " + getTamiFlexJar().getAbsolutePath() + " ");
		arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");

		Application.main(arguments.toString().split("\\s+"));

		InlineData inlineData = Application.inlineData;

		assertEquals(Application.testOutputBefore.getRun(), Application.testOutputAfter.getRun());
		assertEquals(Application.testOutputBefore.getErrors(), Application.testOutputAfter.getErrors());
		assertEquals(Application.testOutputBefore.getFailures(), Application.testOutputAfter.getFailures());
		assertEquals(Application.testOutputBefore.getSkipped(), Application.testOutputAfter.getSkipped());
	}


	@Test
	public void test_junit_test_failures() {
		String junit_project_path = getJunitProjectDir().getAbsolutePath();
		Set<MethodData> entryPoints = new HashSet<MethodData>();
		MethodData failedTest = new MethodData("verifierRunsAfterTest", "org.junit.rules.VerifierRuleTest", "void", new String[] {}, true, false);
		entryPoints.add(failedTest);
		EntryPointProcessor entryPointProcessor = new EntryPointProcessor(false, false, false, false, entryPoints);
		MavenSingleProjectAnalyzer runner = new MavenSingleProjectAnalyzer(junit_project_path, entryPointProcessor, Optional.of(getTamiFlexJar()), false, false);
		runner.setup();
		runner.run();
		assertTrue(isPresent(runner.getUsedAppMethods().keySet(), "org.junit.rules.Verifier", "verify"));
	}

	@Test
	public void reproduce_junit_test_failure(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getDynamicDispatchingProject().getAbsolutePath() + "\" ");
		arguments.append("--test-entry ");
		arguments.append("--remove-methods ");
		arguments.append("--test-output ");
		arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> md = Application.removedMethods;
		assertEquals(1, md.size());
		assertTrue(isPresent(md, "A", "unused"));
		assertEquals(Application.testOutputBefore.getRun(), Application.testOutputAfter.getRun());
		assertEquals(Application.testOutputBefore.getErrors(), Application.testOutputAfter.getErrors());
		assertEquals(Application.testOutputBefore.getFailures(), Application.testOutputAfter.getFailures());
		assertEquals(Application.testOutputBefore.getSkipped(), Application.testOutputAfter.getSkipped());
	}

	@Test @Ignore
	public void lambdaMethodTest(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getLambdaAppProject() + "\" ");
		arguments.append("--main-entry ");
		arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

		assertFalse(Application.removedMethod);
		assertTrue(Application.wipedMethodBody);
		assertFalse(Application.wipedMethodBodyWithExceptionNoMessage);
		assertFalse(Application.wipedMethodBodyWithExceptionAndMessage);

		assertFalse(isPresent(methodsRemoved, "StandardStuff", "isEven"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertFalse(isPresent(methodsRemoved,"Main","isNegativeNumber"));
	}

	@Test @Ignore
	public void lambdaMethodTest_full(){
		/*
		This test fails do to Soot which cannot properly process convert SootClass to .java files which contain Lambda
		expressions. I've set it to ignore for the time being as it's not something we can presently fix.
		 */
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getLambdaAppProject() + "\" ");
		arguments.append("--main-entry ");
		arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

		assertFalse(Application.removedMethod);
		assertTrue(Application.wipedMethodBody);
		assertFalse(Application.wipedMethodBodyWithExceptionNoMessage);
		assertFalse(Application.wipedMethodBodyWithExceptionAndMessage);

		assertFalse(isPresent(methodsRemoved, "StandardStuff", "isEven"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertFalse(isPresent(methodsRemoved,"Main","isNegativeNumber"));
		assertTrue(isPresent(methodsRemoved,"Main","methodNotUsed"));
		assertEquals(0, methodsRemoved.size());
	}

	@Test
	public void inlineMethodTest() throws IOException{
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project " + getSimpleTestProjectDir().getAbsolutePath() + " ");
		arguments.append("--main-entry ");
		arguments.append("--inline ");
		arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");

		Application.main(arguments.toString().split("\\s+"));

		InlineData methodsInlined = Application.inlineData;

		Assert.assertTrue(methodsInlined.getInlineLocations().containsKey(TestUtils.getMethodDataFromSignature(
			"<StandardStuff$NestedClass: void nestedClassMethodCallee()>")));
		assertEquals(1,methodsInlined.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: void nestedClassMethodCallee()>")).size());
		Assert.assertTrue(methodsInlined.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: void nestedClassMethodCallee()>"))
			.contains(TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: public void nestedClassMethod()>")));

		Assert.assertTrue(methodsInlined.getInlineLocations().containsKey(
			TestUtils.getMethodDataFromSignature("<StandardStuff: public java.lang.String getString()>")));
		assertEquals(1, methodsInlined.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff: public java.lang.String getString()>")).size());
		Assert.assertTrue(methodsInlined.getInlineLocations().get(TestUtils.getMethodDataFromSignature("<StandardStuff: public java.lang.String getString()>"))
			.contains(TestUtils.getMethodDataFromSignature("<Main: public static void main(java.lang.String[])>")));

		Assert.assertTrue(methodsInlined.getInlineLocations().containsKey(
			TestUtils.getMethodDataFromSignature("<edu.ucla.cs.onr.test.LibraryClass: public int getNumber()>")));
		assertEquals(1, methodsInlined.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<edu.ucla.cs.onr.test.LibraryClass: public int getNumber()>")).size());
		Assert.assertTrue(methodsInlined.getInlineLocations().get(TestUtils.getMethodDataFromSignature("<edu.ucla.cs.onr.test.LibraryClass: public int getNumber()>"))
			.contains(TestUtils.getMethodDataFromSignature("<Main: public static void main(java.lang.String[])>")));

		assertTrue(jarIntact());
	}

	@Test
	public void classCollapserTest(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getClassCollapserDir().getAbsolutePath() + "\" ");
		arguments.append("--main-entry ");
		arguments.append("--remove-methods ");
		// no need to enable tamiflex since there are no reflection calls in this simple case
//		arguments.append("--tamiflex " + getTamiFlexJar().getAbsolutePath() + " ");
		arguments.append("--class-collapser ");
		arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;
		ClassCollapserData classCollapserData = Application.classCollapserData;

		assertEquals(1, classCollapserData.getClassesToRemove().size());
		assertTrue(classCollapserData.getClassesToRemove().contains("B"));

		assertFalse(isPresent(classCollapserData.getRemovedMethods(), "Main", "main"));
		// assertTrue(isPresent(classCollapserData.getRemovedMethods(), "C", "<init>"));
		// assertTrue(isPresent(classCollapserData.getRemovedMethods(), "C", "saySomething"));
		// assertTrue(isPresent(classCollapserData.getRemovedMethods(), "C", "uniqueToC"));
		assertTrue(isPresent(classCollapserData.getRemovedMethods(), "B","<init>"));
		assertTrue(isPresent(classCollapserData.getRemovedMethods(), "B", "uniqueToB"));
		assertTrue(isPresent(classCollapserData.getRemovedMethods(), "B", "saySomething"));
		assertFalse(isPresent(classCollapserData.getRemovedMethods(), "A", "uniqueToB"));
		assertFalse(isPresent(classCollapserData.getRemovedMethods(), "A", "uniqueToA"));
		assertFalse(isPresent(classCollapserData.getRemovedMethods(), "A", "<init>"));
		// A.saySomething is replaced by B.saySomething.
		assertFalse(isPresent(classCollapserData.getRemovedMethods(), "A", "saySomething"));
		assertFalse(isPresent(classCollapserData.getRemovedMethods(), "A", "getClassType"));
	}

	@Test
	public void mainTest_targetMainEntryPoint_classCollapser(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project " + getSimpleTestProjectDir().getAbsolutePath() + " ");
		arguments.append("--main-entry ");
		arguments.append("--class-collapser ");
		arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");


		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

		assertFalse(Application.removedMethod);
		assertTrue(Application.wipedMethodBody);
		assertFalse(Application.wipedMethodBodyWithExceptionNoMessage);
		assertFalse(Application.wipedMethodBodyWithExceptionAndMessage);

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff", "doNothing"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuff", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "subMethodUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethod"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethodCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassNeverTouched"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertFalse(isPresent(methodsRemoved, "Main", "compare"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertTrue(classesRemoved.contains("StandardStuffSub"));
		assertEquals(2, classesRemoved.size());

		assertTrue(jarIntact());
	}

    private boolean isFieldPresent(Set<FieldData> fieldSet, String className, String fieldName){
        for(FieldData fieldData : fieldSet){
            if(fieldData.getClassName().equals(className) && fieldData.getName().equals(fieldName)){
                return true;
            }
        }
        return false;
    }

	/**
	 * This test case aims to test the field removal function only without interacting with other transformations.
	 */
	@Test
	public void fieldRemovalTest() {
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project " + getSimpleTestProjectDir().getAbsolutePath() + " ");
		arguments.append("--main-entry ");
        arguments.append("--test-entry ");
		arguments.append("--remove-fields ");
        arguments.append("--skip-method-removal ");
        arguments.append("--test-output ");
		arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");

		Application.main(arguments.toString().split("\\s+"));

        Set<FieldData> fieldsRemoved = Application.removedFields;

        assertEquals(4, fieldsRemoved.size());
        // though the following fields are referened in the source code, they are inlined by Java compiler in the bytecode
        // so they are not used in bytecode
        assertTrue(isFieldPresent(fieldsRemoved, "StandardStuff", "HELLO_WORLD_STRING"));
        assertTrue(isFieldPresent(fieldsRemoved, "StandardStuff", "GOODBYE_STRING"));
        assertTrue(isFieldPresent(fieldsRemoved, "edu.ucla.cs.onr.test.LibraryClass", "x"));
        assertTrue(isFieldPresent(fieldsRemoved, "edu.ucla.cs.onr.test.LibraryClass2", "y"));
		assertEquals(Application.testOutputBefore.getRun(), Application.testOutputAfter.getRun());
		assertEquals(Application.testOutputBefore.getErrors(), Application.testOutputAfter.getErrors());
		assertEquals(Application.testOutputBefore.getFailures(), Application.testOutputAfter.getFailures());
		assertEquals(Application.testOutputBefore.getSkipped(), Application.testOutputAfter.getSkipped());

        assertTrue(jarIntact());
	}

	@Test
	public void fieldRemovalTestWithTamiFlex() {
		ClassLoader classLoader = ApplicationTest.class.getClassLoader();
		String tamiflex_test_project_path =
				new File(classLoader.getResource("tamiflex-test-project").getFile()).getAbsolutePath();

		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project " + tamiflex_test_project_path + " ");
		arguments.append("--test-entry ");
		arguments.append("--tamiflex " + getTamiFlexJar().getAbsolutePath() + " ");
		arguments.append("--remove-fields ");
		arguments.append("--skip-method-removal ");
		arguments.append("--test-output ");
		arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");

		Application.main(arguments.toString().split("\\s+"));

		Set<FieldData> fieldsRemoved = Application.removedFields;

		assertEquals(1, fieldsRemoved.size());
		// though the following fields are referened in the source code, they are inlined by Java compiler in the bytecode
		// so they are not used in bytecode
		assertTrue(isFieldPresent(fieldsRemoved, "A", "f3"));
		assertEquals(Application.testOutputBefore.getRun(), Application.testOutputAfter.getRun());
		assertEquals(Application.testOutputBefore.getErrors(), Application.testOutputAfter.getErrors());
		assertEquals(Application.testOutputBefore.getFailures(), Application.testOutputAfter.getFailures());
		assertEquals(Application.testOutputBefore.getSkipped(), Application.testOutputAfter.getSkipped());

		assertTrue(jarIntact());
	}

	/**
	 * Check how many test failures there will be when running our tool on JUnit without enabling TamiFlex
	 */
	@Test
	public void runFieldRemovalOnJUnitWithoutTamiFlex(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getJunitProjectDir() + "\" ");
		arguments.append("--main-entry ");
		arguments.append("--test-entry ");
		arguments.append("--public-entry ");
		arguments.append("--remove-fields ");
		arguments.append("--skip-method-removal ");
		arguments.append("--test-output ");
		arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");

		Application.main(arguments.toString().split("\\s+"));

		assertEquals(Application.testOutputBefore.getRun(), Application.testOutputAfter.getRun());
		assertEquals(0, Application.testOutputBefore.getErrors());
		assertEquals(10, Application.testOutputAfter.getErrors());

		assertTrue(jarIntact());
	}

	/**
	 * After enabling TamiFlex, there should be no test failure after removing unused fields.
	 */
    @Test
    public void runFieldRemovalOnJUnitWithTamiFlex(){
        //This test ensures that all test cases pass before and after the tool is run
        StringBuilder arguments = new StringBuilder();
        arguments.append("--prune-app ");
        arguments.append("--maven-project \"" + getJunitProjectDir() + "\" ");
        arguments.append("--main-entry ");
        arguments.append("--test-entry ");
		arguments.append("--tamiflex " + getTamiFlexJar().getAbsolutePath() + " ");
		arguments.append("--public-entry ");
        arguments.append("--remove-fields ");
		arguments.append("--skip-method-removal ");
        arguments.append("--test-output ");
	    arguments.append("--log-directory " + getLogDirectory().getAbsolutePath() + " ");

        Application.main(arguments.toString().split("\\s+"));

        assertEquals(Application.testOutputBefore.getRun(), Application.testOutputAfter.getRun());
        assertEquals(Application.testOutputBefore.getErrors(), Application.testOutputAfter.getErrors());
        assertEquals(Application.testOutputBefore.getFailures(), Application.testOutputAfter.getFailures());
        assertEquals(Application.testOutputBefore.getSkipped(), Application.testOutputAfter.getSkipped());

		assertTrue(jarIntact());
    }

    @Test
	public void runClassCollapsingOnNettySocketIO() {
    	// the test case tests the bug in issue#38
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getNettySocketIOProjectDir() + "\" ");
		arguments.append("--public-entry ");
//		arguments.append("--main-entry ");
//		arguments.append("--test-entry ");
//		arguments.append("--tamiflex " + getTamiFlexJar().getAbsolutePath() + " ");
		arguments.append("--skip-method-removal ");
		arguments.append("--class-collapser ");
		arguments.append("--verbose ");
		arguments.append("-T ");

		Application.main(arguments.toString().split("\\s+"));
	}

	@Test
	public void runClassCollapsingOnGecco() {
		// the test case tests the bug in issue#39
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getGeccoProjectDir() + "\" ");
//		arguments.append("--public-entry ");
		arguments.append("--main-entry ");
		arguments.append("--test-entry ");
		arguments.append("--tamiflex " + getTamiFlexJar().getAbsolutePath() + " ");
		arguments.append("--skip-method-removal ");
		arguments.append("--class-collapser ");
		arguments.append("--verbose ");
		arguments.append("-T ");

		Application.main(arguments.toString().split("\\s+"));
	}
}
