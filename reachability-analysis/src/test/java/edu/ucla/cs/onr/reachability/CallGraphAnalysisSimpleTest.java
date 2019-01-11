package edu.ucla.cs.onr.reachability;

import static org.junit.Assert.*;

import java.io.File;
import java.util.*;

import org.junit.After;
import org.junit.Test;

import soot.G;

import javax.swing.text.html.Option;

public class CallGraphAnalysisSimpleTest {
	
	/* Class Hierarchy for the sample project
	 *    A
	 *   / \
	 *  B   C
	 *  
	 * There are four main classes that demonstrate different usage scenarios.
	 * There are is also one test file with three test cases. 
	 *  
	 */
	
	/**
	 * Test 1. 
	 * 
	 * A a = new B("a", "b");
	 * a.foo();
	 * 
	 * Because Soot assigns the most narrow type to a local variable when generating Jimple,
	 * the declared type of the local variable, a, in the Jimple code is B. 
	 * Since B does not have subclasses, both CHA and Spark generates the same call graph 
	 * for this program.
	 * 
	 */
	@Test
	public void testSparkOnDynamicDispatching1() {
		ClassLoader classLoader = CallGraphAnalysisSimpleTest.class.getClassLoader();
		List<File> libJarPath = new ArrayList<File>();
        List<File> appClassPath = new ArrayList<File>();
        appClassPath.add(new File(classLoader.getResource("simple-test-project2/target/classes").getFile()));
        List<File> appTestPath = new ArrayList<File>(); 
        appTestPath.add(new File(classLoader.getResource("simple-test-project2/target/test-classes").getFile()));
        Set<MethodData> entryMethods = new HashSet<MethodData>();
        MethodData entry = 
        		new MethodData("main", "Main", "void", new String[] {"java.lang.String[]"}, true, true);
        entryMethods.add(entry);
        CallGraphAnalysis runner = 
        		new CallGraphAnalysis(libJarPath, appClassPath, appTestPath, new EntryPointProcessor(false, false, false,false, entryMethods));
        runner.run();
        assertEquals(3, runner.getUsedAppClasses().size());
        assertEquals(5, runner.getUsedAppMethods().size());
	}
	
	@Test
	public void testCHAOnDynamicDispatching1() {
		// disable Spark and use CHA instead
		CallGraphAnalysis.useSpark = false;
		ClassLoader classLoader = CallGraphAnalysisSimpleTest.class.getClassLoader();
		List<File> libJarPath = new ArrayList<File>();
        List<File> appClassPath = new ArrayList<File>();
        appClassPath.add(new File(classLoader.getResource("simple-test-project2/target/classes").getFile()));
        List<File> appTestPath = new ArrayList<File>(); 
        appTestPath.add(new File(classLoader.getResource("simple-test-project2/target/test-classes").getFile()));
        Set<MethodData> entryMethods = new HashSet<MethodData>();
        MethodData entry = 
        		new MethodData("main", "Main", "void", new String[] {"java.lang.String[]"}, true, true);
        entryMethods.add(entry);
        CallGraphAnalysis runner = 
        		new CallGraphAnalysis(libJarPath, appClassPath, appTestPath, new EntryPointProcessor(false, false, false,false, entryMethods));
        runner.run();
        assertEquals(3, runner.getUsedAppClasses().size());
        assertEquals(5, runner.getUsedAppMethods().size());
	}
	
	/**
	 * Test 2. 
	 * 
	 * A a = new A("a");
	 * a.foo();
	 * 
	 * The declared type of the local variable, a, is A.
	 * Spark knows that its real type is A since it keeps track of object allocations.
	 * But CHA does not know that so it looks up to the class hierarchy and finds that
	 * A has two subclasses B and C. So it thinks a.foo() can also be dispatched to B.foo()
	 * and C.foo() and therefore generates a bigger call graph.
	 *    
	 */
	@Test
	public void testSparkOnDynamicDispatching2() {
		ClassLoader classLoader = CallGraphAnalysisSimpleTest.class.getClassLoader();
		List<File> libJarPath = new ArrayList<File>();
        List<File> appClassPath = new ArrayList<File>();
        appClassPath.add(new File(classLoader.getResource("simple-test-project2/target/classes").getFile()));
        List<File> appTestPath = new ArrayList<File>(); 
        appTestPath.add(new File(classLoader.getResource("simple-test-project2/target/test-classes").getFile()));
        Set<MethodData> entryMethods = new HashSet<MethodData>();
        MethodData entry = 
        		new MethodData("main", "Main2", "void", new String[] {"java.lang.String[]"}, true, true);
        entryMethods.add(entry);
        CallGraphAnalysis runner = 
        		new CallGraphAnalysis(libJarPath, appClassPath, appTestPath, new EntryPointProcessor(false, false, false,false, entryMethods));
        runner.run();
        assertEquals(2, runner.getUsedAppClasses().size());
        assertEquals(3, runner.getUsedAppMethods().size());
	}
	
