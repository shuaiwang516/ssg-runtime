package org.zlab.dinv.diffconfig;

import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.stmt.*;
import org.zlab.dinv.visibility.IterateAST;

import java.util.*;

public class RetrieveHandlerBlock extends IterateAST {
    /**
     * Input: program locations -> output: string
     */
    private final Map<String, Set<Integer>> programLocations;
    private final Map<String, Map<Integer, String>> programLocation2block = new HashMap<>();

    private String currentClassFullName;
    private String currentMethodName;

    RetrieveHandlerBlock(Map<String, Set<Integer>> programLocations) {
        this.programLocations = programLocations;
    }

    Map<String, Map<Integer, String>> getProgramLocation2block() {
        return programLocation2block;
    }

    public void process(CompilationUnit cu) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {

            if (!classDecl.getFullyQualifiedName().isPresent()) {
                return;
            }
            String clazzFullName = classDecl.getFullyQualifiedName().get();
            if (!programLocations.containsKey(clazzFullName))
                return;
            Set<Integer> lineSet = programLocations.get(clazzFullName);
            currentClassFullName = clazzFullName;

            classDecl.getMethods().forEach(methodDecl -> {
                currentMethodName = methodDecl.getNameAsString();
                methodDecl.getBody().ifPresent(body -> processBlockStmt(body, lineSet));
            });

        });
    }

    public void recurProcess(Statement stmt, NodeList<Statement> newStatements,
            Set<Integer> lineSet) {
        if (stmt.getRange().isPresent()) {
            Range range = stmt.getRange().get();
            int begin = range.begin.line;

            if (lineSet.contains(begin)) {
                if (stmt instanceof IfStmt) {
                    // capture the branch being used, also connect the else stmt?
                    // lets check the only check the if stmt for now
                    Statement thenStmt = ((IfStmt) stmt).getThenStmt();
                    String blockString = thenStmt.toString();
                    // add the then Stmt
                    if (!programLocation2block.containsKey(currentClassFullName)) {
                        programLocation2block.put(currentClassFullName, new HashMap<>());
                    }
                    if (!programLocation2block.get(currentClassFullName).containsKey(begin)) {
                        programLocation2block.get(currentClassFullName).put(begin, blockString);
                    }
                }
                // This is an if branch, capture the current block!
                // retrieve the if block
            }
        }
        iterateStmt(stmt, lineSet);
    }
}
