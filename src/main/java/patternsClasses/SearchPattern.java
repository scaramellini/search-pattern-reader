package patternsClasses;

import java.util.List;

import IFMLElements.NavigationFlow;

public class SearchPattern extends GenericPattern {

    public SearchPattern() {
        this.name = "searchPattern";
    }

    @Override
    public boolean matches(List<NavigationFlow> flows, NavigationFlow current) {
        return current.getFromElement().equals("Form")
                && current.getToElement().equals("List");
    }
}
