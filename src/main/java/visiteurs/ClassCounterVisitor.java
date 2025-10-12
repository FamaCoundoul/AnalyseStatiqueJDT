package visiteurs;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.TypeDeclaration;


/*
 * Cette classe permettre de 
 * compter le nombre de classes
 * de l'application
 * */
public class ClassCounterVisitor extends ASTVisitor{
	
	private int classCount=0;
	private int interfaceCount=0;
	
	@Override
	public boolean visit(TypeDeclaration node) {
		
		if(!node.isInterface()) {
			classCount++;
		}else {
			interfaceCount++;
		}
		
		return super.visit(node);
	}
	
	

	public int getClassCount() {
		return classCount;
	}

	public int getInterfaceCount() {
		return interfaceCount;
	}
	

}
