package edu.ucla.cs.jshrinklib.classcollapser;

import edu.ucla.cs.jshrinklib.reachability.MethodData;
import edu.ucla.cs.jshrinklib.util.SootUtils;
import fj.P;
import soot.*;
import soot.JastAddJ.Annotation;
import soot.jimple.*;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JCastExpr;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JInstanceOfExpr;
import soot.jimple.toolkits.invoke.SiteInliner;
import soot.tagkit.*;

import java.util.*;
import java.util.regex.Pattern;

public class ClassCollapser {

    private final Set<String> classesToRewrite;
    private final Set<String> classesToRemove;
    private final Set<MethodData> removedMethods;


    public ClassCollapser() {
        this.classesToRemove = new HashSet<String>();
        this.classesToRewrite = new HashSet<String>();
        this.removedMethods = new HashSet<MethodData>();
    }

    public void run(ClassCollapserAnalysis classCollapserAnalysis, Set<String> testClasses){
        HashMap<String, SootClass> nameToSootClass = new HashMap<String, SootClass>();

        for (ArrayList<String> collapse: classCollapserAnalysis.getCollapseList()) {
            String fromName = collapse.get(0);
            String toName = collapse.get(1);
            if (!nameToSootClass.containsKey(fromName)) {
                nameToSootClass.put(fromName, Scene.v().loadClassAndSupport(fromName));
            }
            if (!nameToSootClass.containsKey(toName)) {
                nameToSootClass.put(toName, Scene.v().loadClassAndSupport(toName));
            }
            SootClass from = nameToSootClass.get(fromName);
            SootClass to = nameToSootClass.get(toName);

            Set<MethodData> removedMethods =
                    ClassCollapser.mergeTwoClasses(from, to, ((ClassCollapserAnalysis) classCollapserAnalysis).getProcessedUsedMethods());

            this.removedMethods.addAll(removedMethods);

            this.classesToRewrite.add(to.getName());
            this.classesToRemove.add(from.getName());
        }

        Set<String> allClasses = new HashSet<String>();
        allClasses.addAll(classCollapserAnalysis.appClasses);
        allClasses.addAll(testClasses);

        Map<String, String> nameChangeList = classCollapserAnalysis.getNameChangeList();
        for(String fromName: nameChangeList.keySet()) {
            String toName = nameChangeList.get(fromName);
            if (!nameToSootClass.containsKey(fromName)) {
                nameToSootClass.put(fromName, Scene.v().loadClassAndSupport(fromName));
            }
            if (!nameToSootClass.containsKey(toName)) {
                nameToSootClass.put(toName, Scene.v().loadClassAndSupport(toName));
            }
            SootClass from = nameToSootClass.get(fromName);
            SootClass to = nameToSootClass.get(toName);
            for (String className : allClasses) {
                if(className.equals(fromName)) {
                    // no need to handle the collapsed class, since this class will be removed at the end
                    continue;
                }
                if (!nameToSootClass.containsKey(className)) {
                    nameToSootClass.put(className, Scene.v().loadClassAndSupport(className));
                }
                SootClass sootClass = nameToSootClass.get(className);
                if (ClassCollapser.changeClassNamesInClass(sootClass, from, to)) {
                    classesToRewrite.add(sootClass.getName());
                }
            }
        }
    }

