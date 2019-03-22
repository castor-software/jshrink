package edu.ucla.cs.jshrinklib.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.ucla.cs.jshrinklib.reachability.FieldData;
import edu.ucla.cs.jshrinklib.reachability.MethodData;
import org.junit.Test;

public class ASMUtilsTest {
	@Test
	public void testReadClassFromJar() {
		File jarPath = new File(ASMUtilsTest.class.getClassLoader().getResource("Jama-1.0.3.jar").getFile());
		HashSet<String> classes = new HashSet<String>();
		HashSet<MethodData> methods = new HashSet<MethodData>();
		HashSet<FieldData> fields = new HashSet<FieldData>();
		Map<MethodData, Set<FieldData>> fieldRefs = new HashMap<MethodData, Set<FieldData>>();
		ASMUtils.readClass(jarPath, classes, methods, fields, fieldRefs);
		assertEquals(9, classes.size());
		assertEquals(118, methods.size());
		assertEquals(35, fields.size());
		assertEquals(118, fieldRefs.size());
	}
	
	@Test
	public void testReadClassFromDir() {
		File jarPath = new File(ASMUtilsTest.class.getClassLoader().getResource("Jama-1.0.3").getFile());
		HashSet<String> classes = new HashSet<String>();
		HashSet<MethodData> methods = new HashSet<MethodData>();
		HashSet<FieldData> fields = new HashSet<FieldData>();
		Map<MethodData, Set<FieldData>> fieldRefs = new HashMap<MethodData, Set<FieldData>>();
		ASMUtils.readClass(jarPath, classes, methods, fields, fieldRefs);
		assertEquals(9, classes.size());
		assertEquals(118, methods.size());
		assertEquals(35, fields.size());
		assertEquals(118, fieldRefs.size());
	}
}
