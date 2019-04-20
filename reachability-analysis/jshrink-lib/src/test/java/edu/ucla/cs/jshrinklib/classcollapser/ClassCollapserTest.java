package edu.ucla.cs.jshrinklib.classcollapser;

import edu.ucla.cs.jshrinklib.TestUtils;
import edu.ucla.cs.jshrinklib.reachability.MethodData;
import edu.ucla.cs.jshrinklib.util.ClassFileUtils;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;
import soot.*;
import soot.jimple.InvokeStmt;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

public class ClassCollapserTest {

    @After
    public void after(){
        G.reset();
    }

    @Test
    public void classClassifierTest() throws IOException {
        String overridePath
                = new File(ClassCollapser.class.getClassLoader()
                .getResource("classcollapser"
                        + File.separator + "override" + File.separator + "original").getFile()).getAbsolutePath();
        TestUtils.soot_setup(overridePath);

        Set<String> appClasses = new HashSet<String>();
        appClasses.add("A");
        appClasses.add("B");

        Set<String> usedAppClasses = new HashSet<String>();
        usedAppClasses.add("A"); // A is considered used due to the virtual call to A.foo in Main
        usedAppClasses.add("B");

        MethodData m1 = new MethodData("foo", "B", "void", new String[] {}, true, false);
        MethodData m2 = new MethodData("<init>", "B", "void", new String[] {}, true, false);
        MethodData m3 = new MethodData("<init>", "A", "void", new String[] {}, true, false);
        MethodData m4 = new MethodData("foo", "A", "void", new String[] {}, true, false);
        MethodData m5 = new MethodData("main", "Main", "void", new String[] {"java.lang.String[]"}, true, true);

        Set<MethodData> usedAppMethodData = new HashSet<MethodData>();
        usedAppMethodData.add(m1);
        usedAppMethodData.add(m2);
        usedAppMethodData.add(m3);
        usedAppMethodData.add(m4);

        Map<MethodData, Set<MethodData>> callGraph = new HashMap<MethodData, Set<MethodData>>();

        Set<MethodData> m1_callers = new HashSet<MethodData>();
        m1_callers.add(m5);
        callGraph.put(m1, m1_callers);
        Set<MethodData> m2_callers = new HashSet<MethodData>();
        m2_callers.add(m5);
        callGraph.put(m2, m2_callers);
        Set<MethodData> m3_callers = new HashSet<MethodData>();
        m3_callers.add(m2);
        callGraph.put(m3, m3_callers);
        // A.foo is a virtual call so its caller set is empty
        callGraph.put(m4, new HashSet<MethodData>());
        // Main.main is an entry method so its caller set is also empty
        callGraph.put(m5, new HashSet<MethodData>());

        Set<MethodData> entryPoints = new HashSet<MethodData>();
        entryPoints.add(m5);

        ClassCollapserAnalysis classCollapserAnalysis
                = new ClassCollapserAnalysis(appClasses,usedAppClasses,usedAppMethodData, callGraph, entryPoints);
        classCollapserAnalysis.run();

        ClassCollapser classCollapser = new ClassCollapser();
        classCollapser.run(classCollapserAnalysis, new HashSet<String>());

        ClassCollapserData classCollapserData = classCollapser.getClassCollapserData();
        assertTrue(classCollapserData.getClassesToRemove().contains("B"));
        assertEquals(1, classCollapserData.getClassesToRemove().size());
        assertTrue(classCollapserData.getClassesToRewrite().contains("A"));
        assertEquals(1, classCollapserData.getClassesToRewrite().size());
    }

    @Test
    public void mergeTwoClassesTest_override() {
        String overridePath
            = new File(ClassCollapser.class.getClassLoader()
            .getResource("classcollapser"
                + File.separator + "override" + File.separator + "original").getFile()).getAbsolutePath();
        SootClass A = TestUtils.getSootClass(overridePath,"A");
        SootClass B = TestUtils.getSootClass(overridePath,"B");

        assertEquals(2, A.getMethods().size());
        assertEquals(2, B.getMethodCount());

        HashMap<String, Set<String>> usedMethods = new HashMap<String, Set<String>>();
        usedMethods.put("B", new HashSet<String>());
        for (SootMethod m : B.getMethods()) {
            usedMethods.get("B").add(m.getSubSignature());
        }

        ClassCollapser.mergeTwoClasses(B, A, usedMethods);

        assertEquals(2, A.getMethodCount());
        for (Unit u: A.getMethodByName("foo").retrieveActiveBody().getUnits()) {
            if (u instanceof InvokeStmt) {
                assertEquals("\"class B\"", ((InvokeStmt)u).getInvokeExpr().getArg(0).toString());
            }
        }
    }

