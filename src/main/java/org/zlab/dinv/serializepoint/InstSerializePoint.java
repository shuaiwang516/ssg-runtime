package org.zlab.dinv.serializepoint;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.stmt.Statement;
import org.zlab.dinv.visibility.IterateAST;

import java.util.Set;

public class InstSerializePoint extends IterateAST {
    public Set<SerializePoint> serializePoints;

    public InstSerializePoint(Set<SerializePoint> serializePoints) {
        this.serializePoints = serializePoints;
    }

    public void process(CompilationUnit cu) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            if (!classDecl.getFullyQualifiedName().isPresent()) {
                return;
            }
            String clazzFullName = classDecl.getFullyQualifiedName().get();

            // if (!programLocations.containsKey(clazzFullName))
            // return;
            // Set<Integer> lineSet = programLocations.get(clazzFullName);
            // currentClassFullName = clazzFullName;
            //
            // newStaticFields = new HashSet<>();
            // newNonStaticFields = new HashSet<>();
            //
            // classDecl.getMethods().forEach(methodDecl -> {
            // isStatic = methodDecl.isStatic();
            // currentMethodName = methodDecl.getNameAsString();
            // methodDecl.getBody().ifPresent(body -> processBlockStmt(body, lineSet));
            // });
        });
    }

    @Override
    public void recurProcess(Statement stmt, NodeList<Statement> newStatements,
            Set<Integer> lineSet) {
    }
}
