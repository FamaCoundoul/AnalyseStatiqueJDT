package visiteurs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * 
 * Les	classes	qui	possèdent plus	
 * de	X	méthodes	
 * (la	valeur	de X	est	donnée).	
 * */

public class PlusXMethodCounterVisitor extends ASTVisitor {

	private final Map<String, Integer> classMethodCount = new HashMap<>();
    private String currentClass = null;
  

	@Override
	public boolean visit(TypeDeclaration node) {
        currentClass = node.getName().getIdentifier();
        classMethodCount.putIfAbsent(currentClass, 0);
        return super.visit(node);
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        if (currentClass != null) {
            classMethodCount.put(currentClass, classMethodCount.get(currentClass) + 1);
        }
        return super.visit(node);
    }
    
    public List<String> getClassesOverThreshold(int X) {
        return classMethodCount.entrySet().stream()
                .filter(e -> e.getValue() > X)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    
	
}
