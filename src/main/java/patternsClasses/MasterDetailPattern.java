package patternsClasses;

import java.util.List;

import IFMLElements.NavigationFlow;

public class MasterDetailPattern extends GenericPattern {
    public MasterDetailPattern() {
        this.name = "masterDetailPattern";
    }

    @Override
    public boolean matches(List<NavigationFlow> flows, NavigationFlow current) {
        return current.getFromElement().equals("List")
                && current.getToElement().equals("Details");
    }
}
