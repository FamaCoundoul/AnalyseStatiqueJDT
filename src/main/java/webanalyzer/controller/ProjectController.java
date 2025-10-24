package webanalyzer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graph.ClassMethodCallVisitor;
import webanalyzer.parser.Parser;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur principal pour l'analyse du projet et le calcul du couplage.
 */
@Controller
public class ProjectController {

    private Parser parser;

    @GetMapping("/")
    public String index() {
        return "index";
    }
    
    


    /**
     * Analyse le projet et calcule les métriques ainsi que le couplage entre classes.
     */
    @PostMapping("/analyze")
    public String analyzeProject(@RequestParam("path") String path,
					            @RequestParam(value = "xMethods", defaultValue = "2") int xMethods,
					            @RequestParam(value = "classA", required = false) String classA,
					            @RequestParam(value = "classB", required = false) String classB,
					            Model model) {
        parser = new Parser();

        File folder = new File(path);
        if (!folder.exists() || !folder.isDirectory()) {
            model.addAttribute("error", "Le chemin n'existe pas ou n'est pas un dossier : " + path);
            return "analysis";
        }

        List<File> javaFiles = Parser.listJavaFiles(folder);
        if (javaFiles.isEmpty()) {
            model.addAttribute("error", "Aucun fichier Java trouvé dans : " + path);
            return "analysis";
        }

        try {
            parser.analyzeProject(path);

            // --- Calcul du couplage entre classes ---
            Map<String, Map<String, Double>> couplingMap = calculateCouplingRatio(parser.getClassMethodCalls());
            String couplingGraphJson = convertCouplingToJson(couplingMap);
            List<Map<String, Object>> couplingMatrix = buildCouplingMatrix(couplingMap);
            
         // Liste pour affichage (noms simples)
            List<String> classAliases = javaFiles.stream()
                .map(this::extractClassName)
                .collect(Collectors.toList());
            
         // On garde aussi les fichiers complets pour le calcul réel
            Map<String, String> classAliasToPath = new HashMap<>();
            for (File file : javaFiles) {
                classAliasToPath.put(extractClassName(file), file.getAbsolutePath());
            }
            
            // --- Données Couplage ---
            
            model.addAttribute("couplingGraphJson", couplingGraphJson);
            model.addAttribute("couplingMatrix", couplingMatrix);
            model.addAttribute("allClasses", classAliases);
            model.addAttribute("projectPath", path);
            
            // --- Si l’utilisateur a sélectionné deux classes ---
            if (classA != null && classB != null) {
                double couplingAB = couplingMap.getOrDefault(classA, Collections.emptyMap())
                                               .getOrDefault(classB, 0.0);
                double couplingBA = couplingMap.getOrDefault(classB, Collections.emptyMap())
                                               .getOrDefault(classA, 0.0);

                model.addAttribute("classA", classA);
                model.addAttribute("classB", classB);
                model.addAttribute("couplingResultAB", String.format("%.5f", couplingAB));
                model.addAttribute("couplingResultBA", String.format("%.5f", couplingBA));
            } else {
            	model.addAttribute("classA",classA);
            	model.addAttribute("classB",classB);
            	
                model.addAttribute("couplingResultAB", "—");
                model.addAttribute("couplingResultBA", "—");
            }


            // Valeurs globales (moyennes et exemples)
            double moyenne = couplingMatrix.stream()
                    .mapToDouble(e -> (double) e.get("value"))
                    .average()
                    .orElse(0.0);
            model.addAttribute("couplingResultMean", String.format("%.5f", moyenne));
            

            // --- Métriques de Volume ---
            model.addAttribute("totalClasses", parser.getTotalClasses());
            model.addAttribute("totalInterfaces", parser.getTotalInterfaces());
            model.addAttribute("totalMethods", parser.getTotalMethods());
            model.addAttribute("totalLines", parser.getTotalLines());
            model.addAttribute("totalPackage", parser.getTotalPackage());

            // --- Métriques de Densité et Complexité ---
            model.addAttribute("avgMethodsPerClass", String.format("%.2f", parser.getAvgMethodsPerClass()));
            model.addAttribute("avgLinesPerMethod", String.format("%.2f", parser.getAvgLinesPerMethod()));
            model.addAttribute("avgAttributesPerClass", String.format("%.2f", parser.getAvgAttributesPerClass()));
            model.addAttribute("maxParameters", parser.getMaxParameters());

            // --- Densité : Top classes ---
            model.addAttribute("topMethodsClasses", parser.getTopMethodsClasses());
            model.addAttribute("topAttributeClasses", parser.getTopAttributeClasses());
            model.addAttribute("intersectionTopClasses", parser.getIntersectionTopClasses());
            model.addAttribute("xMethods", xMethods);
            model.addAttribute("classesOverXMethods", parser.getClassesOverXMethods(xMethods));

            // --- Autres données ---
            model.addAttribute("projectPath", path);
            model.addAttribute("fileAnalyses", parser.getFileAnalyses());
            model.addAttribute("graphJson", convertGraphToJson(parser.getClassMethodCalls()));

        } catch (IOException e) {
            model.addAttribute("error", "Erreur lors de l'analyse : " + e.getMessage());
        }

        return "analysis";
    }
    
    



    // --------------------------------------------------------------------
    // ---- CONVERTISSEURS ET CALCULATEURS
    // --------------------------------------------------------------------

