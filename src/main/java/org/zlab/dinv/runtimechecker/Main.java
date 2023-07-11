package org.zlab.dinv.runtimechecker;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import picocli.CommandLine;

public class Main {

    public static void main(String[] args) {
        EmbedInvariant embedInvariant = new EmbedInvariant();
        new CommandLine((embedInvariant)).execute(args);
    }

    public static void rewriteFieldsAsPublic(CompilationUnit cu) {
        // Iterate over all non-abstract/interface classes
        cu.findAll(ClassOrInterfaceDeclaration.class, c -> !c.isAbstract() && !c.isInterface())
                .forEach(cls -> {
                    // Iterate over all fields in class
                    cls.getFields().forEach(field -> {
                        field.removeModifier(Modifier.Keyword.PRIVATE, Modifier.Keyword.PROTECTED);
                        field.setModifier(Modifier.Keyword.PUBLIC, true);
                    });
                });
    }
}