	@Test
	public void testCHAOnDynamicDispatching2() {
		// disable Spark and use CHA instead
		CallGraphAnalysis.useSpark = false;
		ClassLoader classLoader = CallGraphAnalysisSimpleTest.class.getClassLoader();
		List<File> libJarPath = new ArrayList<File>();
        List<File> appClassPath = new ArrayList<File>();
        appClassPath.add(new File(classLoader.getResource("simple-test-project2/target/classes").getFile()));
        List<File> appTestPath = new ArrayList<File>(); 
        appTestPath.add(new File(classLoader.getResource("simple-test-project2/target/test-classes").getFile()));
        Set<MethodData> entryMethods = new HashSet<MethodData>();
        MethodData entry = 
        		new MethodData("main", "Main2", "void", new String[] {"java.lang.String[]"}, true, true);
        entryMethods.add(entry);
        CallGraphAnalysis runner = 
        		new CallGraphAnalysis(libJarPath, appClassPath, appTestPath, new EntryPointProcessor(false, false, false,false, entryMethods));
        runner.run();
        assertEquals(4, runner.getUsedAppClasses().size());
        assertEquals(6, runner.getUsedAppMethods().size());
	}
	
	/**
	 * Test 3. 
	 * 
	 * void main() {
	 *   A b = new B("a", "b");
	 *   delegate(b);
	 * }
	 *   
	 * void delegate(A a) {
	 * 	 a.foo();
	 * }
	 * 
	 * The declared type of the local variable, a, is B.
	 * Spark knows that its real type is A since it keeps track of object allocations
	 * and assignments across procedures.  
	 * But CHA does not know that so it looks up to the class hierarchy and finds that
	 * A has two subclasses B and C. So it thinks a.foo() can also be dispatched to B.foo()
	 * and C.foo() and therefore generates a bigger call graph.
	 *    
	 */
	@Test
	public void testSparkOnDynamicDispatching3() {
		ClassLoader classLoader = CallGraphAnalysisSimpleTest.class.getClassLoader();
		List<File> libJarPath = new ArrayList<File>();
        List<File> appClassPath = new ArrayList<File>();
        appClassPath.add(new File(classLoader.getResource("simple-test-project2/target/classes").getFile()));
        List<File> appTestPath = new ArrayList<File>(); 
        appTestPath.add(new File(classLoader.getResource("simple-test-project2/target/test-classes").getFile()));
        Set<MethodData> entryMethods = new HashSet<MethodData>();
        MethodData entry = 
        		new MethodData("main", "Main3", "void", new String[] {"java.lang.String[]"}, true, true);
        entryMethods.add(entry);
        CallGraphAnalysis runner = 
        		new CallGraphAnalysis(libJarPath, appClassPath, appTestPath, new EntryPointProcessor(false, false, false,false, entryMethods));
        runner.run();
        assertEquals(3, runner.getUsedAppClasses().size());
        assertEquals(6, runner.getUsedAppMethods().size());
	}
	
