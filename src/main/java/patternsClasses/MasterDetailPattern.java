package patternsClasses;

import java.util.List;

import IFMLElements.Binding;
import IFMLElements.NavigationFlow;
import it.davide.xml.JsonPatternStructure;
import it.davide.xml.JsonPatternStructure.FilterBinding;
import it.davide.xml.JsonPatternStructure.PagePatterns;

public class MasterDetailPattern extends GenericPattern {
    public MasterDetailPattern() {
        this.name = "masterDetailPattern";
    }

    @Override
    public boolean matches(List<NavigationFlow> flows, NavigationFlow current) {
        return current.getFromElement().equals("List")
                && current.getToElement().equals("Details");
    }

    @Override
    public void createJsonPattern(PagePatterns page) {
        JsonPatternStructure.FlowPattern pattern = new JsonPatternStructure.FlowPattern();
        pattern.patternType = name;

        NavigationFlow flow = flows.get(0);

        JsonPatternStructure.Endpoint from = new JsonPatternStructure.Endpoint();
        from.id = flow.getFromId();
        from.type = flow.getFromElement();

        JsonPatternStructure.Endpoint to = new JsonPatternStructure.Endpoint();
        to.id = flow.getToId();
        to.type = flow.getToElement();
        pattern.from = from;
        pattern.to = to;

        for (Binding binding : flow.getBindings()) {
            FilterBinding b = new FilterBinding();

            if (binding.isAutomaticCoupling()) {
                b.automaticCoupling = true;
            } else {
                b.source = binding.getFromAttribute();
                b.target = binding.getToAttribute();
            }

            pattern.bindings.add(b);
        }

        page.patterns.add(pattern);
    }
}
