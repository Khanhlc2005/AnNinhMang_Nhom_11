package des;

public enum InputFormat {
    TEXT("Văn bản"),
    HEX("Hex"),
    BASE64("Base64");

    private final String label;

    InputFormat(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
