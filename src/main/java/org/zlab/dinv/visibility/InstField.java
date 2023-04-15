package org.zlab.dinv.visibility;

import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InstField {

    private final Map<String, Map<String, Set<Integer>>> targetIfBranches;

    InstField(Map<String, Map<String, Set<Integer>>> targetIfBranches) {
        this.targetIfBranches = targetIfBranches;
    }

    public void process(CompilationUnit cu) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            if (!classDecl.getFullyQualifiedName().isPresent()) {
                return;
            }
            String clazzFullName = classDecl.getFullyQualifiedName().get();
            if (!targetIfBranches.containsKey(clazzFullName))
                return;
            Map<String, Set<Integer>> method2lineNumber = targetIfBranches.get(clazzFullName);
            classDecl.getMethods().forEach(methodDecl -> {

                Set<Integer> lineSet = new HashSet<>();
                for (Map.Entry<String, Set<Integer>> entry: method2lineNumber.entrySet()) {
                    lineSet.addAll(entry.getValue());
                }

                // iterate all the if branches
                methodDecl.getBody().ifPresent(body -> processBlockStmt(body, lineSet));
            });
        });
    }

    public void recurProcess(Statement stmt, NodeList<Statement> newStatements, Set<Integer> lineSet) {
        if (stmt instanceof IfStmt) {
            // decide whether this is the target if branch
            // line id
            if (stmt.getRange().isPresent()) {
                Range range = stmt.getRange().get();
                int begin = range.begin.line;
                if (lineSet.contains(begin)) {
                    Statement newStmt = StaticJavaParser.parseStatement("System.out.println(\"This statement was added before an if branch\");");
                    newStatements.add(newStatements.indexOf(stmt), newStmt);
                }
            }
        }

        if (stmt instanceof IfStmt) {
            Statement thenStmt = ((IfStmt) stmt).getThenStmt();

            if (thenStmt instanceof BlockStmt) {
                processBlockStmt((BlockStmt) thenStmt, lineSet);
            }
            ((IfStmt) stmt).getElseStmt().ifPresent(elseStmt -> processBlockStmt((BlockStmt) elseStmt, lineSet));
        } else if (stmt instanceof BlockStmt) {
            processBlockStmt((BlockStmt) stmt, lineSet);
        }  else if (stmt instanceof WhileStmt) {
            Statement body = ((WhileStmt) stmt).getBody();
            if (body instanceof BlockStmt) {
                processBlockStmt((BlockStmt) body, lineSet);
            }
        }
        // TODOs: add more types
    }

    public void processBlockStmt(BlockStmt blockStmt, Set<Integer> lineSet) {
        NodeList<Statement> statements = blockStmt.getStatements();
        NodeList<Statement> newStatements = new NodeList<>(statements);
        for (Statement innerStmt : statements) {
            recurProcess(innerStmt, newStatements, lineSet);
        }
        blockStmt.setStatements(newStatements);
    }
}
