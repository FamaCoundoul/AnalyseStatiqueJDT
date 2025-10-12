package visiteurs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/*
 * Les	10%	des	classes	qui	possèdent	le	
 * plus	grand	nombre	de	méthodes.
 * */

public class TopMethodClassVisitor extends ASTVisitor {
	
	private final Map<String, Integer> classMethodCount = new HashMap<>();

	private String currentClass=null;
	
	
	@Override
	public boolean visit(TypeDeclaration node) {
		currentClass=node.getName().getIdentifier();
		classMethodCount.putIfAbsent(currentClass, 0);	
		return super.visit(node);
	}
	
	@Override
	public boolean visit(MethodDeclaration node) {
		if(currentClass != null) {
			classMethodCount.put(currentClass, classMethodCount.get(currentClass)+1);
		}
		return super.visit(node);
	}
	
	public List<String> getTop10PercentClasses(){
		int limit = Math.max(1, (int) Math.ceil(classMethodCount.size() * 0.1));
		
		return classMethodCount.entrySet().stream()
				.sorted(Map.Entry.<String,Integer> comparingByValue().reversed())
				.limit(limit)
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());
	}
}
