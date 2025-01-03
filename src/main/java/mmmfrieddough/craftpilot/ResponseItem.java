package mmmfrieddough.craftpilot;

import java.util.List;

public class ResponseItem {
    private String value;
    private List<Integer> position;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<Integer> getPosition() {
        return position;
    }

    public void setPosition(List<Integer> position) {
        this.position = position;
    }
}