    /**
     * Merges one soot class into another
     * @param from The class that will be merged from, and discarded (the super class)
     * @param to The class that will be merged into, and kept (the sub class)
     * @return The set of methods that have been removed
     */
    /*package*/ static Set<MethodData> mergeTwoClasses(SootClass from, SootClass to, Map<String, Set<String>> usedMethods) {
        Set<MethodData> toReturn = new HashSet<MethodData>();
        HashMap<String, SootField> originalFields = new HashMap<String, SootField>();
        for (SootField field : to.getFields()) {
            originalFields.put(field.getName(), field);
        }
        // reset modifiers
        to.setModifiers(from.getModifiers());

        // find fields that are used in super class constructors
        HashSet<String> fieldsUsedInConstructor = new HashSet<String>();
        for(SootMethod method : to.getMethods()) {
            if(method.getName().equals("<init>")) {
                Body b = method.retrieveActiveBody();
                for(Unit u : b.getUnits()) {
                    Stmt s = (Stmt) u;
                    if(!s.containsFieldRef()) {
                        continue;
                    }
                    FieldRef fr = s.getFieldRef();
                    fieldsUsedInConstructor.add(fr.getField().getName());
                }
            }
        }

        HashSet<String> renamedFields = new HashSet<String>();
        for (SootField field : from.getFields()) {
            String fieldName = field.getName();
            if (originalFields.containsKey(fieldName)) {
                // overridden field
                if(fieldsUsedInConstructor.contains(fieldName)) {
                    // must keep this field and rename
                    renamedFields.add(fieldName);
                    originalFields.get(fieldName).setName("super" + fieldName);
                } else {
                    // safely remove
                    to.getFields().remove(originalFields.get(fieldName));
                }
            }
            // reset the declaring class
            field.setDeclaringClass(to);
            to.getFields().addLast(field);
        }

        // update references to all renamed fields if any
        for(SootMethod method : to.getMethods()) {
            if(method.isAbstract() || method.isNative()) continue;
            Body b = method.retrieveActiveBody();
            for(Unit u : b.getUnits()) {
                Stmt s = (Stmt) u;
                if(!s.containsFieldRef()) {
                    continue;
                }

                FieldRef fr = s.getFieldRef();
                SootFieldRef sfr = fr.getFieldRef();
                if(renamedFields.contains(sfr.name())) {
                    // the original field has been renamed
                    AbstractSootFieldRef new_sfr = new AbstractSootFieldRef(sfr.declaringClass(),
                            "super" + sfr.name(), sfr.type(), sfr.isStatic());
                    fr.setFieldRef(new_sfr);
                }
            }
        }

        HashMap<String, SootMethod> originalMethods = new HashMap<String, SootMethod>();
        for (SootMethod method : to.getMethods()) {
            originalMethods.put(method.getSubSignature(), method);
        }
        HashSet<SootMethod> methodsToMove = new HashSet<SootMethod>();
        HashSet<SootMethod> methodsToRemoveInSuperClass = new HashSet<SootMethod>();
        List<SootMethod> fromMethods = from.getMethods();
        for (int i = 0; i < fromMethods.size(); i++) {
            SootMethod method = fromMethods.get(i);
            // find the super constructor calls in a constructor of a subclass
            Stmt toInLine = null;
            SootMethod inlinee = null;
            if (method.getName().equals("<init>")) {
                Body b = method.retrieveActiveBody();
                for (Unit u : b.getUnits()) {
                    if (u instanceof InvokeStmt) {
                        InvokeExpr expr = ((InvokeStmt)u).getInvokeExpr();
                        SootMethod m = expr.getMethod();
                        if (m.getName().equals(method.getName()) && m.getDeclaringClass().getName().equals(to.getName())) {
                            toInLine = (InvokeStmt) u;
                            inlinee = m;
                        }
                    }
                }
            }
            if (inlinee != null && toInLine != null) {
                Body b = inlinee.retrieveActiveBody();
                // inline the constructor
                SiteInliner.inlineSite(inlinee, toInLine, method);
                if (originalMethods.containsKey(method.getSubSignature())) {
                    methodsToRemoveInSuperClass.add(originalMethods.get(method.getSubSignature()));
                }
                // add this method to the methodsToMove list
                methodsToMove.add(method);
            } else {
                if (usedMethods.containsKey(from.getName()) && usedMethods.get(from.getName()).contains(method.getSubSignature())) {
                    if (!originalMethods.containsKey(method.getSubSignature())) {
                        // add this method to the methodsToMove list
                        methodsToMove.add(method);
                    } else {
                        // add this method to the methodsToMove list
                        methodsToMove.add(method);
                    }
                }
            }
        }

        // remove the unused methods in super class
        if(usedMethods.containsKey(to.getName())) {
            Set<String> usedMethodsInSuperClass = usedMethods.get(to.getName());
            for(String subSign : originalMethods.keySet()) {
                if(!usedMethodsInSuperClass.contains(subSign)) {
                    to.removeMethod(originalMethods.get(subSign));
                }
            }
        } else {
            // no methods in the super class is used
            for(SootMethod m : originalMethods.values()) {
                to.removeMethod(m);
            }
        }

        // move methods from the subclass to the superclass
        for(SootMethod m : methodsToMove) {
            toReturn.add(SootUtils.sootMethodToMethodData(m));
            from.removeMethod(m);
            to.addMethod(m);
        }

        return toReturn;
    }

