package visiteurs;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

/**
 * Le	nombre	maximal	de	
 * paramètres	par	rapport	à	
 * toutes	les	méthodes	de
l’application.
*/

public class MaxParametersCounterVisitor  extends ASTVisitor{

	
	private int maxParameters=1;
	
	@Override
	public boolean visit(MethodDeclaration node) {
			int paramCount= node.parameters().size();
			if(paramCount> maxParameters) {
				maxParameters=paramCount;
			}
			return super.visit(node);
		}
	
	public int getMaxParameters() {
		return maxParameters;
	}
}
