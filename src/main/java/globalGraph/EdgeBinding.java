package globalGraph;

public class EdgeBinding {

    private final boolean automaticCoupling;
    private final String sourceAttribute;
    private final String targetAttribute;

    public EdgeBinding(boolean automaticCoupling,
                       String sourceAttribute,
                       String targetAttribute) {

        this.automaticCoupling = automaticCoupling;
        this.sourceAttribute = sourceAttribute;
        this.targetAttribute = targetAttribute;
    }

    public boolean isAutomaticCoupling() {
        return automaticCoupling;
    }

    public String getSourceAttribute() {
        return sourceAttribute;
    }

    public String getTargetAttribute() {
        return targetAttribute;
    }
}
