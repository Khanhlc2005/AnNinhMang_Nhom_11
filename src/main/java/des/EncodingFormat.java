package des;

public enum EncodingFormat {
    BASE64("Base64"),
    HEX("Hex");

    private final String label;

    EncodingFormat(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
