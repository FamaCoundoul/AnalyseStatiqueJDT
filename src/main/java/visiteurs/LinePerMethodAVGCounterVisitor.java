package visiteurs;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;


/*
 * Nombre	moyen	de	lignes
 * 	de	code	par	méthode.
 * */

public class LinePerMethodAVGCounterVisitor extends ASTVisitor {
	
	private int totalLines=0;
	private int totalMethods=0;
	private final CompilationUnit cu;
	
	public LinePerMethodAVGCounterVisitor(CompilationUnit cu) {
		this.cu=cu;
		
	}
	
	@Override
	public boolean visit(MethodDeclaration node) {
		int start = cu.getLineNumber(node.getStartPosition());
		int end= cu.getLineNumber(node.getStartPosition() + node.getLength());
		totalLines += (end-start);
		
		return super.visit(node);
	}
	
	public double getAverageLinesPerMethod() {
		
		return totalMethods==0 ? 0 : (double) totalLines /totalMethods;
		
	}

}
