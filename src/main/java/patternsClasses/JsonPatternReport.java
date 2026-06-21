package patternsClasses;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON report structure for functional patterns
 */
public class JsonPatternReport {
    public String patternType;
    public String sourceComponentId;
    public String actionId;
    public String webviewId;
    public List<String> details = new ArrayList<>();

    public JsonPatternReport() {
    }

    public JsonPatternReport(String patternType, String sourceComponentId, String actionId, String webviewId) {
        this.patternType = patternType;
        this.sourceComponentId = sourceComponentId;
        this.actionId = actionId;
        this.webviewId = webviewId;
    }

    public void addDetail(String detail) {
        details.add(detail);
    }

    @Override
    public String toString() {
        return "JsonPatternReport{" +
                "patternType='" + patternType + '\'' +
                ", sourceComponentId='" + sourceComponentId + '\'' +
                ", actionId='" + actionId + '\'' +
                ", webviewId='" + webviewId + '\'' +
                ", details=" + details +
                '}';
    }
}
