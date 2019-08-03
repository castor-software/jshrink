package edu.ucla.cs.jshrinklib.util;

import edu.ucla.cs.jshrinklib.reachability.FieldData;
import edu.ucla.cs.jshrinklib.reachability.MethodData;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class DependencyGraphUtilsTest {
	@Test
	public void testReadClassFromJar() {
		File jarPath = new File(DependencyGraphUtilsTest.class.getClassLoader().getResource("Jama-1.0.3.jar").getFile());
		HashSet<String> classes = new HashSet<String>();
		HashSet<MethodData> methods = new HashSet<MethodData>();
		HashSet<FieldData> fields = new HashSet<FieldData>();
		Map<MethodData, Set<FieldData>> fieldRefs = new HashMap<MethodData, Set<FieldData>>();
		Map<MethodData, Set<MethodData>> virtualCalls = new HashMap<MethodData, Set<MethodData>>();

		DependencyGraphUtils.readClassWithDependencies(jarPath, classes, methods, fields, fieldRefs, virtualCalls, null);
		assertEquals(9, classes.size());
		assertEquals(118, methods.size());
		assertEquals(35, fields.size());
		assertEquals(118, virtualCalls.size());
		Set<MethodData> allVirtualCalls = new HashSet<MethodData>();
		for(MethodData md : virtualCalls.keySet()) {
			allVirtualCalls.addAll(virtualCalls.get(md));
		}
		assertEquals(102, allVirtualCalls.size());
	}
	
	@Test
	public void testReadClassFromDir() {
		File jarPath = new File(DependencyGraphUtilsTest.class.getClassLoader().getResource("Jama-1.0.3").getFile());
		HashSet<String> classes = new HashSet<String>();
		HashSet<MethodData> methods = new HashSet<MethodData>();
		HashSet<FieldData> fields = new HashSet<FieldData>();
		Map<MethodData, Set<FieldData>> fieldRefs = new HashMap<MethodData, Set<FieldData>>();
		Map<MethodData, Set<MethodData>> virtualCalls = new HashMap<MethodData, Set<MethodData>>();
		DependencyGraphUtils.readClassWithDependencies(jarPath, classes, methods, fields, fieldRefs, virtualCalls, null);
		assertEquals(9, classes.size());
		assertEquals(118, methods.size());
		assertEquals(35, fields.size());
		assertEquals(118, fieldRefs.size());
		assertEquals(118, virtualCalls.size());
		Set<MethodData> allVirtualCalls = new HashSet<MethodData>();
		for(MethodData md : virtualCalls.keySet()) {
			allVirtualCalls.addAll(virtualCalls.get(md));
		}
		assertEquals(102, allVirtualCalls.size());
	}
	@Test
	public void testClassDependencies() {

	}

	@Test
	public void testMultiClassGraphWithJars(){

	}
}
