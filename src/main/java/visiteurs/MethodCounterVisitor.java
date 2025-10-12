package visiteurs;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;


/*
 * Cette classe permet de compter
 * le nombre total de methodes
 * **/

public class MethodCounterVisitor  extends ASTVisitor{
	
	private int methodCount=0;
	
	
	@Override
	public boolean visit(MethodDeclaration node) {
		// TODO Auto-generated method stub
		methodCount++;
		return super.visit(node);
	}
	
	public int getMethodCount() {
		return methodCount;
	}

}
