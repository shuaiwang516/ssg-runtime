package org.zlab.dinv.diffconfig;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.List;

public class ConfigVisitor extends VoidVisitorAdapter<SingleClassConfigInfo> {

    @Override
    public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration,
            SingleClassConfigInfo singleClassConfigInfo) {
        // there's no need to iterate the inner static class since it's done outside
        // super.visit(classOrInterfaceDeclaration, singleClassConfigInfo);

        List<FieldDeclaration> fields = classOrInterfaceDeclaration.getFields();
        for (FieldDeclaration field : fields) {
            for (VariableDeclarator variableDeclarator : field.getVariables()) {
                // we might resolve it
                Type type = variableDeclarator.getType();
                String typeString = type.asString();
                try {
                    ResolvedType resolvedType = type.resolve();
                    if (resolvedType.isReferenceType()) {
                        ResolvedReferenceType resolvedReferenceType = resolvedType
                                .asReferenceType();
                        typeString = resolvedReferenceType.getQualifiedName();
                    }
                } catch (UnsolvedSymbolException ignored) {
                }

                singleClassConfigInfo.typeCollector.put(variableDeclarator.getNameAsString(),
                        typeString);

                if (variableDeclarator.getInitializer().isPresent()) {
                    singleClassConfigInfo.initCollector.put(variableDeclarator.getNameAsString(),
                            variableDeclarator.getInitializer().get().toString());
                }
            }
        }
    }
}