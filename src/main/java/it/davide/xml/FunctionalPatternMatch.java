package it.davide.xml;

/**
 * Represents a detected functional pattern match
 */
public class FunctionalPatternMatch {
    private final String patternName;
    private final String sourceComponentId;
    private final String actionId;
    private final String webviewId;

    public FunctionalPatternMatch(String patternName, String sourceComponentId, String actionId, String webviewId) {
        this.patternName = patternName;
        this.sourceComponentId = sourceComponentId;
        this.actionId = actionId;
        this.webviewId = webviewId;
    }

    public String getPatternName() {
        return patternName;
    }

    public String getSourceComponentId() {
        return sourceComponentId;
    }

    public String getActionId() {
        return actionId;
    }

    public String getWebviewId() {
        return webviewId;
    }

    @Override
    public String toString() {
        return "FunctionalPatternMatch{" +
                "patternName='" + patternName + '\'' +
                ", sourceComponentId='" + sourceComponentId + '\'' +
                ", actionId='" + actionId + '\'' +
                ", webviewId='" + webviewId + '\'' +
                '}';
    }
}
