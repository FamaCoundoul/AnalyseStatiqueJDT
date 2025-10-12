package graph;

import org.eclipse.jdt.core.dom.*;
import java.util.*;

public class ClassMethodCallVisitor extends ASTVisitor {

    private String className; // Nom de la classe analysée
    private Map<String, List<MethodCall>> methods = new LinkedHashMap<>();

    private String currentMethod;

    public ClassMethodCallVisitor(String className) {
        this.className = className;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        if (!node.getName().getIdentifier().equals(className)) {
            return false; // ignorer les autres classes
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        currentMethod = node.getName().getIdentifier();
        methods.put(currentMethod, new ArrayList<>());

        if (node.getBody() != null) {
            node.getBody().accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodInvocation mi) {
                    String calledName = mi.getName().getIdentifier();
                    String receiverType = (mi.getExpression() != null && mi.getExpression().resolveTypeBinding() != null)
                            ? mi.getExpression().resolveTypeBinding().getName()
                            : "this";
                    methods.get(currentMethod).add(new MethodCall(calledName, receiverType));
                    return true;
                }
            });
        }

        return false;
    }

    public String getClassName() {
        return className;
    }

    public Map<String, List<MethodCall>> getMethods() {
        return methods;
    }

    public static class MethodCall {
        public String name;
        public String type;

        public MethodCall(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }
}
