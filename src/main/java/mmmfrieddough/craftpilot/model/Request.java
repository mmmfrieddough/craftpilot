package mmmfrieddough.craftpilot.model;

import java.util.Map;

public class Request {
    private String platform;
    private int version_number;
    private double temperature;
    private int start_radius;
    private int max_iterations;
    private int max_blocks;
    private int max_alternatives;
    private float min_alternative_probability;
    private int[][][] structure;
    private Map<Integer, String> palette;

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public int getVersion_number() {
        return version_number;
    }

    public void setVersion_number(int versionNumber) {
        this.version_number = versionNumber;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getStart_radius() {
        return start_radius;
    }

    public void setStart_radius(int startRadius) {
        this.start_radius = startRadius;
    }

    public int getMax_iterations() {
        return max_iterations;
    }

    public void setMax_iterations(int maxIterations) {
        this.max_iterations = maxIterations;
    }

    public int getMax_blocks() {
        return max_blocks;
    }

    public void setMax_blocks(int maxBlocks) {
        this.max_blocks = maxBlocks;
    }

    public int getMax_alternatives() {
        return max_alternatives;
    }

    public void setMax_alternatives(int maxAlternatives) {
        this.max_alternatives = maxAlternatives;
    }

    public float getMin_alternative_probability() {
        return min_alternative_probability;
    }

    public void setMin_alternative_probability(float minAlternativeProbability) {
        this.min_alternative_probability = minAlternativeProbability;
    }

    public int[][][] getStructure() {
        return structure;
    }

    public void setStructure(int[][][] structure) {
        this.structure = structure;
    }

    public Map<Integer, String> getPalette() {
        return palette;
    }

    public void setPalette(Map<Integer, String> palette) {
        this.palette = palette;
    }
}
