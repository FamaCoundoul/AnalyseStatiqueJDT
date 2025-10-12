package graph;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Parser amélioré : retourne un graphe global :
 * Map<className, Map<methodName, List<MethodCall>>>
 */
public class JDTParser {

    // Chemin vers ton projet (modifiable dynamiquement)
    public static String projectPath = "/Users/njap/eclipse-workspace/visitorDesignPattern";
    public static String projectSourcePath = projectPath + "/src";

    public static void setProjectPath(String p) {
        projectPath = p;
        projectSourcePath = projectPath + "/src";
    }

    /**
     * Parse tout le projet et retourne la structure d'appel complète.
     */
    public static Map<String, Map<String, List<ClassMethodCallVisitor.MethodCall>>> parseProject() throws IOException {
        ArrayList<File> javaFiles = listJavaFilesForFolder(new File(projectSourcePath));
        Map<String, Map<String, List<ClassMethodCallVisitor.MethodCall>>> projectGraph = new LinkedHashMap<>();

        for (File f : javaFiles) {
            try {
                String content = FileUtils.readFileToString(f, "UTF-8");
                CompilationUnit cu = parse(content.toCharArray(), f.getName());

                // pour chaque TypeDeclaration dans le fichier, on applique le visitor
                cu.accept(new org.eclipse.jdt.core.dom.ASTVisitor() {
                    @Override
                    public boolean visit(org.eclipse.jdt.core.dom.TypeDeclaration node) {
                        String className = node.getName().getIdentifier();
                        ClassMethodCallVisitor visitor = new ClassMethodCallVisitor(className);
                        cu.accept(visitor);
                        projectGraph.put(className, visitor.getMethods());
                        return false; // ne pas descendre récursivement
                    }
                });

            } catch (Exception ex) {
                System.err.println("Erreur lors de l'analyse du fichier : " + f.getAbsolutePath());
                ex.printStackTrace();
            }
        }

        return projectGraph;
    }

    /**
     * Parse et construit une CompilationUnit à partir d’un contenu Java.
     */
    public static CompilationUnit parse(char[] source, String unitName) {
        org.eclipse.jdt.core.dom.ASTParser parser = org.eclipse.jdt.core.dom.ASTParser.newParser(org.eclipse.jdt.core.dom.AST.JLS4);
        parser.setResolveBindings(true);
        parser.setKind(org.eclipse.jdt.core.dom.ASTParser.K_COMPILATION_UNIT);
        parser.setBindingsRecovery(true);

        parser.setUnitName(unitName);
        String[] sources = { projectSourcePath };

        // Classpath à enrichir selon ton environnement (facultatif)
        String[] classpath = {};

        parser.setEnvironment(classpath, sources, new String[]{"UTF-8"}, true);
        parser.setSource(source);

        return (CompilationUnit) parser.createAST(null);
    }

    /**
     * Récupère récursivement tous les fichiers .java d’un dossier.
     */
    public static ArrayList<File> listJavaFilesForFolder(final File folder) {
        ArrayList<File> javaFiles = new ArrayList<>();
        if (folder == null || !folder.exists()) return javaFiles;
        File[] files = folder.listFiles();
        if (files == null) return javaFiles;
        for (File f : files) {
            if (f.isDirectory()) javaFiles.addAll(listJavaFilesForFolder(f));
            else if (f.getName().endsWith(".java")) javaFiles.add(f);
        }
        return javaFiles;
    }

    // -------------------------------------------------------------------------
    //  Nouvelle méthode : affichage console du graphe complet du projet
    // -------------------------------------------------------------------------
    public static void printProjectCallGraph() {
        try {
            Map<String, Map<String, List<ClassMethodCallVisitor.MethodCall>>> projectGraph = parseProject();

            System.out.println("\n===== GRAPHE D'APPEL GLOBAL DU PROJET =====\n");
            for (String className : projectGraph.keySet()) {
                System.out.println("Classe : " + className);
                Map<String, List<ClassMethodCallVisitor.MethodCall>> methods = projectGraph.get(className);

                if (methods.isEmpty()) {
                    System.out.println("  (aucune méthode trouvée)\n");
                    continue;
                }

                for (String method : methods.keySet()) {
                    System.out.println("  Méthode : " + method);
                    List<ClassMethodCallVisitor.MethodCall> calls = methods.get(method);
                    if (calls.isEmpty()) {
                        System.out.println("    -> (aucun appel détecté)");
                    } else {
                        for (ClassMethodCallVisitor.MethodCall call : calls) {
                            System.out.println("    -> Appelle : " + call.name + " (Type : " + call.type + ")");
                        }
                    }
                }
                System.out.println();
            }
            System.out.println("=============================================\n");

        } catch (IOException e) {
            System.err.println("Erreur lors de l'analyse du projet : " + e.getMessage());
            e.printStackTrace();
        }
    }

    
    /**
     * Exporte le graphe sous forme de fichier DOT.
     *
     * @param graph Map<className, Map<methodName, List<MethodCall>>>
     * @return contenu DOT en String
     */
    public static String exportGraphToDot(Map<String, Map<String, List<ClassMethodCallVisitor.MethodCall>>> graph) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph G {\n");
        sb.append("  rankdir=LR;\n"); // Left to Right
        sb.append("  node [shape=box, style=filled, fillcolor=mediumpurple];\n");

        // Création des noeuds pour classes et méthodes
        for (String className : graph.keySet()) {
            sb.append("  \"").append(className).append("\" [shape=ellipse, fillcolor=orange];\n");
            Map<String, List<ClassMethodCallVisitor.MethodCall>> methods = graph.get(className);
            for (String methodName : methods.keySet()) {
                sb.append("  \"").append(methodName).append("\" [shape=box, fillcolor=pink];\n");
            }
        }

        // Création des arêtes
        for (String className : graph.keySet()) {
            Map<String, List<ClassMethodCallVisitor.MethodCall>> methods = graph.get(className);
            for (String methodName : methods.keySet()) {
                // Classe -> méthode
                sb.append("  \"").append(className).append("\" -> \"").append(methodName).append("\";\n");

                // Méthode -> appels
                List<ClassMethodCallVisitor.MethodCall> calls = methods.get(methodName);
                for (ClassMethodCallVisitor.MethodCall call : calls) {
                    sb.append("  \"").append(methodName).append("\" -> \"").append(call.name).append("\";\n");
                }
            }
        }
        
     // --- Légende ---
        sb.append("  subgraph cluster_legend {\n");
        sb.append("    label=\"Légende\";\n");
        sb.append("    fontsize=14;\n");
        sb.append("    color=black;\n");
        sb.append("    style=dashed;\n");
        sb.append("    legend_class [label=\"Classe\", shape=box, style=filled, fillcolor=orange];\n");
        sb.append("    legend_method [label=\"Méthode interne\", shape=box, style=filled, fillcolor=pink];\n");
        sb.append("    legend_external [label=\"Méthode externe\", shape=box, style=filled, fillcolor=mediumpurple];\n");
        sb.append("    legend_class -> legend_method [label=\"Contient\"];\n");
        sb.append("    legend_method -> legend_external [label=\"Appelle\"];\n");
        sb.append("  }\n");

        sb.append("}\n");
        return sb.toString();
    }
    
    // -------------------------------------------------------------------------
    //  Méthode main pour tester directement
    // -------------------------------------------------------------------------
    public static void main(String[] args) {
        printProjectCallGraph();
    }
}
