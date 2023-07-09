package org.zlab.dinv.visibility;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.stmt.*;

import java.util.Set;

public abstract class IterateAST {

    public abstract void recurProcess(Statement stmt, NodeList<Statement> newStatements, Set<Integer> lineSet);

    public void processBlockStmt(BlockStmt blockStmt, Set<Integer> lineSet) {
        NodeList<Statement> statements = blockStmt.getStatements();
        NodeList<Statement> newStatements = new NodeList<>(statements);
        for (Statement innerStmt : statements) {
            recurProcess(innerStmt, newStatements, lineSet);
        }
        blockStmt.setStatements(newStatements);
    }

    public void iterateStmt(Statement stmt, Set<Integer> lineSet) {
        if (stmt instanceof IfStmt) {
            Statement iterateStmt = stmt;
            while (true) {
                // iterate all if-elseif-elseif-elseblock
                Statement thenStmt = ((IfStmt) iterateStmt).getThenStmt();
                if (thenStmt instanceof BlockStmt) {
                    processBlockStmt((BlockStmt) thenStmt, lineSet);
                }
                if (((IfStmt) iterateStmt).getElseStmt().isPresent()) {
                    Statement elseStmt =  ((IfStmt) iterateStmt).getElseStmt().get();
                    if (elseStmt instanceof IfStmt) {
                        iterateStmt = elseStmt;
                    } else {
                        // this is a block o null
                        if (elseStmt instanceof BlockStmt)
                            processBlockStmt((BlockStmt) elseStmt, lineSet);
                        break;
                    }
                } else {
                    break;
                }
            }
        } else if (stmt instanceof BlockStmt) {
            processBlockStmt((BlockStmt) stmt, lineSet);
        }  else if (stmt instanceof WhileStmt) {
            Statement body = ((WhileStmt) stmt).getBody();
            if (body instanceof BlockStmt) {
                processBlockStmt((BlockStmt) body, lineSet);
            }
        } else if (stmt instanceof TryStmt) {
            BlockStmt blockStmt = ((TryStmt) stmt).getTryBlock();
            processBlockStmt(blockStmt, lineSet);
            ((TryStmt) stmt).getFinallyBlock().ifPresent(b -> processBlockStmt(b, lineSet));
        }
        // TODO: add more types
    }
}
