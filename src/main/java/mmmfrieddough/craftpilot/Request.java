package mmmfrieddough.craftpilot;

public class Request {
    private String[][][] structure;
    private double temperature;

    // Getters and setters for 'structure' and 'temperature'
    public String[][][] getStructure() {
        return structure;
    }

    public void setStructure(String[][][] structure) {
        this.structure = structure;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
}
