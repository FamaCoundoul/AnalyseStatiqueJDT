package webanalyzer.parser;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import graph.ClassMethodCallVisitor;
import visiteurs.PackageCounterVisitor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyseur Java par fichier et projet complet avec graphe d'appels.
 */
@Service
public class Parser {

    // --- Métriques globales ---
    private int totalClasses = 0;
    private int totalInterfaces = 0;
    private int totalMethods = 0;
    private int totalLines = 0;
    private int totalPackage = 0;
    private int totalAttributes = 0;
    private int totalLinesInMethods = 0;
    private int maxParameters = 0;

    private double avgMethodsPerClass = 0.0;
    private double avgLinesPerMethod = 0.0;
    private double avgAttributesPerClass = 0.0;

    // --- Structures de données pour les tops et stats ---
    private final Map<String, FileAnalysis> fileAnalyses = new LinkedHashMap<>();
    private final Map<String, Map<String, List<ClassMethodCallVisitor.MethodCall>>> classMethodCalls = new LinkedHashMap<>();
    private final Map<String, Integer> methodsPerClass = new HashMap<>(); // NOM DE CLASSE -> NOMBRE DE METHODES
    private final Map<String, Integer> attributesPerClass = new HashMap<>(); // NOM DE CLASSE -> NOMBRE D'ATTRIBUTS

    //chemin du projet
    private String projectPath;

    // ===============================================
    // CLASSE INTERNE : ANALYSE PAR FICHIER
    // ===============================================
    public static class FileAnalysis {
        private final String fileName;
        private final List<String> classes = new ArrayList<>();
        private final List<String> methods = new ArrayList<>();

        public FileAnalysis(String fileName) { this.fileName = fileName; }
        public void addClass(String cls) { classes.add(cls); }
        public void addMethods(Collection<String> m) { methods.addAll(m); }
        public String getFileName() { return fileName; }
        public List<String> getClasses() { return classes; }
        public List<String> getMethods() { return methods; }
    }


    // ===============================================
    // VISITEUR AST POUR L'ANALYSE GLOBALE
    // ===============================================

    /**
     * Visiteur pour collecter les métriques spécifiques par classe/interface.
     */
    public class GlobalAnalysisVisitor extends ASTVisitor {
        private String currentClassName = null;

        @Override
        public boolean visit(TypeDeclaration node) {
            currentClassName = node.getName().getIdentifier();
            methodsPerClass.put(currentClassName, 0);
            attributesPerClass.put(currentClassName, 0);
            
            if (node.isInterface()) {
                totalInterfaces++;
            } else {
                totalClasses++;
            }

            // Compter les attributs (Fields)
            int attributeCount = 0;
            for (FieldDeclaration fd : node.getFields()) {
                attributeCount += fd.fragments().size();
            }
            attributesPerClass.put(currentClassName, attributeCount);
            totalAttributes += attributeCount;

            return true; 
        }

        @Override
        public boolean visit(MethodDeclaration node) {
            if (currentClassName != null) {
                // 1. Compter les méthodes par classe
                methodsPerClass.merge(currentClassName, 1, Integer::sum);
                totalMethods++;
                
                // 2. Compter les lignes de la méthode (approximatif)
                if (node.getBody() != null) {
                    totalLinesInMethods += node.getBody().getLength(); 
                }

                // 3. Nombre maximal de paramètres
                int paramCount = node.parameters().size();
                if (paramCount > maxParameters) {
                    maxParameters = paramCount;
                }
            }
            return super.visit(node);
        }

        @Override
        public void endVisit(TypeDeclaration node) {
            currentClassName = null;
        }
    }
    
    // ===============================================
    // MÉTHODE D'ANALYSE ET ACCESSEURS
    // ===============================================

    public void analyzeProject(String projectPath) throws IOException {
    	
    	if(!projectPath.isEmpty()) {
    		this.projectPath=projectPath;
    	}
    	
        resetMetrics();
        List<File> javaFiles = listJavaFiles(new File(projectPath));

        if (javaFiles.isEmpty()) {
            return;
        }

        PackageCounterVisitor p = new PackageCounterVisitor();

        for (File javaFile : javaFiles) {
            String code = FileUtils.readFileToString(javaFile, "UTF-8");
            CompilationUnit cu = createCompilationUnit(code, javaFile.getName());

            FileAnalysis fa = new FileAnalysis(javaFile.getName());

            // 1. Analyse Globale (Classes, Méthodes, Attributs, Paramètres, Interfaces)
            GlobalAnalysisVisitor globalVisitor = new GlobalAnalysisVisitor();
            cu.accept(globalVisitor);
            
            // 2. Analyse du Graphe d'Appels et par fichier
            for (Object typeObj : cu.types()) {
                if (typeObj instanceof TypeDeclaration) {
                    TypeDeclaration td = (TypeDeclaration) typeObj;
                    String className = td.getName().getIdentifier();
                    
                    ClassMethodCallVisitor callVisitor = new ClassMethodCallVisitor(className);
                    cu.accept(callVisitor);
                    classMethodCalls.put(className, callVisitor.getMethods());
                    
                    totalLines += countLines(td); 

                    fa.addClass(className);
                    fa.addMethods(callVisitor.getMethods().keySet());
                }
            }

            // 3. Comptage des packages
            cu.accept(p);
            totalPackage = p.getDistinctPackageCount();
            
            fileAnalyses.put(javaFile.getAbsolutePath(), fa);
        }

        // --- Calcul final des métriques moyennes ---
        int totalTypes = totalClasses + totalInterfaces;
        
        avgMethodsPerClass = totalTypes == 0 ? 0 : (double) totalMethods / totalTypes;
        avgAttributesPerClass = totalTypes == 0 ? 0 : (double) totalAttributes / totalTypes;
        avgLinesPerMethod = totalMethods == 0 ? 0 : (double) totalLinesInMethods / totalMethods;
    }

