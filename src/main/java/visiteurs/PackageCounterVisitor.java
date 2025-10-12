package visiteurs;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.PackageDeclaration;

/**
 * Visiteur AST pour compter tous les packages DISTINCTS
 * présents dans une application Java, en incluant les packages parents.
 *
 */
public class PackageCounterVisitor extends ASTVisitor {

    // Ensemble des noms uniques de tous les packages
    private final Set<String> packageNames = new HashSet<>();

    @Override
    public boolean visit(PackageDeclaration node) {
        if (node != null && node.getName() != null) {
            String fullName = node.getName().getFullyQualifiedName();
            addPackageWithParents(fullName);
        }
        return false;
    }

    /**
     * Gère les fichiers sans déclaration de package
     */
    public void handleDefaultPackage(CompilationUnit unit) {
        if (unit.getPackage() == null) {
            packageNames.add("default");
        }
    }

    /**
     * Ajoute le package complet et tous ses préfixes parents
     * Exemple : "behavioral.visitor.sub" -> "behavioral", "behavioral.visitor", "behavioral.visitor.sub"
     */
    private void addPackageWithParents(String fullName) {
        String[] segments = fullName.split("\\.");
        String prefix = "";
        for (String s : segments) {
            prefix = prefix.isEmpty() ? s : prefix + "." + s;
            packageNames.add(prefix);
        }
    }

    /** Retourne le nombre total de packages distincts */
    public int getDistinctPackageCount() {
        return packageNames.size();
    }

    /** Retourne la liste complète des packages distincts */
    public Set<String> getPackageNames() {
        return packageNames;
    }
}
