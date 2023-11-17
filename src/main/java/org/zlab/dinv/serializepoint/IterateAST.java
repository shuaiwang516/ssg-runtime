package org.zlab.dinv.serializepoint;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.stmt.*;

import java.util.Map;
import java.util.Set;

public abstract class IterateAST<T> {

    public abstract void recurProcess(Statement stmt, NodeList<Statement> newStatements,
            Map<Integer, Set<T>> line2Points);

    public void processBlockStmt(BlockStmt blockStmt, Map<Integer, Set<T>> line2Points) {
        NodeList<Statement> statements = blockStmt.getStatements();
        NodeList<Statement> newStatements = new NodeList<>(statements);
        for (Statement innerStmt : statements) {
            recurProcess(innerStmt, newStatements, line2Points);
        }
        blockStmt.setStatements(newStatements);
    }

    public BlockStmt processNonBlockStmt(Statement stmt, Map<Integer, Set<T>> line2Points) {
        return null;
    }

    public void iterateStmt(Statement stmt, Map<Integer, Set<T>> line2Points) {
        if (stmt instanceof IfStmt) {
            Statement iterateStmt = stmt;
            while (true) {
                // iterate all if-elseif-elseif-elseblock
                Statement thenStmt = ((IfStmt) iterateStmt).getThenStmt();
                if (thenStmt instanceof BlockStmt) {
                    processBlockStmt((BlockStmt) thenStmt, line2Points);
                } else {
                    // process non-block stmt, if we inject stmt here
                    // we need to create a new block
                    BlockStmt blockStmt = processNonBlockStmt(thenStmt, line2Points);
                    if (blockStmt != null) {
                        ((IfStmt) iterateStmt).setThenStmt(blockStmt);
                    }
                }
                if (((IfStmt) iterateStmt).getElseStmt().isPresent()) {
                    Statement elseStmt = ((IfStmt) iterateStmt).getElseStmt().get();
                    if (elseStmt instanceof IfStmt) {
                        iterateStmt = elseStmt;
                    } else {
                        // this is a block o null
                        if (elseStmt instanceof BlockStmt)
                            processBlockStmt((BlockStmt) elseStmt, line2Points);
                        else {
                            BlockStmt blockStmt = processNonBlockStmt(elseStmt, line2Points);
                            if (blockStmt != null) {
                                ((IfStmt) iterateStmt).setElseStmt(blockStmt);
                            }
                        }
                        break;
                    }
                } else {
                    break;
                }
            }
        } else if (stmt instanceof BlockStmt) {
            processBlockStmt((BlockStmt) stmt, line2Points);
        } else if (stmt instanceof WhileStmt) {
            Statement body = ((WhileStmt) stmt).getBody();
            if (body instanceof BlockStmt) {
                processBlockStmt((BlockStmt) body, line2Points);
            } else {
                BlockStmt blockStmt = processNonBlockStmt(body, line2Points);
                if (blockStmt != null) {
                    ((WhileStmt) stmt).setBody(blockStmt);
                }
            }
        } else if (stmt instanceof TryStmt) {
            BlockStmt blockStmt = ((TryStmt) stmt).getTryBlock();
            processBlockStmt(blockStmt, line2Points);
            ((TryStmt) stmt).getFinallyBlock().ifPresent(b -> processBlockStmt(b, line2Points));
        } else if (stmt instanceof ForStmt) {
            Statement body = ((ForStmt) stmt).getBody();
            if (body instanceof BlockStmt) {
                processBlockStmt((BlockStmt) body, line2Points);
            } else {
                BlockStmt blockStmt = processNonBlockStmt(body, line2Points);
                if (blockStmt != null) {
                    ((ForStmt) stmt).setBody(blockStmt);
                }
            }
        }
        // TODO: add more types
    }
}
