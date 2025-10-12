package visiteurs;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

/*
 * Cette classe permet de compter 
 * le nombre de lignes de code
 * */

public class LineCodeCounterVisitor extends ASTVisitor {
	
	
	private int lgcode=0;
	
	@Override
		public void endVisit(CompilationUnit node) {
			int start= node.getStartPosition();
			int end =start+ node.getLength();
			
			String[] lines = node.toString().split("\n");
			lgcode += lines.length;
		
			super.endVisit(node);
		}
	
	
     public int getLgcode() {
    	 return lgcode;
     }

	

	

}