    @Test
    public void mergeTwoClassesTest_field() {
        String fieldPath
                = new File(ClassCollapser.class.getClassLoader()
                .getResource("classcollapser" + File.separator
                    + "field" + File.separator + "original").getFile()).getAbsolutePath();
        SootClass A = TestUtils.getSootClass(fieldPath,"A");
        SootClass B = TestUtils.getSootClass(fieldPath,"B");

        assertEquals(1, A.getFieldCount());
        assertEquals(1, B.getFieldCount());

        ClassCollapser.mergeTwoClasses(B, A, new HashMap<String, Set<String>>());

        assertEquals(2, A.getFieldCount());
        assertNotNull(A.getFieldByName("a"));
        assertNotNull(A.getFieldByName("b"));
    }

    @Test
    public void changeClassNameTest_override() {
        String overridePath
                = new File(ClassCollapser.class.getClassLoader()
                .getResource("classcollapser" + File.separator
                    + "override" + File.separator + "original").getFile()).getAbsolutePath();
        SootClass A = TestUtils.getSootClass(overridePath,"A");
        SootClass B = TestUtils.getSootClass(overridePath,"B");
        SootClass main = TestUtils.getSootClass(overridePath, "Main");

        ClassCollapser.changeClassNamesInClass(main, B, A);
        for (SootMethod m: main.getMethods()) {
            Body body = m.retrieveActiveBody();
            for (Local l: body.getLocals()) {
                assertNotEquals("B", l.getType().toString());
            }
        }
    }

    @Test
    public void mergeTwoClassesWithOverridenFields() throws IOException {
        // This is a special case where a superclass and its sublcass have a field with the same name, and the field
        // is referenced by the class constructor in both classes. It will cause a "Resolved field is null" exception
        // when Soot writes out the merged class to a class file
        String classPath = new File(ClassCollapser.class.getClassLoader()
                .getResource("classcollapser" + File.separator
                        + "overrideField").getFile()).getAbsolutePath();

        File dir = new File(classPath + File.separator + "collapse");
        if(dir.exists()) {
            FileUtils.deleteDirectory(dir);
        }
        dir.mkdir();

        SootClass A = TestUtils.getSootClass(classPath, "A");
        SootClass SubA = TestUtils.getSootClass(classPath, "SubA");

        HashMap<String, Set<String>> usedMethods = new HashMap<String, Set<String>>();
        usedMethods.put("SubA", new HashSet<String>());
        for (SootMethod m : SubA.getMethods()) {
            usedMethods.get("SubA").add(m.getSubSignature());
        }

        ClassCollapser.mergeTwoClasses(SubA, A, usedMethods);

        ClassFileUtils.writeClass(A, new File(dir.getAbsolutePath() + File.separator + "A.class"));
    }

