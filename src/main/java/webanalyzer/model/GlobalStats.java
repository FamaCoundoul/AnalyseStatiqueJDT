package webanalyzer.model;

import java.util.List;

public class GlobalStats {
	private int totalFiles;
    private int totalClasses;
    private int totalInterfaces;
    private int totalMethods;
    private int totalLines;
    private int totalPackage;
    private double avgMethodsPerClass;
    private double avgLinesPerMethod;
    private double avgAttributesPerClass;
    
    private List<String> top10MethodsClasses;
    private List<String> top10AttributesClasses;
    private List<String> intersectionTopClasses;
    private List<String> classesOverXMethods;
    private int maxParameters;

    // Getters / setters
    public int getTotalFiles() { return totalFiles; }
    public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }
    public int getTotalClasses() { return totalClasses; }
    public void setTotalClasses(int totalClasses) { this.totalClasses = totalClasses; }
    public int getTotalMethods() { return totalMethods; }
    public void setTotalMethods(int totalMethods) { this.totalMethods = totalMethods; }
	public int getTotalPackage() {
		return totalPackage;
	}
	public void setTotalPackage(int totalPackage) {
		this.totalPackage = totalPackage;
	}
	public int getTotalInterfaces() {
		return totalInterfaces;
	}
	public void setTotalInterfaces(int totalInterfaces) {
		this.totalInterfaces = totalInterfaces;
	}
	public int getTotalLines() {
		return totalLines;
	}
	public void setTotalLines(int totalLines) {
		this.totalLines = totalLines;
	}
	public double getAvgMethodsPerClass() {
		return avgMethodsPerClass;
	}
	public void setAvgMethodsPerClass(double avgMethodsPerClass) {
		this.avgMethodsPerClass = avgMethodsPerClass;
	}
	public double getAvgLinesPerMethod() {
		return avgLinesPerMethod;
	}
	public void setAvgLinesPerMethod(double avgLinesPerMethod) {
		this.avgLinesPerMethod = avgLinesPerMethod;
	}
	public double getAvgAttributesPerClass() {
		return avgAttributesPerClass;
	}
	public void setAvgAttributesPerClass(double avgAttributesPerClass) {
		this.avgAttributesPerClass = avgAttributesPerClass;
	}
	public List<String> getTop10MethodsClasses() {
		return top10MethodsClasses;
	}
	public void setTop10MethodsClasses(List<String> top10MethodsClasses) {
		this.top10MethodsClasses = top10MethodsClasses;
	}
	public List<String> getTop10AttributesClasses() {
		return top10AttributesClasses;
	}
	public void setTop10AttributesClasses(List<String> top10AttributesClasses) {
		this.top10AttributesClasses = top10AttributesClasses;
	}
	public List<String> getIntersectionTopClasses() {
		return intersectionTopClasses;
	}
	public void setIntersectionTopClasses(List<String> intersectionTopClasses) {
		this.intersectionTopClasses = intersectionTopClasses;
	}
	public List<String> getClassesOverXMethods() {
		return classesOverXMethods;
	}
	public void setClassesOverXMethods(List<String> classesOverXMethods) {
		this.classesOverXMethods = classesOverXMethods;
	}
	public int getMaxParameters() {
		return maxParameters;
	}
	public void setMaxParameters(int maxParameters) {
		this.maxParameters = maxParameters;
	}
	
	
    
}
