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
import java.util.List;
import java.util.Map;

@Controller
public class ProjectController {

    private Parser parser;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    // Ajout du paramètre xMethods avec une valeur par défaut de 10
    @PostMapping("/analyze")
    public String analyzeProject(@RequestParam("path") String path, 
                                 @RequestParam(value = "xMethods", defaultValue = "2") int xMethods, 
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

            // --- Métriques de Volume ---
            model.addAttribute("totalClasses", parser.getTotalClasses());
            model.addAttribute("totalInterfaces", parser.getTotalInterfaces());
            model.addAttribute("totalMethods", parser.getTotalMethods());
            model.addAttribute("totalLines", parser.getTotalLines());
            model.addAttribute("totalPackage", parser.getTotalPackage());
            
            // --- Métriques de Densité et Complexité (formatées) ---
            model.addAttribute("avgMethodsPerClass", String.format("%.2f", parser.getAvgMethodsPerClass()));
            model.addAttribute("avgLinesPerMethod", String.format("%.2f", parser.getAvgLinesPerMethod()));
            model.addAttribute("avgAttributesPerClass", String.format("%.2f", parser.getAvgAttributesPerClass()));
            model.addAttribute("maxParameters", parser.getMaxParameters());
            
            // --- Métriques de densité (Top classes) ---
            model.addAttribute("topMethodsClasses", parser.getTopMethodsClasses());
            model.addAttribute("topAttributeClasses", parser.getTopAttributeClasses());
            model.addAttribute("intersectionTopClasses", parser.getIntersectionTopClasses());
            model.addAttribute("xMethods", xMethods); // Pour l'affichage
            model.addAttribute("classesOverXMethods", parser.getClassesOverXMethods(xMethods));
            
         // 
            model.addAttribute("projectPath", path); 
            // ...
            model.addAttribute("xMethods", xMethods); 

            // --- Analyses par fichier et Graphe ---
            model.addAttribute("fileAnalyses", parser.getFileAnalyses());
            model.addAttribute("graphJson", convertGraphToJson(parser.getClassMethodCalls()));
            
           

        } catch (IOException e) {
            model.addAttribute("error", "Erreur lors de l'analyse : " + e.getMessage());
        }

        return "analysis";
    }

    private String convertGraphToJson(Map<String, Map<String, List<ClassMethodCallVisitor.MethodCall>>> graph) throws IOException {
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
}