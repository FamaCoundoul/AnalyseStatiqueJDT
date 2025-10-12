package gui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import visiteurs.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class JDTAnalyzerGUI extends Application {

    private TableView<FileStats> fileTable = new TableView<>();
    private ObservableList<FileStats> fileStatsData = FXCollections.observableArrayList();

    private VBox globalStatsBox = new VBox(10);
    private Label statusLabel = new Label("Prêt");

    private File projectFolder;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle("Analyseur de Code Source ");

        BorderPane root = new BorderPane();
        root.setTop(createMenu(primaryStage));
        root.setCenter(createTabPane());
        root.setBottom(statusLabel);
        BorderPane.setMargin(statusLabel, new Insets(5));

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private MenuBar createMenu(Stage stage) {
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu("Fichier");
        MenuItem openProject = new MenuItem("Ouvrir projet");
        openProject.setOnAction(e -> chooseProjectFolder(stage));
        MenuItem exitItem = new MenuItem("Quitter");
        exitItem.setOnAction(e -> stage.close());
        fileMenu.getItems().addAll(openProject, exitItem);

        Menu helpMenu = new Menu("Aide");
        MenuItem about = new MenuItem("À propos");
        about.setOnAction(e -> showAlert("À propos", "Analyseur JDT\nVersion 1.0"));
        helpMenu.getItems().add(about);

        menuBar.getMenus().addAll(fileMenu, helpMenu);
        return menuBar;
    }

    private TabPane createTabPane() {
        TabPane tabPane = new TabPane();

        Tab fileStatsTab = new Tab("Statistiques par fichier", createFileStatsTab());
        Tab globalStatsTab = new Tab("Statistiques globales", createGlobalStatsTab());

        tabPane.getTabs().addAll(fileStatsTab, globalStatsTab);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        return tabPane;
    }

    private VBox createFileStatsTab() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        fileTable.setItems(fileStatsData);

        TableColumn<FileStats, String> nameCol = new TableColumn<>("Fichier");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));

        TableColumn<FileStats, Integer> classesCol = new TableColumn<>("Classes");
        classesCol.setCellValueFactory(new PropertyValueFactory<>("numClasses"));

        TableColumn<FileStats, Integer> methodsCol = new TableColumn<>("Méthodes");
        methodsCol.setCellValueFactory(new PropertyValueFactory<>("numMethods"));

        TableColumn<FileStats, Integer> linesCol = new TableColumn<>("Lignes");
        linesCol.setCellValueFactory(new PropertyValueFactory<>("numLines"));

        TableColumn<FileStats, Integer> packagesCol = new TableColumn<>("Packages");
        packagesCol.setCellValueFactory(new PropertyValueFactory<>("numPackages"));

        fileTable.getColumns().addAll(nameCol, classesCol, methodsCol, linesCol, packagesCol);
        fileTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Button detailsBtn = new Button("Voir détails");
        detailsBtn.setOnAction(e -> showFileDetails());

        vbox.getChildren().addAll(fileTable, detailsBtn);
        return vbox;
    }

    private VBox createGlobalStatsTab() {
        globalStatsBox.setPadding(new Insets(10));
        return globalStatsBox;
    }

    private void chooseProjectFolder(Stage stage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choisir le projet Java");
        File selectedDir = directoryChooser.showDialog(stage);
        if (selectedDir != null) {
            this.projectFolder = selectedDir;
            analyzeProject();
        }
    }

