package visiteurs;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/*
 * Cette classe permet de compter
 * le nombre moyen d'attributs par classe
 * */

public class AttributPerClassAVGCounterVisitor extends ASTVisitor {
	
	
	private final Map<String, Integer> classAttrCount= new HashMap<>();
	private String currentClass=null;
	
	@Override
	public boolean visit(TypeDeclaration node) {
		currentClass= node.getName().getIdentifier();
		classAttrCount.putIfAbsent(currentClass, 0);
		
		
		return super.visit(node);
	}
	
	@Override
	public boolean visit(FieldDeclaration node) {
	
		if(currentClass != null) {
			classAttrCount.put(currentClass, classAttrCount.get(currentClass)+1);
		}
		
		return super.visit(node);
	}
	
	public double getAverageAttributesperClass() {
		
		return classAttrCount.values().stream().mapToInt(i -> i).average().orElse(0.0);
		
	}
	

}
