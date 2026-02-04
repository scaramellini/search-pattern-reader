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

    public abstract List<NavigationFlow> matches(List<NavigationFlow> flows, NavigationFlow current);

    public abstract void createJsonPattern(JsonPatternStructure.PagePatterns page);
}
