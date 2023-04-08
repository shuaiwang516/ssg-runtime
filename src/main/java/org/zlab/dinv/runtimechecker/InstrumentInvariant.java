package org.zlab.dinv.runtimechecker;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class InstrumentInvariant {

    public static int curInvId = 0; // this will count a total num of invs

    // traverse AST to inject the invariants

    // TODO: change all fields to public!

    private static class InstClassVisitor extends VoidVisitorAdapter<Void> {

        public Map<String, List<String>> invs;

        public InstClassVisitor(Map<String, List<String>> invs) {
            this.invs = invs;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration classDecl, Void arg) {
            // perform instrumentation
            classDecl.findAll(MethodDeclaration.class).forEach(method -> {

                if (method.getNameAsString().startsWith("internal$")) {
                    return;
                }

                LinkedList<String> enterInvs = new LinkedList<>();
                LinkedList<String> exitInvs = new LinkedList<>();

                for (String ppt: invs.keySet()) {
                    String[] strs = ppt.split(":::");
                    if (strs.length != 2)
                        continue;
                    String methodSig = strs[0];
                    String posStatus = strs[1];

                    // FIXME: handle object
                    if (posStatus.equals("OBJECT") || posStatus.equals("CLASS"))
                        continue;

                    String methodName = Utils.getMethodName(methodSig);

                    if (methodName.equals(method.getNameAsString())) {
                        if (posStatus.equals("ENTER")) {
                            enterInvs.addAll(invs.get(ppt));
                        } else {
                            exitInvs.addAll(invs.get(ppt));
                        }
                    }
                }

                // construct enter inv
                List<String> enterInvsBlocks = Utils.constructInvStmt(enterInvs);
                List<String> exitInvsBlocks = Utils.constructInvStmt(exitInvs);

                // Create a new method with the wrapped name
                MethodDeclaration wrappedMethod = method.clone();
                method.setName("internal$" + method.getNameAsString());

                // Create a new method body for wrappedMethod
                BlockStmt body = new BlockStmt();

                // Enter env
                for (String enterInvsBlock: enterInvsBlocks) {
                    try {
                        body.addStatement(enterInvsBlock);
                    } catch (Exception e) {
                        // FIXME: if (!size != size(DataStructures.StackArTester.s.theArray[])-1){System.out.println("broken inv!"); }
                    }
                }

                body.addStatement("System.out.println(\"Before calling " + wrappedMethod.getNameAsString() + "()\");");

                List<String> paramNames = new LinkedList<>();
                for (Parameter parameter : wrappedMethod.getParameters()) {
                    paramNames.add(parameter.getNameAsString());
                }
                String concatenatedParameter = String.join(", ", paramNames);

                com.github.javaparser.ast.type.Type returnType = method.getType();

                if (!returnType.isVoidType()) {
                    VariableDeclarationExpr variableDeclaration = new VariableDeclarationExpr(
                            returnType,
                            "returnValue");
                    body.addStatement(variableDeclaration);
                    body.addStatement("returnValue = " + method.getNameAsString() + "(" +
                            concatenatedParameter +
                            ");");
                } else {
                    body.addStatement(method.getNameAsString() + "(" +
                            concatenatedParameter +
                            ");");
                }

                body.addStatement("System.out.println(\"After calling " + wrappedMethod.getNameAsString() + "()\");");

                // Enter env
                for (String exitInvsBlock: exitInvsBlocks) {
                    try {
                        body.addStatement(exitInvsBlock);
                    } catch (Exception e) {
                        // FIXME: if (!size != size(DataStructures.StackArTester.s.theArray[])-1){System.out.println("broken inv!"); }
                    }
                }

                if (!returnType.isVoidType()) {
                    body.addStatement("return returnValue;");
                }

                wrappedMethod.setBody(body);
                classDecl.addMember(wrappedMethod);
            });

        }
    }


    public static void main(String[] args) throws IOException {
        // Target file
        Path targetInv = Paths.get("input/inv");
        Path targetFile = Paths.get("input/StackArTester.java");

        // Parse the input class file
        File file = targetFile.toFile();
        CompilationUnit cu = StaticJavaParser.parse(file);

        // test(cu);

        // Use a VoidVisitor to visit all MethodDeclaration nodes
        cu.accept(new InstClassVisitor(LoadInvariant.load(targetInv)), null);

        System.out.println("cu = " + cu.toString());

        FileWriter output = new FileWriter("output/StackArTester.java");
        output.write(cu.toString());
        output.close();
    }

}