    /**
     * Changes class names in the body of all methods of a class (Legacy soot approach)
     * @param c The class in which we are modifying the bodies
     * @param changeFrom The original name of the class to be changed
     * @param changeTo The new name of the class to be changed
    **/
    /*package*/ static boolean changeClassNamesInClass(SootClass c, SootClass changeFrom, SootClass changeTo) {
        assert c != changeFrom;

        boolean changed = false;
        if (c.hasSuperclass() && c.getSuperclass().getName().equals(changeFrom.getName())) {
            c.setSuperclass(changeTo);
            changed = true;
        }
        if (c.getInterfaces().contains(changeFrom)) {
            c.removeInterface(changeFrom);
            c.addInterface(changeTo);
            changed = true;
        }
        for (SootField f: c.getFields()) {
            if (f.getType() == Scene.v().getType(changeFrom.getName())) {
                f.setType(Scene.v().getType(changeTo.getName()));
                changed = true;
            }
        }

        List<Tag> tags  = c.getTags();
        for(int i = 0; i < tags.size(); i++) {
            Tag tag = tags.get(i);
            if(tag instanceof VisibilityAnnotationTag) {
                ArrayList<AnnotationTag> annotations = ((VisibilityAnnotationTag) tag).getAnnotations();
                for(AnnotationTag annotation : annotations) {
                    String type = annotation.getType();
                    Collection<AnnotationElem> values = annotation.getElems();
                    List<AnnotationElem> newValues = new ArrayList<AnnotationElem>();
                    for(AnnotationElem annotationElem: values) {
                        if(annotationElem instanceof AnnotationClassElem) {
                            String desc = ((AnnotationClassElem) annotationElem).getDesc();
                            if(desc.startsWith("L") && desc.endsWith(";")) {
                                String typeName = desc.substring(1, desc.length() - 1);
                                typeName = typeName.replaceAll(Pattern.quote("/"), ".");
                                if(typeName.equals(changeFrom.getName())) {
                                    AnnotationClassElem classElem = new AnnotationClassElem(
                                            "L" + changeTo.getName().replaceAll(Pattern.quote("."), "/") + ";",
                                            annotationElem.getKind(), annotationElem.getName());
                                    newValues.add(classElem);
                                    changed = true;
                                    continue;
                                }
                            }
                            newValues.add(annotationElem);
                        } else if (annotationElem instanceof AnnotationArrayElem) {
                            AnnotationArrayElem annotationArrayElem = (AnnotationArrayElem) annotationElem;
                            ArrayList<AnnotationElem> newValues2 = new ArrayList<AnnotationElem>();
                            for(AnnotationElem annotationElem2 : annotationArrayElem.getValues()) {
                                if(annotationElem2 instanceof AnnotationClassElem) {
                                    String desc2 = ((AnnotationClassElem) annotationElem2).getDesc();
                                    if(desc2.startsWith("L") && desc2.endsWith(";")) {
                                        String typeName2 = desc2.substring(1, desc2.length() - 1);
                                        typeName2 = typeName2.replaceAll(Pattern.quote("/"), ".");
                                        if(typeName2.equals(changeFrom.getName())) {
                                            AnnotationClassElem classElem2 = new AnnotationClassElem(
                                                    "L" + changeTo.getName().replaceAll(Pattern.quote("."), "/") + ";",
                                                    annotationElem2.getKind(), annotationElem2.getName());
                                            newValues2.add(classElem2);
                                            changed = true;
                                            continue;
                                        }
                                    }
                                }
                                newValues2.add(annotationElem2);
                            }
                            AnnotationArrayElem newArrayElem = new AnnotationArrayElem(newValues2, annotationArrayElem.getKind(), annotationArrayElem.getName());
                            newValues.add(newArrayElem);
                        } else {
                            newValues.add(annotationElem);
                        }
                    }
                    annotation.setElems(newValues);
                }
            }
        }
        List<SootMethod> sootMethods = c.getMethods();
        for (int i = 0; i < sootMethods.size(); i++) {
            SootMethod m = sootMethods.get(i);
            // I saw a case in android.jar where one method in a class has the return type of changeFrom and the other method
            // in the same class has the return type of changeTo. In Java, two methods in the same class can never have return
            // types with inheritance relationship. But Android allows this on purpose to generate stub methods which will be
            // interpreted by Android emulator. So resetting the return of the first method from changeFrom to changeTo will
            // cause a naming conflict. Nevertheless, keep one of these methods to avoid errors.
            if(m.getReturnType() == Scene.v().getType(changeFrom.getName())) {
                // check if there is another method that has the same name and the return type is changeTo
                boolean flag = false;
                for(int j = 0; j < sootMethods.size(); j++) {
                    SootMethod m2 = sootMethods.get(j);
                    if(m2.getName().equals(m.getName()) && m2.getReturnType() == Scene.v().getType(changeTo.getName())) {
                        // this is not allowed in Java but saw this case in android.jar
                        // remove m from class and continue
                        flag = true;
                        break;
                    }
                }
                if (flag) {
                    c.removeMethod(m);
                    continue;
                }
            }
            boolean changed2 = changeClassNamesInMethod(m, changeFrom, changeTo);
            // do not inline change2 since Java do short circuit evaluation
            // we still want to make sure type references in each method body is updated correctly
            changed = changed || changed2;
        }

        return changed;
    }

