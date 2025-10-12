package gui;

import graph.ClassMethodCallVisitor;
import graph.JDTParser;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * CallGraphGUI - Visualisation interactive du graphe d'appel d'un projet Java.
 */
public class JDTCallGraphGUI extends Application {

    private TreeView<String> treeView = new TreeView<>();
    private Pane graphPane = new Pane();
    private Label statusLabel = new Label("Prêt");
    private Map<String, VisualNode> nodeMap = new HashMap<>();
    private Map<String, Map<String, List<ClassMethodCallVisitor.MethodCall>>> projectGraph = new LinkedHashMap<>();
    private BorderPane mainRoot;
    private TextField searchField = new TextField();

    // Transform for zoom/pan
    private double scale = 1.0;
    private double mousePrevX, mousePrevY;

    // layout constants
    private static final double CLASS_RADIUS = 36;
    private static final double METHOD_RADIUS = 28;
    private static final double NODE_SPACING_X = 220;
    private static final double NODE_SPACING_Y = 90;
    
    //pour eviter le clignotement de mon graphe
    private boolean legendAdded= false;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Graphe d'Appel - Visualisation Projet");

        // ---- Left: TreeView + Details below ----
        VBox leftBox = new VBox(8);
        leftBox.setPadding(new Insets(8));
        leftBox.setPrefWidth(360); // un peu plus large

        // Structure du projet
        Label treeTitle = new Label("Structure du projet");
        treeTitle.setFont(Font.font(14));
        treeView.setRoot(new TreeItem<>("Aucun projet chargé"));
        treeView.setPrefHeight(400); // laisse de la place pour les détails
        treeView.setShowRoot(true);

        searchField.setPromptText("Rechercher classe/méthode...");
        searchField.setOnKeyReleased(e -> applySearch(searchField.getText()));

        // Détails en dessous
        Label detailsTitle = new Label("Détails");
        detailsTitle.setFont(Font.font(14));
        TextArea detailsArea = new TextArea();
        detailsArea.setEditable(false);
        detailsArea.setWrapText(true);
        detailsArea.setPrefHeight(250);

        Button exportDot = new Button("Exporter en .dot");
        exportDot.setOnAction(e -> exportDot(stage));

        leftBox.getChildren().addAll(treeTitle, searchField, treeView, detailsTitle, detailsArea, exportDot);

