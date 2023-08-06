package org.zlab.dinv.runtimechecker;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class Utils {

    public static final String getFirstItemPrefix = "org.zlab.dinv.runtimechecker.Runtime.getFirstItem(";
    public static final String getFirstItemOldPrefix = "org.zlab.dinv.runtimechecker.Runtime.getFirstItem(\\old(";
    public static final String getLastItemPrefix = "org.zlab.dinv.runtimechecker.Runtime.getLastItem(";
    public static final String getLastItemOldPrefix = "org.zlab.dinv.runtimechecker.Runtime.getLastItem(\\old(";

    public static final String leftMatcher = "this.left_";
    public static final String rightMatcher = "this.right_";
    public static final String staticLeftMatcher = "left_";
    public static final String staticRightMatcher = "right_";

    public static String getMethodName(String methodSig) {
        // input: DataStructures.StackArTester.createItem(int)
        // output: createItem
        int pos1 = methodSig.indexOf("(");
        if (pos1 == -1) {
            throw new RuntimeException("method Sig " + methodSig + " does not contain (");
        }
        String methodSigNoParam = methodSig.substring(0, pos1);
        int pos2 = methodSigNoParam.lastIndexOf(".");
        if (pos2 == -1) {
            throw new RuntimeException("method Sig " + methodSigNoParam + " does not contain dot");
        }
        return methodSigNoParam.substring(pos2 + 1);
    }

    public static String getMethodSigWithoutParam(String methodSig) {
        // input: DataStructures.StackArTester.createItem(int)
        // output: DataStructures.StackArTester.createItem

        int pos1 = methodSig.indexOf("(");
        if (pos1 == -1) {
            throw new RuntimeException("method Sig " + methodSig + " does not contain (");
        }
        return methodSig.substring(0, pos1);
    }

    public static String[] getParamTypes(String methodSig) {
        // input: DataStructures.StackArTester.createItem(int)
        // output: DataStructures.StackArTester.createItem

        int pos1 = methodSig.indexOf("(");
        if (pos1 == -1) {
            throw new RuntimeException("method Sig " + methodSig + " does not contain (");
        }
        String typeStr = methodSig.substring(pos1 + 1, methodSig.length()-1);

        typeStr = typeStr.replaceAll("\\s", "");

        if (typeStr.isEmpty()) {
            return new String[0];
        }

        return typeStr.split(",");
    }

    public static String constructIfCondition(String stmt) {
        int invId = InstrumentInvariant.curInvId++;

        // transform fake sizeXXX related variables
        if (stmt.contains("_daikonReflectMethod")) {
            // replace this with ()
            stmt = stmt.replaceAll("_daikonReflectMethod", "()");
        }
        if (stmt.contains("$")) {
            // replace this with ()
            stmt = stmt.replaceAll("\\$", ".");
        }
        //        return "if (!(" + stmt + "))" + "{System.out.println(\"broken inv!\"); }";

        String condCheck = "if (!(" + stmt + "))" + String.format("{org.zlab.dinv.runtimechecker.Runtime.addViolation(%s); }", invId);
        return wrapWithTryCatch(condCheck);
    }

    public static String wrapWithTryCatch(String tryStmt) {
        return String.format("try {%s} catch (Exception e) {}", tryStmt);
    }

    public static String wrapWithTryCatch(String tryStmt, String exceptionStmt) {
        return String.format("try {%s} catch (Exception e) {%s}", tryStmt, exceptionStmt);
    }


    public static String constructExitIfCondition(String stmt, String exitPoint) {
        int invId = InstrumentInvariant.curInvId++;

        // transform fake sizeXXX related variables
        if (stmt.contains("_daikonReflectMethod")) {
            // replace this with ()
            stmt = stmt.replaceAll("_daikonReflectMethod", "()");
        }
        if (stmt.contains("$")) {
            // replace this with ()
            stmt = stmt.replaceAll("\\$", ".");
        }
        //        return "if (!(" + stmt + "))" + "{System.out.println(\"broken inv!\"); }";

        String condCheck = String.format("if ( %s && !(" + stmt + ")) {org.zlab.dinv.runtimechecker.Runtime.addViolation(%s); }", exitPoint, invId);
        return String.format("try {%s} catch (Exception e) {}", condCheck);

    }

    public static boolean isCollectionComparison(String inv) {
        // org.zlab.dinv.runtimechecker.Runtime.getFirstItem(this.partitionKeyColumns) == org.zlab.dinv.runtimechecker.Runtime.getFirstItem(\old(this.partitionKeyColumns))

        if (!inv.contains("=="))
            return false;

        // remove spaces
        inv = inv.replaceAll("\\s+", "");

        // it can only have one \old
        String[] substrings = inv.split("\\\\old");
        int count = substrings.length - 1;
        if (count > 1)
            return false;

        String[] vars = inv.split("==");
        if (vars.length != 2)
            return false;

        if (vars[0].startsWith(getFirstItemPrefix) && vars[1].startsWith(getFirstItemOldPrefix)) {
            // get the target string
            String left = vars[0].substring(getFirstItemPrefix.length(), vars[0].length()-1);
            String right = vars[1].substring(getFirstItemOldPrefix.length(), vars[1].length()-2);
            return left.equals(right);
        } else if (vars[0].startsWith(getLastItemPrefix) && vars[1].startsWith(getLastItemOldPrefix)) {
            String left = vars[0].substring(getLastItemPrefix.length(), vars[0].length()-1);
            String right = vars[1].substring(getLastItemOldPrefix.length(), vars[1].length()-2);
            return left.equals(right);
        }
        return false;
    }

    public static Map.Entry<String, CollectionCompareType> getCollectionCompareEntry(String inv) {
        inv = inv.replaceAll("\\s+", "");
        String[] vars = inv.split("==");
        assert vars.length == 2;
        if (vars[0].startsWith("org.zlab.dinv.runtimechecker.Runtime.getFirstItem(")) {
            return new AbstractMap.SimpleEntry<>(vars[0].substring(50, vars[0].length()-1), CollectionCompareType.first);
        } else {
            return new AbstractMap.SimpleEntry<>(vars[0].substring(49, vars[0].length()-1), CollectionCompareType.last);
        }
    }


    public static boolean excludeInv(String inv) {
        // filter out some invariants that cannot be embedded now
        // TODO: Handle comparison between pre-state and post-state
        if (inv.contains("orig") || inv.contains("\\old") || inv.contains("\\new")) {
            return true;
        }
        // TODO: Handle return val
        if (inv.contains("return") || inv.contains("\\result")) {
            return true;
        }
        // TODO: Handle daikon.Quant.xxx, redirect them to our rt
        if (inv.contains("daikon.Quant")) {
            return true;
        }
        // Exclude JML implications
        if (inv.contains("==>")) {
            return true;
        }
        // Exclude
        if (inv.contains("assertionsDisabled")) {
            return true;
        }
        if (inv.startsWith("assignable ")) {
            return true;
        }
        if (inv.contains(".toString()")) {
            return true;
        }
        if (inv.contains(".getName()")) {
            return true;
        }
        if (inv.contains("org.zlab.dinv.runtimechecker.Runtime.getFirstItem")) {
            return true;
        }
        if (inv.contains("org.zlab.dinv.runtimechecker.Runtime.getLastItem")) {
            return true;
        }
        if (inv.contains("Exiting Daikon.")) {
            return true;
        }
        if (!compareCorrectVar(inv))
            return true;
        return false;
    }

    /**
     * Make sure we are comparing correct pair of variables
     * this.left_25 with this.right_25
     */
    public static boolean compareCorrectVar(String inv) {
        if (inv.contains(leftMatcher) || inv.contains(rightMatcher)) {
            return innerCompareCorrectVar(inv, leftMatcher, rightMatcher);
        } else if (inv.contains(staticLeftMatcher) || inv.contains(staticRightMatcher)) {
            return innerCompareCorrectVar(inv, staticLeftMatcher, staticRightMatcher);
        } else
            return true;
    }

    public static boolean innerCompareCorrectVar(String inv, String lMatch, String rMatch) {
        String[] items = inv.split(" ");
        Set<String> numberSet = new HashSet<>();
        for (String item : items) {
            if (item.startsWith(lMatch)) {
                numberSet.add(item.substring(lMatch.length()));
            }
            if (item.startsWith(rMatch)) {
                numberSet.add(item.substring(rMatch.length()));
            }
        }
        // if (!(numberSet.size() <= 1))
        //     System.out.println("excluded inv: " + inv);
        return numberSet.size() <= 1;
    }

    public static List<String> constructInvStmt(List<String> invs) {
        List<String> ret = new LinkedList<>();
        for (String inv: invs) {
            // special handle pre_post inv
            if (excludeInv(inv))
                continue;
            // All the generated invs will be added
            ret.add(constructIfCondition(inv));
        }
        return ret;
    }

    public static List<String> constructExitInvStmt(List<String> invs, String exitPoint) {
        List<String> ret = new LinkedList<>();
        for (String inv: invs) {
            // special handle pre_post inv
            if (excludeInv(inv))
                continue;
            // All the generated invs will be added
            ret.add(constructExitIfCondition(inv, exitPoint));
        }
        return ret;
    }

    public enum CollectionCompareType {
        first, last
    }

    /**
     * Special handle firstitem(coll) == firstitem(\old(coll))
     */
    public static Map<String, Set<CollectionCompareType>> constructCondCompareInvStmt(List<String> invs, String exitPoint) {
        Map<String, Set<CollectionCompareType>> ret = new HashMap<>();
        for (String inv: invs) {
            // special handle pre_post inv
            if (!isCollectionComparison(inv))
                continue;
            // All the generated invs will be added
            Map.Entry<String, CollectionCompareType> entry = getCollectionCompareEntry(inv);
            if (!ret.containsKey(entry.getKey())) {
                ret.put(entry.getKey(), new HashSet<>());
            }
            ret.get(entry.getKey()).add(entry.getValue());
        }
        return ret;
    }

    public static boolean isMainMethod(MethodDeclaration methodDeclaration) {
        // Check the method name
        if (!methodDeclaration.getNameAsString().equals("main")) {
            return false;
        }

        // Check the modifiers
        if (!methodDeclaration.isPublic() || !methodDeclaration.isStatic()) {
            return false;
        }

        // Check the return type
        if (!methodDeclaration.getTypeAsString().equals("void")) {
            return false;
        }

        // Check the parameters
        List<Parameter> parameters = methodDeclaration.getParameters();
        if (parameters.size() != 1 || !parameters.get(0).getTypeAsString().equals("String[]")) {
            return false;
        }

        return true;
    }

    public static boolean isMatchPpt(ClassOrInterfaceDeclaration classDecl, MethodDeclaration methodDecl, String pptMethodSig) {

        StringBuilder signatureBuilder = new StringBuilder();

        if (classDecl.getFullyQualifiedName().isPresent())
            signatureBuilder.append(classDecl.getFullyQualifiedName().get());
        else
            throw new RuntimeException("class " + classDecl.getName() + " do not have full name");
        // append method name
        signatureBuilder.append(".").append(methodDecl.getNameAsString());

        String methodSigWithoutParam = signatureBuilder.toString();

        // ppt parse
        // parse class full name without parameter
        String pptMethodSigWithoutParam = Utils.getMethodSigWithoutParam(pptMethodSig);
        // javaparser represents inner class with . while ppt represents inner class with $
        // switch it!
        pptMethodSigWithoutParam = pptMethodSigWithoutParam.replace("$", ".");

        String[] pptParamTypes = Utils.getParamTypes(pptMethodSig);

        // (1) Compare method sig without param
        if (!methodSigWithoutParam.equals(pptMethodSigWithoutParam))
            return false;

        // (2) Compare param num (only coarse comparison)
        NodeList<Parameter> parameters = methodDecl.getParameters();
        if (parameters.size() != pptParamTypes.length)
            return false;

        // (3) Compare param type (only coarse comparison)
        for (int i = 0; i < parameters.size(); i++) {
            Type type = parameters.get(i).getType();
            if (type instanceof ClassOrInterfaceType) {
                ClassOrInterfaceType classOrInterfaceType = (ClassOrInterfaceType) type;
                if (!pptParamTypes[i].contains(classOrInterfaceType.getNameAsString()))
                    return false;
            }
        }
        return true;
    }

    public static boolean isMatchPpt2(ClassOrInterfaceDeclaration classDecl, String pptMethodSig) {
        // pptMethodSig contains class def
        StringBuilder signatureBuilder = new StringBuilder();

        if (classDecl.getFullyQualifiedName().isPresent())
            signatureBuilder.append(classDecl.getFullyQualifiedName().get());
        else
            throw new RuntimeException("class " + classDecl.getName() + " do not have full name");

        String pptMethodSigWithoutDollar = pptMethodSig.replace("$", ".");

        return signatureBuilder.toString().equals(pptMethodSigWithoutDollar);
    }

    public static List<String> readFile(Path path) {
        List<String> lines = new LinkedList<>();
        try(BufferedReader br = new BufferedReader(new FileReader(path.toString()))) {
            String line = br.readLine();
            while (line != null) {
                lines.add(line);
                line = br.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return lines;
    }

    public static String replaceDollarWithDot(String str) {
        return str.replaceAll("\\$", ".");
    }

    public static void mergeInv(Map<String, Set<String>> dest, Map<String, Set<String>> src) {
        for (String ppt: src.keySet()) {
            if (!dest.containsKey(ppt)) {
                dest.put(ppt, src.get(ppt));
            } else {
                dest.get(ppt).addAll(src.get(ppt));
            }
        }
    }

    public static void injectTmpVariable(String paramName, int collTmpCount, BlockStmt body, boolean first, String exitPoint) {
        String type = first? "first": "last";
        String funcName = first? "getFirstItem": "getLastItem";

        String tmpVarName_pre = String.format("tmp_pre_%s_%d", type, collTmpCount);
        String initStmt_pre = String.format("int %s = Integer.MIN_VALUE;", tmpVarName_pre);
        String try_stmt_pre = String.format("try {%s = org.zlab.dinv.runtimechecker.Runtime.%s(%s);} catch (Exception e) {}", tmpVarName_pre, funcName, paramName);
        body.addStatement(collTmpCount, StaticJavaParser.parseStatement(try_stmt_pre));
        body.addStatement(collTmpCount, StaticJavaParser.parseStatement(initStmt_pre));

        String tmpVarName_post = String.format("tmp_post_%s_%d", type, collTmpCount);
        String initStmt_post = String.format("int %s = Integer.MIN_VALUE;", tmpVarName_post);
        String try_stmt_post = String.format("try {%s = org.zlab.dinv.runtimechecker.Runtime.%s(%s);} catch (Exception e) {}", tmpVarName_post, funcName, paramName);
        body.addStatement(StaticJavaParser.parseStatement(initStmt_post));
        body.addStatement(StaticJavaParser.parseStatement(try_stmt_post));

        // comparison!
        String collCompInv = String.format("%s == %s", tmpVarName_pre, tmpVarName_post);
        body.addStatement(Utils.constructExitIfCondition(collCompInv, exitPoint));
    }

    public static void injectExitPointMonitorVariables(ClassOrInterfaceDeclaration classDecl, Set<String > exitPointMonitorVariables, boolean isStatic) {
        for (String fieldName: exitPointMonitorVariables) {
            FieldDeclaration field = new FieldDeclaration();
            field.addModifier(Modifier.Keyword.PRIVATE); // Add the 'private' modifier
            field.setStatic(isStatic);

            NodeList<VariableDeclarator> vars = new NodeList<>();

            VariableDeclarator variable = new VariableDeclarator();
            variable.setType(PrimitiveType.booleanType());
            variable.setName(fieldName);
            variable.setInitializer(new BooleanLiteralExpr(false)); // setting the default value to false

            vars.add(variable);
            field.setVariables(vars); // Set the type to int and the variable name to 'a'

            // Add the new field to the class declaration
            classDecl.getMembers().add(field);
        }
    }

    public static Set<Integer> extractExitPointLineSet(Set<String> exitPoints) {
        Set<Integer> lineSet = new HashSet<>();
        for (String exitPoint: exitPoints) {
            if (exitPoint.equals("EXIT")) {
                lineSet.add(-1);
            } else {
                String number = exitPoint.substring(4);
                lineSet.add(Integer.parseInt(number));
            }
        }
        return lineSet;
    }
}