	@Test
	public void testCHAOnDynamicDispatching3() {
		// disable Spark and use CHA instead
		CallGraphAnalysis.useSpark = false;
		ClassLoader classLoader = CallGraphAnalysisSimpleTest.class.getClassLoader();
		List<File> libJarPath = new ArrayList<File>();
        List<File> appClassPath = new ArrayList<File>();
        appClassPath.add(new File(classLoader.getResource("simple-test-project2/target/classes").getFile()));
        List<File> appTestPath = new ArrayList<File>(); 
        appTestPath.add(new File(classLoader.getResource("simple-test-project2/target/test-classes").getFile()));
        Set<MethodData> entryMethods = new HashSet<MethodData>();
        MethodData entry = 
        		new MethodData("main", "Main3", "void", new String[] {"java.lang.String[]"}, true, true);
        entryMethods.add(entry);
        CallGraphAnalysis runner = 
        		new CallGraphAnalysis(libJarPath, appClassPath, appTestPath, new EntryPointProcessor(false, false, false,false, entryMethods));
        runner.run();
        assertEquals(4, runner.getUsedAppClasses().size());
        assertEquals(8, runner.getUsedAppMethods().size());
	}
	
	/**
	 * Test 3. 
	 * 
	 * void main() {
	 *   A b = new B("a", "b");
	 *   A c = new C("a", 1);
	 *   delegate(b);
	 * }
	 *   
	 * void delegate(A a) {
	 * 	 a.foo();
	 * }
	 * 
	 * Just one more test to show that Spark keeps track of data flow across procedures.
	 * Though other algorithms such as RTA also keeps track of object allocations, but RTA
	 * does not perform points-to analysis but only estimate possible types just based on 
	 * previous object allocations. 
	 *    
	 */
	@Test
	public void testSparkOnDynamicDispatching4() {
		ClassLoader classLoader = CallGraphAnalysisSimpleTest.class.getClassLoader();
		List<File> libJarPath = new ArrayList<File>();
        List<File> appClassPath = new ArrayList<File>();
        appClassPath.add(new File(classLoader.getResource("simple-test-project2/target/classes").getFile()));
        List<File> appTestPath = new ArrayList<File>(); 
        appTestPath.add(new File(classLoader.getResource("simple-test-project2/target/test-classes").getFile()));
        Set<MethodData> entryMethods = new HashSet<MethodData>();
        MethodData entry = 
        		new MethodData("main", "Main4", "void", new String[] {"java.lang.String[]"}, true, true);
        entryMethods.add(entry);
        CallGraphAnalysis runner = 
        		new CallGraphAnalysis(libJarPath, appClassPath, appTestPath, new EntryPointProcessor(false, false, false,false, entryMethods));
        runner.run();
        assertEquals(4, runner.getUsedAppClasses().size());
        assertEquals(7, runner.getUsedAppMethods().size());
	}
	
	@Test
	public void testCHAOnDynamicDispatching4() {
		// disable Spark and use CHA instead
		CallGraphAnalysis.useSpark = false;
		ClassLoader classLoader = CallGraphAnalysisSimpleTest.class.getClassLoader();
		List<File> libJarPath = new ArrayList<File>();
        List<File> appClassPath = new ArrayList<File>();
        appClassPath.add(new File(classLoader.getResource("simple-test-project2/target/classes").getFile()));
        List<File> appTestPath = new ArrayList<File>(); 
        appTestPath.add(new File(classLoader.getResource("simple-test-project2/target/test-classes").getFile()));
        Set<MethodData> entryMethods = new HashSet<MethodData>();
        MethodData entry = 
        		new MethodData("main", "Main4", "void", new String[] {"java.lang.String[]"}, true, true);
        entryMethods.add(entry);
        CallGraphAnalysis runner = 
        		new CallGraphAnalysis(libJarPath, appClassPath, appTestPath, new EntryPointProcessor(false, false, false,false, entryMethods));
        runner.run();
        assertEquals(4, runner.getUsedAppClasses().size());
        assertEquals(9, runner.getUsedAppMethods().size());
	}

