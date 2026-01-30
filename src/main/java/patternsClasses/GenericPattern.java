package patternsClasses;

import java.util.List;

import IFMLElements.NavigationFlow;

public abstract class GenericPattern {
    String name;
    private List<NavigationFlow> flows;

    public String getName() {
        return this.name;
    };

    public List<NavigationFlow> getFlows() {
        return this.flows;
    }

    public void setFlows(List<NavigationFlow> flows) {
        this.flows = flows;
    }

    public abstract boolean matches(List<NavigationFlow> flows, NavigationFlow current);
}
