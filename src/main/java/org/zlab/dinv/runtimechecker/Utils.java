package org.zlab.dinv.runtimechecker;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class Utils {

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

        return String.format("try {%s} catch (Exception e) {}", condCheck);

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
        return false;
    }

    public static List<String> constructInvStmt(List<String> invs) {
        List<String> ret = new LinkedList<>();
        for (String inv: invs) {
            if (excludeInv(inv))
                continue;
            // All the generated invs will be added
            ret.add(constructIfCondition(inv));
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

}