    //Supporting method for changeClassNameInClass
    private static boolean changeClassNamesInMethod(SootMethod m, SootClass changeFrom, SootClass changeTo) {
        boolean changed = false;
        if (m.getReturnType() == Scene.v().getType(changeFrom.getName())) {
            m.setReturnType(Scene.v().getType(changeTo.getName()));
            changed = true;
        }
        List<Type> types = m.getParameterTypes();
        ArrayList<Type> newTypes = new ArrayList<Type>();
        boolean changeTypes = false;
        for (int i = 0; i < m.getParameterCount(); ++i) {
            if (types.get(i) ==  Scene.v().getType(changeFrom.getName())) {
                newTypes.add(Scene.v().getType(changeTo.getName()));
                changeTypes = true;
            } else {
                newTypes.add(types.get(i));
            }
        }
        if (changeTypes) {
            m.setParameterTypes(newTypes);
            changed = true;
        }

        boolean changeExceptions = false;
        ArrayList<SootClass> newExceptions = new ArrayList<SootClass>();
        for (SootClass e: m.getExceptions()) {
            if (e.getName().equals(changeFrom.getName())) {
                newExceptions.add(changeTo);
                changeExceptions = true;
            } else {
                newExceptions.add(e);
            }
        }
        if (changeExceptions) {
            m.setExceptions(newExceptions);
            changed = true;
        }

        if (!m.isAbstract() && !m.isNative()) {
            Body b = m.retrieveActiveBody();
            for (Local l : b.getLocals()) {
                if (l.getType() == Scene.v().getType(changeFrom.getName())) {
                    l.setType(Scene.v().getType(changeTo.getName()));
                    changed = true;
                }
            }
            for (Unit u : m.retrieveActiveBody().getUnits()) {
                if (u instanceof InvokeStmt) {
                    InvokeExpr expr = ((InvokeStmt) u).getInvokeExpr();
                    SootMethodRef originalMethodRef = expr.getMethodRef();
                    if (originalMethodRef.declaringClass().getName().equals(changeFrom.getName())) {
                        expr.setMethodRef(Scene.v().makeMethodRef(changeTo, originalMethodRef.name(),
                                originalMethodRef.parameterTypes(),
                                originalMethodRef.returnType(),
                                originalMethodRef.isStatic()));
                        ((InvokeStmt) u).setInvokeExpr(expr);
                        changed = true;
                        continue;
                    }
                } else if (u instanceof DefinitionStmt) {
                    Value rightOp = ((DefinitionStmt) u).getRightOp();

                    if (rightOp instanceof NewExpr && rightOp.getType() == Scene.v().getType(changeFrom.getName())) {
                        ((NewExpr) rightOp).setBaseType((RefType) Scene.v().getType(changeTo.getName()));
                        changed = true;
                    } else if (rightOp instanceof JCastExpr) {
                        JCastExpr expr = (JCastExpr) rightOp;
                        if (expr.getType() == Scene.v().getType(changeFrom.getName())) {
                            expr.setCastType(Scene.v().getType(changeTo.getName()));
                            changed = true;
                        }
                    } else if (rightOp instanceof InvokeExpr) {
                        InvokeExpr expr = (InvokeExpr) rightOp;
                        SootMethodRef originalMethodRef = expr.getMethodRef();
                        if (originalMethodRef.declaringClass().getName().equals(changeFrom.getName())) {
                            expr.setMethodRef(Scene.v().makeMethodRef(changeTo, originalMethodRef.name(),
                                    originalMethodRef.parameterTypes(),
                                    originalMethodRef.returnType(),
                                    originalMethodRef.isStatic()));
                            changed = true;
                        }
                    } else if (rightOp instanceof JInstanceOfExpr) {
                        JInstanceOfExpr expr = (JInstanceOfExpr) rightOp;
                        if(expr.getCheckType() == Scene.v().getType(changeFrom.getName())) {
                            expr.setCheckType(Scene.v().getType(changeTo.getName()));
                            changed = true;
                        }
                    }

                    // handle field references
                    if(u instanceof JIdentityStmt) {
                        JIdentityStmt stmt = (JIdentityStmt) u;
                        if(rightOp instanceof ParameterRef && rightOp.getType() == Scene.v().getType(changeFrom.getName())) {
                            ParameterRef oldRef = (ParameterRef) rightOp;
                            ParameterRef newRef = new ParameterRef(Scene.v().getType(changeTo.getName()), oldRef.getIndex());
                            stmt.setRightOp(newRef);
                            changed = true;
                            continue;
                        } else if (rightOp instanceof ThisRef && rightOp.getType() == Scene.v().getType(changeFrom.getName())) {
                            ThisRef newRef = new ThisRef(RefType.v(changeTo.getName()));
                            stmt.setRightOp(newRef);
                            changed = true;
                            continue;
                        }
                    }  else if (u instanceof JAssignStmt) {
                        JAssignStmt stmt = (JAssignStmt) u;

                        if (stmt.containsFieldRef()) {
                            FieldRef fr = stmt.getFieldRef();
                            if (fr instanceof InstanceFieldRef || fr instanceof StaticFieldRef) {
                                if (fr.getType().toString().equals(changeFrom.getName())) {
                                    // the referenced field is in the type of a removed class/interface
                                    SootFieldRef oldFieldRef = fr.getFieldRef();
                                    AbstractSootFieldRef newFieldRef =
                                            new AbstractSootFieldRef(oldFieldRef.declaringClass(), oldFieldRef.name(),
                                                    Scene.v().getType(changeTo.getName()), oldFieldRef.isStatic());
                                    fr.setFieldRef(newFieldRef);
                                    changed = true;
                                    continue;
                                } else if (fr.getFieldRef().declaringClass().getName().equals(changeFrom.getName())) {
                                    // use a field from the collapsed class
                                    // reset the declaring class of this field reference to the collapse-to class
                                    SootFieldRef oldFieldRef = fr.getFieldRef();
                                    AbstractSootFieldRef newFieldRef =
                                            new AbstractSootFieldRef(changeTo, oldFieldRef.name(),
                                                    oldFieldRef.type(), oldFieldRef.isStatic());
                                    fr.setFieldRef(newFieldRef);
                                    changed = true;
                                    continue;
                                }
                            }
                        }
                    }
                }
            }
        }
        return changed;
    }

    public ClassCollapserData getClassCollapserData(){
        return new ClassCollapserData(this.removedMethods, this.classesToRemove, this.classesToRewrite);
    }
}
