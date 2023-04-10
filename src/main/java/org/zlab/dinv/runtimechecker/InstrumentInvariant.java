package org.zlab.dinv.runtimechecker;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
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

    public static class testClassVisitor extends VoidVisitorAdapter<Void> {

        @Override
        public void visit(MethodDeclaration methodDeclaration, Void arg) {
            // add a print for all methods

            if (methodDeclaration.isAbstract() || methodDeclaration.isNative())
                return;

            if (methodDeclaration.getParentNode().isPresent()) {
                Object parentNode = methodDeclaration.getParentNode().get();
                if (parentNode instanceof ClassOrInterfaceDeclaration) {
                    ClassOrInterfaceDeclaration classDeclaration = (ClassOrInterfaceDeclaration) parentNode;
                    if (classDeclaration.isInterface())
                        return;
                }
            }

            BlockStmt methodBody = methodDeclaration.getBody().orElse(new BlockStmt());
            NodeList<Statement> stmts = methodBody.getStatements();

            stmts.add(0, StaticJavaParser.parseStatement("System.out.println(\"[hklog] hello\");"));

            methodBody.setStatements(stmts);
            methodDeclaration.setBody(methodBody);
        }
    }


    public static class InstClassVisitor extends VoidVisitorAdapter<Void> {

        public Map<String, List<String>> invs;

        public InstClassVisitor(Map<String, List<String>> invs) {
            this.invs = invs;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration classDecl, Void arg) {
            super.visit(classDecl, arg);

            // perform instrumentation
            System.out.println("className = " + classDecl.getName());
            classDecl.findAll(MethodDeclaration.class).forEach(method -> {

                // if it's a main function
                if (Utils.isMainMethod(method)) {
                    if (method.getBody().isPresent()) {
                        method.getBody().get().addStatement(0, StaticJavaParser.parseStatement("try {" +
                                "Class.forName(\"org.zlab.dinv.runtimechecker.Runtime\");" +
                                "}" +
                                "catch (ClassNotFoundException e)" +
                                "{throw new RuntimeException(e);}"));
                    }
                }

                // TODO: Only add this at main functions

                if (method.getNameAsString().startsWith("internal$")) {
                    return;
                }

                LinkedList<String> enterInvs = new LinkedList<>();
                LinkedList<String> exitInvs = new LinkedList<>();

                for (String ppt: invs.keySet()) {
                    // match the ppt to the method
                    String[] strs = ppt.split(":::");
                    if (strs.length != 2)
                        continue;
                    String pptMethodSig = strs[0];
                    String posStatus = strs[1];

                    // FIXME: handle object
                    if (posStatus.equals("OBJECT") || posStatus.equals("CLASS"))
                        continue;

                    String methodName = Utils.getMethodName(pptMethodSig);

                    try {
                        if (!Utils.isMatchPpt(classDecl, method, pptMethodSig)) {
                            continue;
                        }
                    } catch (RuntimeException e) {
                        System.out.println("skip class " + classDecl.getName() + " because of " + e);
                        return;
                    }


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

                if (enterInvsBlocks.isEmpty() && exitInvsBlocks.isEmpty())
                    return;

                // Create a new method with the wrapped name
                MethodDeclaration wrappedMethod = method.clone();
                method.setName("internal$" + method.getNameAsString());

                // remove @Override from the method being wrapped
                method.getAnnotations().removeIf(a -> a.getName().asString().equals("Override"));

                // Create a new method body for wrappedMethod
                BlockStmt body = new BlockStmt();

                // Enter env
                for (String enterInvsBlock: enterInvsBlocks) {
                    try {
                        body.addStatement(enterInvsBlock);
                    } catch (Exception e) {
                        // FIXME: if (!size != size(DataStructures.StackArTester.s.theArray[])-1){System.out.println("broken inv!"); }
                        System.out.println("enter add statement exception + " + e);
                    }
                }

//                body.addStatement("System.out.println(\"Before calling " + wrappedMethod.getNameAsString() + "()\");");

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

//                body.addStatement("System.out.println(\"After calling " + wrappedMethod.getNameAsString() + "()\");");

                // Enter env
                for (String exitInvsBlock: exitInvsBlocks) {
                    try {
                        body.addStatement(exitInvsBlock);
                    } catch (Exception e) {
                        // FIXME: if (!size != size(DataStructures.StackArTester.s.theArray[])-1){System.out.println("broken inv!"); }
                        System.out.println("exit add statement exception + " + e);
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


    }

    public static void testCassandra() throws IOException {
        Path targetInv = Paths.get("input/cassandra_inv");


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

    public static void testExample() throws IOException {
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