        // ---- Center: graphPane avec ScrollPane ----
        ScrollPane scrollPane = new ScrollPane(graphPane);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);

        StackPane centerStack = new StackPane(scrollPane);
        centerStack.setPadding(new Insets(6));

        enablePanAndZoom(graphPane); // zoom via molette + drag

      
        // ---- Bottom: status ----
        HBox bottom = new HBox(statusLabel);
        bottom.setPadding(new Insets(6));

        // ---- Menu ----
        MenuBar menuBar = createMenu(stage);

        // ---- BorderPane ----
        mainRoot = new BorderPane();
        mainRoot.setTop(menuBar);
        mainRoot.setLeft(leftBox);
        mainRoot.setCenter(centerStack);
        mainRoot.setBottom(bottom);

        Scene scene = new Scene(mainRoot, 1400, 900);
        stage.setScene(scene);
        stage.show();

        // ---- Tree selection listener ----
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                String val = newV.getValue();
                if (val.startsWith("Classe : ")) {
                    String className = val.substring("Classe : ".length());
                    layoutAndDrawSubGraph(className, null); // Graphe pour la classe
                    detailsArea.setText(buildClassDetails(className));
                } else if (val.startsWith("Méthode : ")) {
                    String methodName = val.substring("Méthode : ".length());
                    layoutAndDrawSubGraph(null, methodName); // Graphe pour la méthode
                    detailsArea.setText(buildMethodDetails(methodName));
                } else if (newV== treeView.getRoot()) {
                	layoutAndDrawGraph(projectGraph);
                    detailsArea.setText("Projet: "+JDTParser.projectPath);
                } else {
                    detailsArea.setText(val);
                }
            }
        });

    }

    // --- MENU ---
    private MenuBar createMenu(Stage stage) {
        MenuBar menuBar = new MenuBar();

        Menu file = new Menu("Fichier");
        MenuItem openProj = new MenuItem("Ouvrir projet (src)...");
        openProj.setOnAction(e -> chooseProjectDir(stage));
        MenuItem exit = new MenuItem("Quitter");
        exit.setOnAction(e -> Platform.exit());
        file.getItems().addAll(openProj, new SeparatorMenuItem(), exit);

        Menu view = new Menu("Affichage");

        MenuItem fit = new MenuItem("Réinitialiser zoom/position");
        fit.setOnAction(e -> resetView());


        view.getItems().addAll(fit, new SeparatorMenuItem());


        Menu help = new Menu("Aide");
        MenuItem about = new MenuItem("À propos");
        about.setOnAction(e -> showAlert("À propos", "Graphe d'Appel – Visualisation Projet\nVersion améliorée"));
        help.getItems().add(about);

        menuBar.getMenus().addAll(file, view, help);
        return menuBar;
    }

    private void chooseProjectDir(Stage stage) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Choisir le dossier racine du projet (contenant src/)");
        File chosen = dc.showDialog(stage);
        if (chosen != null) {
            JDTParser.setProjectPath(chosen.getAbsolutePath());
            Task<Void> task = new Task() {
                @Override
                protected Void call() throws Exception {
                    Platform.runLater(() -> statusLabel.setText("Analyse en cours..."));
                    projectGraph = JDTParser.parseProject();
                    Platform.runLater(() -> {
                        buildTreeFromProject(projectGraph);
                        layoutAndDrawGraph(projectGraph);
                        statusLabel.setText("Analyse terminée : " + chosen.getName());
                    });
                    return null;
                }
            };
            new Thread(task).start();
        }
    }
    
    /**
     * Dessine un graphe limité à une classe ou une méthode spécifique.
     * Si className != null -> montre la classe et ses méthodes + appels
     * Si methodName != null -> montre uniquement cette méthode et ses appels
     */
    private void layoutAndDrawSubGraph(String className, String methodName) {
        graphPane.getChildren().clear();
        nodeMap.clear();

        if (className == null && methodName == null) return;

        double startX = 140;
        double startY = 180;

        if (className != null) {
            // Classe + ses méthodes
            Map<String, List<ClassMethodCallVisitor.MethodCall>> methods = projectGraph.get(className);
            if (methods == null) return;

            VisualNode clsNode = createVisualNode(className, startX, startY, CLASS_RADIUS, NodeType.CLASS);
            nodeMap.put(className, clsNode);
            graphPane.getChildren().addAll(clsNode.view(), clsNode.label);

            int mIdx = 0;
            double methodX = startX + NODE_SPACING_X;
            double methodYStart = startY - (methods.size() - 1) * (NODE_SPACING_Y / 2.0);
            for (String m : methods.keySet()) {
                if (methodName != null && !m.equals(methodName)) continue; // filtrage par méthode
                double methodY = methodYStart + mIdx * NODE_SPACING_Y;
                VisualNode methodNode = createVisualNode(m, methodX, methodY, METHOD_RADIUS, NodeType.METHOD);
                nodeMap.put(m, methodNode);
                graphPane.getChildren().addAll(methodNode.view(), methodNode.label);

                drawArrow(clsNode.getCenterX(), clsNode.getCenterY(), methodNode.getCenterX(), methodNode.getCenterY(), Color.DARKGRAY, CLASS_RADIUS, METHOD_RADIUS);

                List<ClassMethodCallVisitor.MethodCall> calls = methods.get(m);
                double callX = methodNode.getCenterX() + NODE_SPACING_X;
                double callYStart = methodNode.getCenterY() - (calls.size() - 1) * (NODE_SPACING_Y / 2.0);
                for (int j = 0; j < calls.size(); j++) {
                    ClassMethodCallVisitor.MethodCall call = calls.get(j);
                    double callY = callYStart + j * NODE_SPACING_Y;
                    VisualNode target = nodeMap.get(call.name);
                    if (target == null) {
                        target = createVisualNode(call.name, callX, callY, METHOD_RADIUS, NodeType.EXTERNAL);
                        nodeMap.put(call.name, target);
                        graphPane.getChildren().addAll(target.view(), target.label);
                    }
                    drawArrow(methodNode.getCenterX(), methodNode.getCenterY(), target.getCenterX(), target.getCenterY(), Color.MEDIUMPURPLE, METHOD_RADIUS, target.radius);
                }
                mIdx++;
            }
        } else if (methodName != null) {
            // méthode spécifique : chercher la classe qui contient cette méthode
            for (String cls : projectGraph.keySet()) {
                Map<String, List<ClassMethodCallVisitor.MethodCall>> methods = projectGraph.get(cls);
                if (methods.containsKey(methodName)) {
                    layoutAndDrawSubGraph(cls, methodName);
                    break;
                }
            }
        }

        addLegend();
    }


    // --- TREE ---
    private void buildTreeFromProject(Map<String, Map<String, List<ClassMethodCallVisitor.MethodCall>>> graph) {
        TreeItem<String> root = new TreeItem<>("Projet : " + JDTParser.projectPath);
        root.setExpanded(true);
        for (String className : graph.keySet()) {
            TreeItem<String> classItem = new TreeItem<>("Classe : " + className);
            classItem.setExpanded(false);
            Map<String, List<ClassMethodCallVisitor.MethodCall>> methods = graph.get(className);
            for (String methodName : methods.keySet()) {
                TreeItem<String> methodItem = new TreeItem<>("Méthode : " + methodName);
                for (ClassMethodCallVisitor.MethodCall call : methods.get(methodName)) {
                    TreeItem<String> callItem = new TreeItem<>("Appelle : " + call.name + " (Type : " + call.type + ")");
                    methodItem.getChildren().add(callItem);
                }
                classItem.getChildren().add(methodItem);
            }
            root.getChildren().add(classItem);
        }
        treeView.setRoot(root);
    }
    
  


    // --- GRAPH DRAW ---
    private void layoutAndDrawGraph(Map<String, Map<String, List<ClassMethodCallVisitor.MethodCall>>> graph) {
        graphPane.getChildren().clear();
        nodeMap.clear();

        int classesCount = graph.size();
        double startX = 140;
        double startY = 180;
        double classSpacingY = Math.max(120, NODE_SPACING_Y);

        int idx = 0;
        for (String className : graph.keySet()) {
            double classY = startY + idx * classSpacingY;
            VisualNode classNode = createVisualNode(className, startX, classY, CLASS_RADIUS, NodeType.CLASS);
            nodeMap.put(className, classNode);
            graphPane.getChildren().addAll(classNode.view(), classNode.label);
            idx++;
        }

        for (String className : graph.keySet()) {
            VisualNode cls = nodeMap.get(className);
            Map<String, List<ClassMethodCallVisitor.MethodCall>> methods = graph.get(className);
            int mIdx = 0;
            double methodX = cls.getCenterX() + NODE_SPACING_X;
            double methodYStart = cls.getCenterY() - (methods.size() - 1) * (NODE_SPACING_Y / 2.0);
            for (String m : methods.keySet()) {
                double methodY = methodYStart + mIdx * NODE_SPACING_Y;
                VisualNode methodNode = nodeMap.get(m);
                if (methodNode == null) {
                    methodNode = createVisualNode(m, methodX, methodY, METHOD_RADIUS, NodeType.METHOD);
                    nodeMap.put(m, methodNode);
                    graphPane.getChildren().addAll(methodNode.view(), methodNode.label);
                }
                drawArrow(cls.getCenterX(), cls.getCenterY(), methodNode.getCenterX(), methodNode.getCenterY(), Color.DARKGRAY, CLASS_RADIUS, METHOD_RADIUS);

                List<ClassMethodCallVisitor.MethodCall> calls = methods.get(m);
                double callX = methodNode.getCenterX() + NODE_SPACING_X;
                double callYStart = methodNode.getCenterY() - (calls.size() - 1) * (NODE_SPACING_Y / 2.0);
                for (int j = 0; j < calls.size(); j++) {
                    ClassMethodCallVisitor.MethodCall call = calls.get(j);
                    double callY = callYStart + j * NODE_SPACING_Y;
                    VisualNode target = nodeMap.get(call.name);
                    if (target == null) {
                        target = createVisualNode(call.name, callX, callY, METHOD_RADIUS, NodeType.EXTERNAL);
                        nodeMap.put(call.name, target);
                        graphPane.getChildren().addAll(target.view(), target.label);
                    }
                    drawArrow(methodNode.getCenterX(), methodNode.getCenterY(), target.getCenterX(), target.getCenterY(), Color.MEDIUMPURPLE, METHOD_RADIUS, target.radius);
                }
                mIdx++;
            }
        }

        nodeMap.values().forEach(n -> {
            n.view().toFront();
            n.label.toFront();
        });

        addLegend();
    }

    private VisualNode createVisualNode(String name, double x, double y, double r, NodeType type) {
        VisualNode vn = new VisualNode(name, x, y, r, type);

        vn.view().setOnMouseEntered(e -> vn.view().setEffect(new DropShadow(8, Color.GRAY)));
        vn.view().setOnMouseExited(e -> vn.view().setEffect(null));

        vn.view().setOnMouseClicked(e -> {
            treeView.getSelectionModel().clearSelection();
            TreeItem<String> found = findTreeItem(treeView.getRoot(), name);
            if (found != null) treeView.getSelectionModel().select(found);
        });

        vn.view().setOnMousePressed(e -> {
            vn.setDragOffset(e.getX(), e.getY());
            vn.view().setCursor(Cursor.MOVE);
        });

        vn.view().setOnMouseDragged(e -> {
            Point2D p = graphPane.sceneToLocal(e.getSceneX(), e.getSceneY());
            vn.relocateTo(p.getX(), p.getY());
            graphPane.getChildren().removeIf(node -> node instanceof Line || node instanceof Polygon);
            redrawAllEdges();
        });

        vn.view().setOnMouseReleased(e -> vn.view().setCursor(Cursor.HAND));
        vn.view().setCursor(Cursor.HAND);

        vn.label.setFont(Font.font(12));
        vn.label.setMouseTransparent(true);

        return vn;
    }

    private void redrawAllEdges() {
        if (projectGraph == null || projectGraph.isEmpty()) return;
        for (String className : projectGraph.keySet()) {
            VisualNode cls = nodeMap.get(className);
            if (cls == null) continue;
            Map<String, List<ClassMethodCallVisitor.MethodCall>> methods = projectGraph.get(className);
            for (String m : methods.keySet()) {
                VisualNode methodNode = nodeMap.get(m);
                if (methodNode != null) {
                    drawArrow(cls.getCenterX(), cls.getCenterY(), methodNode.getCenterX(), methodNode.getCenterY(), Color.DARKGRAY, cls.radius, methodNode.radius);
                }
                for (ClassMethodCallVisitor.MethodCall call : methods.get(m)) {
                    VisualNode target = nodeMap.get(call.name);
                    VisualNode source = methodNode;
                    if (source != null && target != null) {
                        drawArrow(source.getCenterX(), source.getCenterY(), target.getCenterX(), target.getCenterY(), Color.MEDIUMPURPLE, source.radius, target.radius);
                    }
                }
            }
        }
    }

    private void drawArrow(double startX, double startY, double endX, double endY, Color color, double startRadius, double endRadius) {
        double dx = endX - startX;
        double dy = endY - startY;
        double length = Math.sqrt(dx*dx + dy*dy);
        if (length == 0) return;
        double factorStart = startRadius / length;
        double factorEnd = endRadius / length;
        double sx = startX + dx * factorStart;
        double sy = startY + dy * factorStart;
        double ex = endX - dx * factorEnd;
        double ey = endY - dy * factorEnd;

        Line line = new Line(sx, sy, ex, ey);
        line.setStroke(color);
        line.setStrokeWidth(1.8);
        line.setOpacity(0.9);
        graphPane.getChildren().add(line);

        double angle = Math.atan2(ey - sy, ex - sx);
        double arrowSize = 8;
        double ax1 = ex - arrowSize * Math.cos(angle - Math.PI/6);
        double ay1 = ey - arrowSize * Math.sin(angle - Math.PI/6);
        double ax2 = ex - arrowSize * Math.cos(angle + Math.PI/6);
        double ay2 = ey - arrowSize * Math.sin(angle + Math.PI/6);

        Polygon head = new Polygon(ex, ey, ax1, ay1, ax2, ay2);
        head.setFill(color);
        head.setOpacity(0.95);
        graphPane.getChildren().add(head);
    }

    private void addLegend() {
    	if(legendAdded) return;//ne pas recreer
        HBox leg = new HBox(12);
        leg.setPadding(new Insets(10));
        Rectangle c1 = new Rectangle(14,14, Color.web("#FFA500"));
        Rectangle c2 = new Rectangle(14,14, Color.web("#FFB6C1"));
        Rectangle c3 = new Rectangle(14,14, Color.web("#9370db"));
        leg.getChildren().addAll(c1, new Text("Classe"), c2, new Text("Méthode (interne)"), c3, new Text("Méthode (externe)"));
        leg.setLayoutX(20);
        leg.setLayoutY(60);
        graphPane.getChildren().add(leg);
    }

    // --- PAN & ZOOM ---
    private void enablePanAndZoom(Pane pane) {
        pane.setOnScroll(event -> {
            double delta = 1.2;
            double scaleFactor = (event.getDeltaY() > 0) ? delta : 1/delta;
            applyZoom(scaleFactor);
            event.consume();
        });

        pane.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.MIDDLE || (e.isPrimaryButtonDown() && e.isAltDown())) {
                mousePrevX = e.getSceneX();
                mousePrevY = e.getSceneY();
                pane.setCursor(Cursor.MOVE);
            }
        });

        pane.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.MIDDLE || (e.isPrimaryButtonDown() && e.isAltDown())) {
                double dx = e.getSceneX() - mousePrevX;
                double dy = e.getSceneY() - mousePrevY;
                pane.setTranslateX(pane.getTranslateX() + dx);
                pane.setTranslateY(pane.getTranslateY() + dy);
                mousePrevX = e.getSceneX();
                mousePrevY = e.getSceneY();
            }
        });

        pane.setOnMouseReleased(e -> pane.setCursor(Cursor.DEFAULT));
    }

    private void applyZoom(double factor) {
        scale *= factor;
        graphPane.setScaleX(scale);
        graphPane.setScaleY(scale);
    }

    private void applySearch(String text) {
        if (text == null || text.isEmpty()) {
            nodeMap.values().forEach(n -> n.view().setOpacity(1.0));
            return;
        }
        String lower = text.toLowerCase();
        nodeMap.values().forEach(n -> {
            if (n.name.toLowerCase().contains(lower)) n.view().setOpacity(1.0);
            else n.view().setOpacity(0.2);
        });
    }

    private void resetView() {
        graphPane.setTranslateX(0);
        graphPane.setTranslateY(0);
        scale = 1.0;
        graphPane.setScaleX(scale);
        graphPane.setScaleY(scale);
    }

    private void exportDot(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter graphe en .dot");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier DOT", "*.dot"));
        File f = fc.showSaveDialog(stage);
        if (f != null) {
            try (FileWriter writer = new FileWriter(f)) {
                writer.write(JDTParser.exportGraphToDot(projectGraph));
                showAlert("Export réussi", "Fichier .dot généré : " + f.getAbsolutePath());
            } catch (IOException ex) {
                showAlert("Erreur", "Impossible d'écrire le fichier .dot : " + ex.getMessage());
            }
        }
    }

    private void highlightAndCenterNode(String name) {
        VisualNode n = nodeMap.get(name);
        if (n != null) {
            n.view().setEffect(new DropShadow(14, Color.RED));
            Point2D center = new Point2D(n.getCenterX(), n.getCenterY());
            graphPane.setTranslateX(-center.getX() + 700); // centré approximativement
            graphPane.setTranslateY(-center.getY() + 400);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> n.view().setEffect(null));
                }
            }, 1500);
        }
    }

    private String buildClassDetails(String className) {
        StringBuilder sb = new StringBuilder();
        sb.append("Classe : ").append(className).append("\n");
        Map<String, List<ClassMethodCallVisitor.MethodCall>> methods = projectGraph.get(className);
        if (methods != null) {
            sb.append("Méthodes :\n");
            for (String m : methods.keySet()) sb.append(" - ").append(m).append("\n");
        }
        return sb.toString();
    }

    private String buildMethodDetails(String methodName) {
        StringBuilder sb = new StringBuilder();
        sb.append("Méthode : ").append(methodName).append("\n");
        nodeMap.keySet().forEach(k -> {
            if (k.equals(methodName)) {
                sb.append("Appels :\n");
                projectGraph.values().forEach(m -> {
                    m.forEach((mn, calls) -> {
                        if (mn.equals(methodName)) calls.forEach(c -> sb.append("  -> ").append(c.name).append("\n"));
                    });
                });
            }
        });
        return sb.toString();
    }

    private TreeItem<String> findTreeItem(TreeItem<String> root, String name) {
        if (root.getValue().contains(name)) return root;
        for (TreeItem<String> c : root.getChildren()) {
            TreeItem<String> r = findTreeItem(c, name);
            if (r != null) return r;
        }
        return null;
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }

    // --- NodeType Enum ---
    private enum NodeType { CLASS, METHOD, EXTERNAL }

    // --- VisualNode Class ---
    private static class VisualNode {
        String name;
        double x, y;
        double radius;
        NodeType type;
        Rectangle view;
        Text label;
        double dragOffsetX, dragOffsetY;

        VisualNode(String name, double x, double y, double r, NodeType type) {
            this.name = name; this.x = x; this.y = y; this.radius = r; this.type = type;
            view = new Rectangle(x - r, y - r, 2*r, 2*r);
            view.setArcWidth(14);
            view.setArcHeight(14);
            view.setFill(type == NodeType.CLASS ? Color.ORANGE : type == NodeType.METHOD ? Color.PINK : Color.MEDIUMPURPLE);
            view.setStroke(Color.BLACK);
            label = new Text(x - r/1.5, y+4, name);
        }

        Rectangle view() { return view; }
        void setDragOffset(double dx, double dy) { dragOffsetX = dx; dragOffsetY = dy; }
        void relocateTo(double nx, double ny) { x = nx; y = ny; view.setX(x-radius); view.setY(y-radius); label.setX(x-radius/1.5); label.setY(y+4); }
        double getCenterX() { return x; }
        double getCenterY() { return y; }
    }
}
