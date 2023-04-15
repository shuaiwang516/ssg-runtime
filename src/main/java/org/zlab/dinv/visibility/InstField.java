package org.zlab.dinv.visibility;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;

import java.util.Map;
import java.util.Set;

public class InstField {

    public static void process(CompilationUnit cu, Map<String, Map<String, Set<Integer>>> targetIfBranches) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            if (!classDecl.getFullyQualifiedName().isPresent()) {
                return;
            }
            String clazzFullName = classDecl.getFullyQualifiedName().get();
            classDecl.getMethods().forEach(methodDecl -> {

                // check whether we have any target if branch in current method, otherwise return

                // iterate all the if branches
                BlockStmt body = methodDecl.getBody().orElse(null);
                if (body != null) {
                    processBlockStmt(body);
                }
            });
        });
    }

    public static void recurProcess(Statement stmt, NodeList<Statement> newStatements) {
        if (stmt instanceof IfStmt) {
            // decide whether this is the target if branch

            Statement newStmt = StaticJavaParser.parseStatement("System.out.println(\"This statement was added before an if branch\");");
            newStatements.add(newStatements.indexOf(stmt), newStmt);
        }

        if (stmt instanceof IfStmt) {
            Statement thenStmt = ((IfStmt) stmt).getThenStmt();

            if (thenStmt instanceof BlockStmt) {
                processBlockStmt((BlockStmt) thenStmt);
            }
            Statement elseStmt = ((IfStmt) stmt).getElseStmt().orElse(null);
            if (elseStmt != null) {
                processBlockStmt((BlockStmt) elseStmt);
            }
        } else if (stmt instanceof BlockStmt) {
            processBlockStmt((BlockStmt) stmt);
        }  else if (stmt instanceof WhileStmt) {
            Statement body = ((WhileStmt) stmt).getBody();
            if (body instanceof BlockStmt) {
                processBlockStmt((BlockStmt) body);
            }
        }
        // TODOs: add more types
    }

    public static void processBlockStmt(BlockStmt blockStmt) {
        NodeList<Statement> statements = blockStmt.getStatements();
        NodeList<Statement> newStatements = new NodeList<>(statements);
        for (Statement innerStmt : statements) {
            recurProcess(innerStmt, newStatements);
        }
        blockStmt.setStatements(newStatements);
    }
}
