package visiteurs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * Les	10%	des	classes	
 * qui	possèdent	le plus	
 * grand	nombre d’attributs.
 * */

public class TopAttributeClassVisitor extends ASTVisitor {
	
	private final Map<String, Integer> classAttrCount = new HashMap<>();
    private String currentClass = null;
	

    @Override
    public boolean visit(TypeDeclaration node) {
        currentClass = node.getName().getIdentifier();
        classAttrCount.putIfAbsent(currentClass, 0);
        return super.visit(node);
    }
    
    @Override
    public boolean visit(FieldDeclaration node) {
        if (currentClass != null) {
            classAttrCount.put(currentClass, classAttrCount.get(currentClass) + 1);
        }
        return super.visit(node);
    }

    public List<String> getTop10PercentClasses() {
        int limit = Math.max(1, (int) Math.ceil(classAttrCount.size() * 0.1));
        return classAttrCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
