package mmmfrieddough.craftpilot.model;

public class Request {
    private String platform;
    private int version_number;
    private double temperature;
    private int start_radius;
    private int max_iterations;
    private int max_blocks;
    private float air_probability_iteration_scaling;
    private String[][][] structure;

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

    public float getAir_probability_iteration_scaling() {
        return air_probability_iteration_scaling;
    }

    public void setAir_probability_iteration_scaling(float airProbabilityIterationScaling) {
        this.air_probability_iteration_scaling = airProbabilityIterationScaling;
    }

    public String[][][] getStructure() {
        return structure;
    }

    public void setStructure(String[][][] structure) {
        this.structure = structure;
    }
}