	@Test
	public void testCallGraphInfo(){
		CallGraphAnalysis.useSpark = true;
		ClassLoader classLoader = CallGraphAnalysisSimpleTest.class.getClassLoader();
		List<File> libJarPath = new ArrayList<File>();
		libJarPath.add(
				new File(classLoader.getResource("simple-test-project/libs/standard-stuff-library.jar").getFile()));
		List<File> appClassPath = new ArrayList<File>();
		appClassPath.add(new File(classLoader.getResource("simple-test-project/target/classes").getFile()));
		List<File> appTestPath = new ArrayList<File>();
		appTestPath.add(new File(classLoader.getResource("simple-test-project/target/test-classes").getFile()));
		CallGraphAnalysis runner = new CallGraphAnalysis(libJarPath, appClassPath, appTestPath,
				new EntryPointProcessor(true, false, false,
						false, new HashSet<MethodData>()));
		runner.run();
		Map<MethodData, Set<MethodData>> usedAppMethods = runner.getUsedAppMethods();
		Map<MethodData, Set<MethodData>> usedLibMethods = runner.getUsedLibMethods();

		Optional<Set<MethodData>> mainMethod = get(usedAppMethods, "Main", "main");
		assertTrue(mainMethod.isPresent());
		assertEquals(0, mainMethod.get().size());

		Optional<Set<MethodData>> standardStuffInit = get(usedAppMethods, "StandardStuff", "<init>");
		assertTrue(standardStuffInit.isPresent());
		assertEquals(1,standardStuffInit.get().size());
		assertTrue(contains(standardStuffInit.get(),"Main", "main"));

		/*
		The following is a bit unintuitive, but "StandardStuff$NestClass:<init>(StandardStuff$1)" is called by
		"StandardStuff:<init>()". StandardStuff$NestedClass<init>() is then called by
		"StandardStuff$NestedClass<init>(StandardStuff$1)". However, our "get" method does not consider parameters so
		this is a bit incomplete.
		 */
		Optional<Set<MethodData>> nestedClassInit =
				get(usedAppMethods, "StandardStuff$NestedClass", "<init>");
		assertTrue(nestedClassInit.isPresent());
		assertEquals(1, nestedClassInit.get().size());
		assertTrue(contains(nestedClassInit.get(),"StandardStuff$NestedClass", "<init>"));

		Optional<Set<MethodData>> nestedClassMethod =
				get(usedAppMethods, "StandardStuff$NestedClass", "nestedClassMethod");
		assertTrue(nestedClassMethod.isPresent());
		assertEquals(1, nestedClassMethod.get().size());
		assertTrue(contains(nestedClassMethod.get(),"StandardStuff", "<init>"));

		Optional<Set<MethodData>> getString = get(usedAppMethods, "StandardStuff", "getString");
		assertTrue(getString.isPresent());
		assertEquals(1, getString.get().size());
		assertTrue(contains(getString.get(), "Main", "main"));

		Optional<Set<MethodData>> getStringStatic =
				get(usedAppMethods, "StandardStuff", "getStringStatic");
		assertTrue(getStringStatic.isPresent());
		assertEquals(1, getStringStatic.get().size());
		assertTrue(contains(getStringStatic.get(), "StandardStuff", "getString"));

		Optional<Set<MethodData>> libraryClassInit =
				get(usedLibMethods, "edu.ucla.cs.onr.test.LibraryClass", "<init>");
		assertTrue(libraryClassInit.isPresent());
		assertEquals(1,libraryClassInit.get().size());
		assertTrue(contains(libraryClassInit.get(), "Main", "main"));

		Optional<Set<MethodData>> getNumber =
				get(usedLibMethods, "edu.ucla.cs.onr.test.LibraryClass", "getNumber");
		assertTrue(getNumber.isPresent());
		assertEquals(1, getNumber.get().size());
		assertTrue(contains(getNumber.get(), "Main", "main"));
	}

	private static Optional<Set<MethodData>> get(Map<MethodData,Set<MethodData>> map, String className, String methodName){
		MethodData methodData = null;
		for(Map.Entry<MethodData, Set<MethodData>> entry : map.entrySet()){
			if(entry.getKey().getClassName().equals(className) && entry.getKey().getName().equals(methodName)
			&& (methodData == null || methodData.getArgs().length > entry.getKey().getArgs().length)){
				methodData = entry.getKey();
			}
		}
		if(methodData != null){
			return Optional.of(map.get(methodData));
		}
		return Optional.empty();
	}

	private static boolean contains(Set<MethodData> set, String className, String methodName){
		for(MethodData methodData : set){
			if(methodData.getClassName().equals(className) && methodData.getName().equals(methodName)){
				return true;
			}
		}
		return false;
	}
	
	@After
	public void cleanup() {
		G.reset();
	}
}
