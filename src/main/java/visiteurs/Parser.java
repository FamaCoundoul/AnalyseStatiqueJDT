package visiteurs;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import visiteurs.ClassCounterVisitor;

/**
 * Classe principale Parser qui permet de :
 *  - Explorer un projet Java (à analyser)
 *  - Construire un AST pour chaque fichier Java
 *  - Appliquer les visiteurs AST
 *
 * Ce parser constitue le point d’entrée de l’application Analyseur.
 */

public class Parser {
	//Chemin du projet analyser
	public  static String projectpath="/Users/njap/eclipse-workspace/visitorDesignPattern";
	public static String projectSourcePath=projectpath + "/src";
	
	public static int totalClasses = 0;
	public  static int totalMethods = 0;
	public  static int totalLines = 0;
	public static int globalPackage=0;


	public static void main(String[] args) throws IOException {
	
		// Récupération de tous les fichiers .java du projet à analyser
        final File folder = new File(projectSourcePath);
        ArrayList<File> javaFiles = listJavaFilesForFolder(folder);
        
       // Q1. Nombre de classes
        ClassCounterVisitor classVisitor = new ClassCounterVisitor();
        // Q2. Nombre de lignes de code
        LineCodeCounterVisitor lineVisitor = new LineCodeCounterVisitor();
        // Q3. Nombre de méthodes
        MethodCounterVisitor methodVisitor = new MethodCounterVisitor();
        // Q4. Nombre total de packages
        PackageCounterVisitor packageVisitor = new PackageCounterVisitor();
        //Q8.Les	10%	des	classes	qui	possèdent	le	plus	grand	nombre	de	méthodes.
        TopMethodClassVisitor topMethod= new TopMethodClassVisitor();
        //Q9.Les	10%	des	classes	qui	possèdent	le	plus	grand	nombre d’attributs.
        TopAttributeClassVisitor topAttribute= new TopAttributeClassVisitor();
        //Q10. Les	classes	qui	font	partie	en	même	temps	des	deux	catégories	précédentes.
        CombinedCategoryVisitor combine=new CombinedCategoryVisitor();
        //Q11.Les	classes	qui	possèdent plus	de	X	méthodes	(la	valeur	de X	est	donnée).
        PlusXMethodCounterVisitor xMethod= new PlusXMethodCounterVisitor();
       //Q13.Le	nombre	maximal	de	paramètres	par	rapport	à	toutes	les	méthodes de  l’application.
        MaxParametersCounterVisitor maxParam = new MaxParametersCounterVisitor();
        
        System.out.println("========== STATISTIQUES PAR FICHIER ==========");
        
        // Parcours de chaque fichier Java
        for (File fileEntry : javaFiles) {
            String content = FileUtils.readFileToString(fileEntry, "UTF-8");

            // Création de l’AST pour ce fichier
            CompilationUnit parse = parse(content.toCharArray());
            // Q1. Nombre de classes
            parse.accept(classVisitor);
            //totalClasses += classVisitor.getClassCount();
            // Q2. Nombre de lignes de code
            parse.accept(lineVisitor);
           // totalLines += lineVisitor.getLgcode();
            // Q3. Nombre de méthodes
            parse.accept(methodVisitor);
            //totalMethods += methodVisitor.getMethodCount();
             // Q4. Nombre total de packages
            parse.accept(packageVisitor);
            
            
            //Q5.Nombre	moyen	de	méthodes	par	classe
            MethodPerClassAVGCounterVisitor classMethodStatsVisitor= new MethodPerClassAVGCounterVisitor();
            parse.accept(classMethodStatsVisitor);
            
            //Q6.Nombre	moyen	de	lignes	de	code	par	méthode
            LinePerMethodAVGCounterVisitor linePerMethodAVG= new LinePerMethodAVGCounterVisitor(parse);
            parse.accept(linePerMethodAVG);
            //Q7.Nombre	moyen	d’attributs	par	classe.
            AttributPerClassAVGCounterVisitor attrPerClassAVG= new AttributPerClassAVGCounterVisitor();
            parse.accept(attrPerClassAVG);
            //Q8.Les	10%	des	classes	qui	possèdent	le	plus	grand	nombre	de	méthodes.
            parse.accept(topMethod);
            //Q9.Les	10%	des	classes	qui	possèdent	le	plus	grand	nombre d’attributs.
            parse.accept(topAttribute);
            //Q10. Les	classes	qui	font	partie	en	même	temps	des	deux	catégories	précédentes.
            parse.accept(combine);
            //Q11.Les	classes	qui	possèdent plus	de	X	méthodes	(la	valeur	de X	est	donnée).
            parse.accept(xMethod);
            //Q12. Les	10%	des	méthodes	qui	possèdent	le	plus	grand	nombre	de	lignes	de	code	(par classe).
            LongestMethodsPerClassVisitor longMethod= new LongestMethodsPerClassVisitor(parse);
            parse.accept(longMethod);
            //Q13.Le	nombre	maximal	de	paramètres	par	rapport	à	toutes	les	méthodes de  l’application.
            parse.accept(maxParam);
            
            
            
            // ================================
            // Affichage des résultats par fichier
            // ================================
           
           System.out.println("Fichier analysé : " + fileEntry.getName());
           System.out.println(" -> Nombre moyen de Méthodes par classe : " + classMethodStatsVisitor.getAverageMethodsPerCleass());
           System.out.println(" -> Nombre moyen de lignes de code par Méthodes : " + linePerMethodAVG.getAverageLinesPerMethod());
           System.out.println(" -> Nombre moyen d’Attributs par classe : " + attrPerClassAVG.getAverageAttributesperClass());
           System.out.println(" -> Les 10%	des	méthodes qui	possèdent le plus grand nombre de lignes de code (par classe)  : " + longMethod );
           System.out.println("===========================================");

          }
        System.out.println("\n");
        System.out.println("========== STATISTIQUES GLOBALES ==========");
        System.out.println("Total classes : " + classVisitor.getClassCount());
        System.out.println("Total interfaces : " + classVisitor.getInterfaceCount());
        System.out.println("Total méthodes : " + methodVisitor.getMethodCount());
        System.out.println("Total lignes de code : " + lineVisitor.getLgcode());
        System.out.println("Total packages : " + packageVisitor.getDistinctPackageCount());
        System.out.println(" -> Les	10%	des	classes	qui	possèdent le	plus grand	nombre	de	méthodes. : " + topMethod.getTop10PercentClasses());
        System.out.println(" -> Les	10%	des	classes	qui	possèdent	le	plus grand nombre d’attributs : " + topAttribute.getTop10PercentClasses());
        System.out.println(" ->  Les	classes	qui	font partie	en même temps	des	deux catégories	précédentes : " + combine.getIntersection(topMethod.getTop10PercentClasses(), topAttribute.getTop10PercentClasses()));
        System.out.println(" -> Les	classes	qui	possèdent plus	de	X	méthodes (la valeur de X est donnée) :" + xMethod.getClassesOverThreshold(3));
        System.out.println(" -> Le nombre	maximal	de	paramètres	par	rapport	à	toutes	les	méthodes de  l’application : " + maxParam.getMaxParameters());
 

            
        }
	
	
        
        /**
         * Parcourt récursivement un dossier et récupère tous les fichiers .java
         */
        public static ArrayList<File> listJavaFilesForFolder(final File folder) {
            ArrayList<File> javaFiles = new ArrayList<>();
            for (File fileEntry : folder.listFiles()) {
                if (fileEntry.isDirectory()) {
                    javaFiles.addAll(listJavaFilesForFolder(fileEntry));
                } else if (fileEntry.getName().endsWith(".java")) {
                    javaFiles.add(fileEntry);
                }
            }
            return javaFiles;
        }
        
        /**
         * Construit un AST pour une classe source donnée
         */
        public static CompilationUnit parse(char[] classSource) {
            ASTParser parser = ASTParser.newParser(AST.JLS4); 
            parser.setResolveBindings(true);
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            parser.setBindingsRecovery(true);

            Map<?, ?> options = JavaCore.getOptions();
            parser.setCompilerOptions(options);

            parser.setUnitName("");

            String[] sources = { projectSourcePath };
            String[] classpath = {}; // pas besoin de JRE path explicite en projet Maven

            parser.setEnvironment(classpath, sources, new String[] { "UTF-8" }, true);
            parser.setSource(classSource);

            return (CompilationUnit) parser.createAST(null);
        }
	

}
