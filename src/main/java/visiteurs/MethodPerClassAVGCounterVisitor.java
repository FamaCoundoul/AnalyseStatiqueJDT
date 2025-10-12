package visiteurs;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * Cette classe permet de compter
 * le nombre moyen de methodes par classe*/

public class MethodPerClassAVGCounterVisitor extends ASTVisitor {
	
	private final Map<String,Integer> classMethodCount= new HashMap<>();
	
	private String currentClass=null;
	
	@Override
	public boolean visit(TypeDeclaration node) {
		
		currentClass= node.getName().getIdentifier();
		classMethodCount.putIfAbsent(currentClass,0);
		return super.visit(node);
		
		
	}
	
	@Override
	public boolean visit(MethodDeclaration node) {
		if(currentClass != null) {
			classMethodCount.put(currentClass,classMethodCount.get(currentClass)+1);
		}
		return super.visit(node);
	}
	
	public double getAverageMethodsPerCleass() {
		return classMethodCount.values().stream().mapToInt(i -> i).average().orElse(0.0);
	}
	
	

}
