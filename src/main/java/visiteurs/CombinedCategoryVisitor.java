package visiteurs;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;

/*
 * 
Les	classes	qui	font	partie	en	même	temps	des	deux	catégories	précédentes:
  appartenant aux	10%	des	classes	qui	possèdent	le	plus	grand	nombre	de	méthodes.
  appartenant aux	10%	des	classes	qui	possèdent	le	plus	grand	nombre d’attributs.
 * 
 * **/

public class CombinedCategoryVisitor extends ASTVisitor {
	
	public Set<String> getIntersection(List<String> topMethods, List<String> topAttributes ){
		
		Set<String> set= new HashSet<>(topMethods);
		set.retainAll(topAttributes);
		
		return set;
	}

}
