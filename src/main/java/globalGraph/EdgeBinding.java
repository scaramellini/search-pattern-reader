package globalGraph;

//class representing the binding between two attributes of the source and target components
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

    /** 
     * @return boolean
     */
    public boolean isAutomaticCoupling() {
        return automaticCoupling;
    }

    /** 
     * @return String
     */
    public String getSourceAttribute() {
        return sourceAttribute;
    }

    /** 
     * @return String
     */
    public String getTargetAttribute() {
        return targetAttribute;
    }
}
