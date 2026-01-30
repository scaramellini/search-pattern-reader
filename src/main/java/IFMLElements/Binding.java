package IFMLElements;

public class Binding {
    private String fromAttribute;
    private String toAttribute;
    private boolean automaticCoupling;

    public Binding(String fromAttribute, String toAttribute, boolean automaticCoupling) {
        this.fromAttribute = fromAttribute;
        this.toAttribute = toAttribute;
        this.automaticCoupling = automaticCoupling;
    }

    public String getFromAttribute() {
        return fromAttribute;
    }

    public String getToAttribute() {
        return toAttribute;
    }

    public boolean isAutomaticCoupling() {
        return automaticCoupling;
    }
}
