package visiteurs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

/*
 * Les	10%	des	méthodes	
 * qui	possèdent	le	plus	
 * grand	nombre	de	lignes
 * 	de	code	(par	classe).
 * **/
public class LongestMethodsPerClassVisitor extends ASTVisitor {

	private final Map<String, Integer> methodLineCounts = new HashMap<>();
    private final CompilationUnit cu;

    public LongestMethodsPerClassVisitor(CompilationUnit cu) {
        this.cu = cu;
    }
    
    @Override
    public boolean visit(MethodDeclaration node) {
        int start = cu.getLineNumber(node.getStartPosition());
        int end = cu.getLineNumber(node.getStartPosition() + node.getLength());
        int lines = end - start;
        methodLineCounts.put(node.getName().getIdentifier(), lines);
        return super.visit(node);
    }

    public List<String> getTop10PercentMethods() {
        int limit = Math.max(1, (int) Math.ceil(methodLineCounts.size() * 0.1));
        return methodLineCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
	
    
	
}