    @Test
    public void classClassifierTest_simpleCollapseExample() throws IOException{
        String overridePath
                = new File(ClassCollapser.class.getClassLoader()
                .getResource("classcollapser" + File.separator
                    + "simple-collapse-example" + File.separator + "target" + File.separator + "classes").getFile()).getAbsolutePath();
        TestUtils.soot_setup(overridePath);

        Set<String> appClasses = new HashSet<String>();
        appClasses.add("A");
        appClasses.add("B");
        appClasses.add("C");
        appClasses.add("Main");

        Set<String> usedAppClasses = new HashSet<String>();
        usedAppClasses.add("A");
        usedAppClasses.add("B");
        usedAppClasses.add("Main");

        MethodData m1 = new MethodData("saySomething", "B", "java.lang.String", new String[] {}, true, false);
        MethodData m2 = new MethodData("<init>", "B", "void", new String[] {}, true, false);
        MethodData m3 = new MethodData("uniqueToB", "B", "java.lang.String", new String[] {}, true, false);
        MethodData m4 = new MethodData("uniqueToA", "A", "java.lang.String", new String[] {}, true, false);
        MethodData m5 = new MethodData("<init>", "A", "void", new String[] {"java.lang.String"}, true, false);
        MethodData m6 = new MethodData("getClassType", "A", "java.lang.String", new String[] {}, true, false);
        MethodData m7 = new MethodData("saySomething", "A", "java.lang.String", new String[] {}, true, false);
        MethodData m8 = new MethodData("main", "Main", "void", new String[] {"java.lang.String[]"}, true, true);

        Set<MethodData> usedAppMethodData = new HashSet<MethodData>();
        usedAppMethodData.add(m1);
        usedAppMethodData.add(m2);
        usedAppMethodData.add(m3);
        usedAppMethodData.add(m4);
        usedAppMethodData.add(m5);
        usedAppMethodData.add(m6);
        usedAppMethodData.add(m7);
        usedAppMethodData.add(m8);;

        Map<MethodData, Set<MethodData>> callGraph = new HashMap<MethodData, Set<MethodData>>();
        Set<MethodData> m1_callers = new HashSet<MethodData>();
        m1_callers.add(m8);
        callGraph.put(m1, m1_callers);
        Set<MethodData> m2_callers = new HashSet<MethodData>();
        m2_callers.add(m8);
        callGraph.put(m2, m2_callers);
        Set<MethodData> m3_callers = new HashSet<MethodData>();
        m3_callers.add(m8);
        callGraph.put(m3, m3_callers);
        Set<MethodData> m4_callers = new HashSet<MethodData>();
        m4_callers.add(m8);
        callGraph.put(m4, m4_callers);
        // the superclass constructor is only called in the subclass constructor
        Set<MethodData> m5_callers = new HashSet<MethodData>();
        m5_callers.add(m2);
        callGraph.put(m5, m5_callers);
        Set<MethodData> m6_callers = new HashSet<MethodData>();
        m6_callers.add(m8);
        callGraph.put(m6, m6_callers);
        // this is a virtual call, therefore the caller set is set to empty
        callGraph.put(m7, new HashSet<MethodData>());
        // main is the entry method, so its caller set is also empty
        callGraph.put(m8, new HashSet<MethodData>());

        Set<MethodData> entryPoints = new HashSet<MethodData>();
        entryPoints.add(m8);

        ClassCollapserAnalysis classCollapserAnalysis
                = new ClassCollapserAnalysis(appClasses,usedAppClasses,usedAppMethodData, callGraph, entryPoints);
        classCollapserAnalysis.run();

        ClassCollapser classCollapser = new ClassCollapser();
        classCollapser.run(classCollapserAnalysis, new HashSet<String>());

        ClassCollapserData classCollapserData = classCollapser.getClassCollapserData();

        assertEquals(1,classCollapserData.getClassesToRemove().size());
        assertTrue(classCollapserData.getClassesToRemove().contains("B"));

        assertEquals(2, classCollapserData.getClassesToRewrite().size());
        assertTrue(classCollapserData.getClassesToRewrite().contains("A"));
        assertTrue(classCollapserData.getClassesToRewrite().contains("Main"));

        SootClass A = TestUtils.getSootClass(overridePath, "A");

        assertNotNull(A.getMethodByName("getClassType"));
        assertNotNull(A.getMethodByName("saySomething"));
        assertNotNull(A.getMethodByName("uniqueToA"));
        assertNotNull(A.getMethodByName("uniqueToB"));

        SootMethod saySomething = A.getMethodByName("saySomething");
        assertTrue(saySomething.retrieveActiveBody().toString().contains("\"I am class B\""));
    }

    @Test(expected = RuntimeException.class)
    public void testIssue38() {
        // Issue#38: https://github.com/tianyi-zhang/call-graph-analysis/issues/38
        // this test reproduces issue#38 in a much simpler scenario
        // the bug is not fixed yet
        String classPath
                = new File(ClassCollapser.class.getClassLoader()
                .getResource("classcollapser" + File.separator + "issue38").getFile()).getAbsolutePath();

        String before = TestUtils.runClass(classPath, "Main");

        SootClass B = TestUtils.getSootClass(classPath, "B");
        SootClass A = TestUtils.getSootClass(classPath, "A");

        SootClass Implementation = TestUtils.getSootClass(classPath, "SomeInterfaceImplementation");
        SootClass Interface = TestUtils.getSootClass(classPath, "SomeInterface");

        HashMap<String, Set<String>> usedMethods = new HashMap<String, Set<String>>();
        usedMethods.put("B", new HashSet<String>());
        for (SootMethod m : B.getMethods()) {
            usedMethods.get("B").add(m.getSubSignature());
        }
        usedMethods.put("SomeInterfaceImplementation", new HashSet<String>());
        for(SootMethod m : Implementation.getMethods()) {
            usedMethods.get("SomeInterfaceImplementation").add(m.getSubSignature());
        }

        ClassCollapser.mergeTwoClasses(Implementation, Interface, usedMethods);
        ClassCollapser.mergeTwoClasses(B, A, usedMethods);
    }
}