    private void resetMetrics() {
        totalClasses = totalInterfaces = totalMethods = totalLines = totalPackage = 0;
        totalAttributes = totalLinesInMethods = maxParameters = 0;
        avgMethodsPerClass = avgLinesPerMethod = avgAttributesPerClass = 0.0;
        fileAnalyses.clear();
        classMethodCalls.clear();
        methodsPerClass.clear();
        attributesPerClass.clear();
    }
    
    // === Accesseurs pour les Tops et Stats ===

    private int getTopN(int total) {
        // Retourne 10% (minimum 1, maximum 10)
        return Math.max(1, (int) Math.ceil(0.10 * total));
    }
    
    public List<String> getTopMethodsClasses() {
        int limit = getTopN(methodsPerClass.size());
        return methodsPerClass.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<String> getTopAttributeClasses() {
        int limit = getTopN(attributesPerClass.size());
        return attributesPerClass.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<String> getIntersectionTopClasses() {
        Set<String> topMethods = new HashSet<>(getTopMethodsClasses());
        Set<String> topAttributes = new HashSet<>(getTopAttributeClasses());
        topMethods.retainAll(topAttributes);
        return new ArrayList<>(topMethods);
    }

    public List<String> getClassesOverXMethods(int x) {
        return methodsPerClass.entrySet().stream()
                .filter(e -> e.getValue() > x)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    // === Getters pour le Controller ===
    public int getTotalClasses() { return totalClasses; }
    public int getTotalInterfaces() { return totalInterfaces; }
    public int getTotalMethods() { return totalMethods; }
    public int getTotalLines() { return totalLines; }
    public int getTotalPackage() { return totalPackage; }
    public int getMaxParameters() { return maxParameters; } 

    public double getAvgMethodsPerClass() { return avgMethodsPerClass; }
    public double getAvgLinesPerMethod() { return avgLinesPerMethod; }
    public double getAvgAttributesPerClass() { return avgAttributesPerClass; }

    public Map<String, Map<String, List<ClassMethodCallVisitor.MethodCall>>> getClassMethodCalls() { return classMethodCalls; }
    public Map<String, FileAnalysis> getFileAnalyses() { return fileAnalyses; }


    // === Méthodes auxiliaires ===
    public CompilationUnit createCompilationUnit(String code, String unitName) {
        ASTParser parser = ASTParser.newParser(AST.JLS4);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(code.toCharArray());
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        parser.setStatementsRecovery(true);
        parser.setUnitName(unitName);

        try {
            // Récupère le chemin du rt.jar de ton JRE (important sous Java 8)
            String javaHome = System.getProperty("java.home");
            String rtJarPath = javaHome + File.separator + "lib" + File.separator + "rt.jar";

            // Si le JRE est en mode JDK (comme sur macOS), adapter le chemin
            File rtJar = new File(rtJarPath);
            if (!rtJar.exists()) {
                // macOS JDK 8 stocke souvent les libs dans ../Classes/classes.jar
                rtJar = new File(javaHome + File.separator + ".." + File.separator + "Classes" + File.separator + "classes.jar");
            }

            String[] classpathEntries = { rtJar.getAbsolutePath() };
            String[] sourcepathEntries = { projectPath };

            parser.setEnvironment(
                    classpathEntries,      // dépendances (runtime du JRE)
                    sourcepathEntries,     // ton dossier source
                    null,                  // encodage par défaut
                    true                   // inclure sous-répertoires
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        return (CompilationUnit) parser.createAST(null);
    }



    
    public static List<File> listJavaFiles(File folder) {
        List<File> javaFiles = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    javaFiles.addAll(listJavaFiles(f));
                } else if (f.getName().endsWith(".java")) {
                    javaFiles.add(f);
                }
            }
        }
        return javaFiles;
    }

    private int countLines(TypeDeclaration td) {
        return td.getLength();
    }

	public String getProjectPath() {
		return projectPath;
	}

	public void setProjectPath(String projectPath) {
		this.projectPath = projectPath;
	}

	
	

    
}