private void analyzeProject() {
    if (projectFolder == null) return;

    fileStatsData.clear();
    globalStatsBox.getChildren().clear();

    // --- Demande de la valeur X à l'utilisateur ---

   // int threshold = 3; // valeur par défaut
  
    
    TextInputDialog inputDialog = new TextInputDialog();
    inputDialog.setTitle("Entrée requise");
    inputDialog.setHeaderText("Veuillez entrer une valeur :");
    inputDialog.setContentText("Valeur :");
    
    inputDialog.setTitle("Paramètre d'analyse");
    inputDialog.setHeaderText("Classes avec plus de X méthodes");
    inputDialog.setContentText("Veuillez entrer la valeur de X :");

    Optional<String> result = inputDialog.showAndWait();

    result.ifPresent(value -> {
        System.out.println("Valeur entrée : " + value);
    });


    ArrayList<File> javaFiles = Parser.listJavaFilesForFolder(projectFolder);

    // --- Création des visiteurs globaux ---
    ClassCounterVisitor classVisitor = new ClassCounterVisitor();
    MethodCounterVisitor methodVisitor = new MethodCounterVisitor();
    LineCodeCounterVisitor lineVisitor = new LineCodeCounterVisitor();
    PackageCounterVisitor packageVisitor = new PackageCounterVisitor();
    TopMethodClassVisitor topMethod = new TopMethodClassVisitor();
    TopAttributeClassVisitor topAttribute = new TopAttributeClassVisitor();
    CombinedCategoryVisitor combine = new CombinedCategoryVisitor();
    PlusXMethodCounterVisitor xMethod = new PlusXMethodCounterVisitor(); // ← valeur X
    MaxParametersCounterVisitor maxParam = new MaxParametersCounterVisitor();

    try {
        for (File fileEntry : javaFiles) {
            String content = FileUtils.readFileToString(fileEntry, "UTF-8");
            CompilationUnit parse = Parser.parse(content.toCharArray());

            // --- Visiteurs locaux par fichier ---
            ClassCounterVisitor classVisitorFile = new ClassCounterVisitor();
            MethodCounterVisitor methodVisitorFile = new MethodCounterVisitor();
            LineCodeCounterVisitor lineVisitorFile = new LineCodeCounterVisitor();
            PackageCounterVisitor packageVisitorFile = new PackageCounterVisitor();

            MethodPerClassAVGCounterVisitor classMethodStatsVisitor = new MethodPerClassAVGCounterVisitor();
            LinePerMethodAVGCounterVisitor linePerMethodAVG = new LinePerMethodAVGCounterVisitor(parse);
            AttributPerClassAVGCounterVisitor attrPerClassAVG = new AttributPerClassAVGCounterVisitor();
            LongestMethodsPerClassVisitor longMethod = new LongestMethodsPerClassVisitor(parse);

            // --- Appliquer les visiteurs locaux ---
            parse.accept(classVisitorFile);
            parse.accept(methodVisitorFile);
            parse.accept(lineVisitorFile);
            parse.accept(packageVisitorFile);
            parse.accept(classMethodStatsVisitor);
            parse.accept(linePerMethodAVG);
            parse.accept(attrPerClassAVG);
            parse.accept(longMethod);

            // --- Ajouter au tableau ---
            fileStatsData.add(new FileStats(
                    fileEntry.getName(),
                    classVisitorFile.getClassCount(),
                    methodVisitorFile.getMethodCount(),
                    lineVisitorFile.getLgcode(),
                    packageVisitorFile.getDistinctPackageCount(),
                    classMethodStatsVisitor.getAverageMethodsPerCleass(),
                    linePerMethodAVG.getAverageLinesPerMethod(),
                    attrPerClassAVG.getAverageAttributesperClass(),
                    longMethod.toString()
            ));

            // --- Visiteurs globaux ---
            parse.accept(classVisitor);
            parse.accept(methodVisitor);
            parse.accept(lineVisitor);
            parse.accept(packageVisitor);
            parse.accept(topMethod);
            parse.accept(topAttribute);
            parse.accept(combine);
            parse.accept(xMethod);
            parse.accept(maxParam);
        }

        // --- Statistiques globales ---
        globalStatsBox.getChildren().addAll(
                new Label("Total classes : " + classVisitor.getClassCount()),
                new Label("Total interfaces : " + classVisitor.getInterfaceCount()),
                new Label("Total méthodes : " + methodVisitor.getMethodCount()),
                new Label("Total lignes de code : " + lineVisitor.getLgcode()),
                new Label("Total packages : " + packageVisitor.getDistinctPackageCount()),
                new Label("Top 10% classes par méthodes : " + topMethod.getTop10PercentClasses()),
                new Label("Top 10% classes par attributs : " + topAttribute.getTop10PercentClasses()),
                new Label("Intersection top méthodes & attributs : " +
                        combine.getIntersection(topMethod.getTop10PercentClasses(), topAttribute.getTop10PercentClasses())),
                new Label("Classes > " + Integer.parseInt(result.get())  + " méthodes : " + xMethod.getClassesOverThreshold(Integer.parseInt(result.get()))),
                new Label("Nombre maximal de paramètres : " + maxParam.getMaxParameters())
        );

        statusLabel.setText("Analyse terminée pour " + javaFiles.size() + " fichiers.");

    } catch (IOException e) {
        e.printStackTrace();
        showAlert("Erreur", "Une erreur est survenue lors de l'analyse.");
    }
}

    private void showFileDetails() {
        FileStats selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Détails du fichier " + selected.getFileName());
            alert.setHeaderText(null);
            alert.setContentText(
                    "Nom de la classe / interface : " + selected.getFileName() + "\n" +
                    "Nombre de lignes de code : " + selected.getNumLines() + "\n" +
                    "Nombre  de package (hierarchie) : " + selected.getNumPackages() + "\n" 
                    
            );
            alert.showAndWait();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Classe interne pour TableView
    public static class FileStats {
        private final String fileName;
        private final int numClasses;
        private final int numMethods;
        private final int numLines;
        private final int numPackages;

        private final double avgMethods;
        private final double avgLines;
        private final double avgAttributes;
        private final String longestMethods;

        public FileStats(String fileName, int numClasses, int numMethods, int numLines, int numPackages,
                         double avgMethods, double avgLines, double avgAttributes, String longestMethods) {
            this.fileName = fileName;
            this.numClasses = numClasses;
            this.numMethods = numMethods;
            this.numLines = numLines;
            this.numPackages = numPackages;
            this.avgMethods = avgMethods;
            this.avgLines = avgLines;
            this.avgAttributes = avgAttributes;
            this.longestMethods = longestMethods;
        }

        public String getFileName() { return fileName; }
        public int getNumClasses() { return numClasses; }
        public int getNumMethods() { return numMethods; }
        public int getNumLines() { return numLines; }
        public int getNumPackages() { return numPackages; }
        public double getAvgMethods() { return avgMethods; }
        public double getAvgLines() { return avgLines; }
        public double getAvgAttributes() { return avgAttributes; }
        public String getLongestMethods() { return longestMethods; }
    }
}
