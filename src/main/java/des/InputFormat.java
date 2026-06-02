package des;

public enum InputFormat {
    BASE64("Base64"),
    HEX("Hex");

    private final String label;

    InputFormat(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