    /**
     * Calcule le couplage dirigé entre classes.
     * Couplage(A,B) = Nombre d'appels A->B / Nombre total d'appels inter-classes dans l'application.
     */
    private Map<String, Map<String, Double>> calculateCouplingRatio(
            Map<String, Map<String, List<ClassMethodCallVisitor.MethodCall>>> classMethodCalls) {

        Map<String, Map<String, Double>> couplingMap = new HashMap<>();
        // Note: La logique de calculateCoupling est ici, renommée pour clarifier que c'est une métrique "ratio global".
        int totalRelations = 0;

        // 1. Compter les relations inter-classes (Dénominateur global)
        for (String sourceClass : classMethodCalls.keySet()) {
            for (List<ClassMethodCallVisitor.MethodCall> calls : classMethodCalls.get(sourceClass).values()) {
                for (ClassMethodCallVisitor.MethodCall call : calls) {
                    if (!isInternalCall(sourceClass, call.type)) { // Si la classe cible est différente de la source
                        totalRelations++;
                    }
                }
            }
        }

        // 2. Compter les relations entre chaque paire (A,B) (Numérateur)
        for (String sourceClass : classMethodCalls.keySet()) {
            for (List<ClassMethodCallVisitor.MethodCall> calls : classMethodCalls.get(sourceClass).values()) {
                for (ClassMethodCallVisitor.MethodCall call : calls) {
                    if (!isInternalCall(sourceClass, call.type)) {
                        couplingMap
                                .computeIfAbsent(sourceClass, k -> new HashMap<>())
                                .merge(call.type, 1.0, Double::sum); // Compte le nombre d'appels A -> B
                    }
                }
            }
        }

        // 3. Normalisation par le total (Couplage(A,B) = Appels(A->B) / TotalAppels)
        if (totalRelations > 0) {
            for (String a : couplingMap.keySet()) {
                for (String b : couplingMap.get(a).keySet()) {
                    double value = couplingMap.get(a).get(b) / totalRelations;
                    couplingMap.get(a).put(b, value);
                }
            }
        }

        return couplingMap;
    }

    // Méthode utilitaire pour vérifier si l'appel est dans la même classe
    private boolean isInternalCall(String sourceClass, String targetType) {
        if (targetType == null) return true;
        return sourceClass.equalsIgnoreCase(targetType.replace(".java", ""));
    }

   

    /**
     * Transforme la map de couplage en JSON pour Cytoscape.
     */
    private String convertCouplingToJson(Map<String, Map<String, Double>> couplingMap) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode elements = mapper.createArrayNode();
        Set<String> allClasses = new HashSet<>(couplingMap.keySet());

        // Récupérer toutes les cibles aussi
        couplingMap.values().forEach(targets -> allClasses.addAll(targets.keySet()));

        // Nœuds
        for (String cls : allClasses) {
            ObjectNode node = mapper.createObjectNode();
            node.putObject("data")
                    .put("id", cls)
                    .put("label", cls)
                    .put("type", "CLASS");
            elements.add(node);
        }

        // Arêtes pondérées
        for (String a : couplingMap.keySet()) {
            for (Map.Entry<String, Double> entry : couplingMap.get(a).entrySet()) {
                ObjectNode edge = mapper.createObjectNode();
                edge.putObject("data")
                        .put("source", a)
                        .put("target", entry.getKey())
                        .put("weight", entry.getValue())
                        .put("label", String.format("%.3f", entry.getValue()));
                elements.add(edge);
            }
        }

        return mapper.writeValueAsString(elements);
    }

    /**
     * Construit une liste plate {source, target, value} pour affichage dans un tableau Thymeleaf.
     */
    private List<Map<String, Object>> buildCouplingMatrix(Map<String, Map<String, Double>> couplingMap) {
        List<Map<String, Object>> list = new ArrayList<>();

        for (String a : couplingMap.keySet()) {
            for (Map.Entry<String, Double> entry : couplingMap.get(a).entrySet()) {
                Map<String, Object> map = new HashMap<>();
                map.put("source", a);
                map.put("target", entry.getKey());
                map.put("value", entry.getValue());
                list.add(map);
            }
        }
        return list;
    }

    /**
     * Conversion du graphe des appels de méthodes (global) en JSON pour Cytoscape.
     */
    private String convertGraphToJson(Map<String, Map<String, List<ClassMethodCallVisitor.MethodCall>>> graph)
            throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode elements = mapper.createArrayNode();

        for (String cls : graph.keySet()) {
            ObjectNode clsNode = mapper.createObjectNode();
            clsNode.putObject("data")
                    .put("id", cls)
                    .put("label", cls)
                    .put("type", "CLASS");
            elements.add(clsNode);

            for (Map.Entry<String, List<ClassMethodCallVisitor.MethodCall>> entry : graph.get(cls).entrySet()) {
                String method = entry.getKey();
                String methodId = cls + "." + method;

                ObjectNode methodNode = mapper.createObjectNode();
                methodNode.putObject("data")
                        .put("id", methodId)
                        .put("label", method)
                        .put("type", "METHOD");
                elements.add(methodNode);

                ObjectNode edgeClsMethod = mapper.createObjectNode();
                edgeClsMethod.putObject("data")
                        .put("source", cls)
                        .put("target", methodId);
                elements.add(edgeClsMethod);

                for (ClassMethodCallVisitor.MethodCall call : entry.getValue()) {
                    String targetId = call.type + "." + call.name;

                    ObjectNode callNode = mapper.createObjectNode();
                    callNode.putObject("data")
                            .put("id", targetId)
                            .put("label", call.name)
                            .put("type", "EXTERNAL");
                    elements.add(callNode);

                    ObjectNode edgeMethodCall = mapper.createObjectNode();
                    edgeMethodCall.putObject("data")
                            .put("source", methodId)
                            .put("target", targetId);
                    elements.add(edgeMethodCall);
                }
            }
        }

        return mapper.writeValueAsString(elements);
    }
    
    private String extractClassName(File file) {
        String name = file.getName();
        if (name.endsWith(".java")) {
            name = name.substring(0, name.length() - 5);
        }
        return name;
    }

}
