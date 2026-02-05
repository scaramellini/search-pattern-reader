package patternsClasses;

import java.util.List;

import IFMLElements.NavigationFlow;
import it.davide.xml.JsonPatternStructure;

public abstract class GenericPattern {
    String name;
    List<NavigationFlow> flows;

    public String getName() {
        return this.name;
    };

    public List<NavigationFlow> getFlows() {
        return this.flows;
    }

    public void setFlows(List<NavigationFlow> flows) {
        this.flows = flows;
    }

    protected String resolveElementType(String id, String originalType) {
        if (!"externalElement".equals(originalType) || id == null) {
            return originalType;
        }

        String last = id.substring(id.lastIndexOf("#") + 1);

        if (last.startsWith("act"))
            return "Action";
        if (last.startsWith("lst"))
            return "List";
        if (last.startsWith("frm"))
            return "Form";
        if (last.startsWith("fld"))
            return "Field";

        return originalType;
    }

    public abstract List<NavigationFlow> matches(List<NavigationFlow> flows, NavigationFlow current,
            List<NavigationFlow> propertiesFlows);

    public abstract void createJsonPattern(JsonPatternStructure.PagePatterns page);
}
