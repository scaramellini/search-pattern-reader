package patternsClasses.old_pattern;

import java.util.List;

import IFMLElements.Binding;
import IFMLElements.NavigationFlow;
import it.davide.xml.JsonPatternStructure;
import it.davide.xml.JsonPatternStructure.FilterBinding;
import it.davide.xml.JsonPatternStructure.Flow;
import it.davide.xml.JsonPatternStructure.PagePatterns;

public class multicriteriaSearchPattern extends GenericPattern {

    public multicriteriaSearchPattern() {
        this.name = "Multicriteria search pattern";
    }

    @Override
    public List<NavigationFlow> matches(List<NavigationFlow> flows, NavigationFlow current, List<NavigationFlow> propertiesFlows) {
        if (current.getFromElement().equals("Form") && current.getToElement().equals("List")) {
            if (current.getBindings().size() > 1) {
                return List.of(current);
            }
        }
        return null;
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

        Flow f = new Flow();
        f.from = from;
        f.to = to;

        for (Binding binding : flow.getBindings()) {
            FilterBinding b = new FilterBinding();

            if (binding.isAutomaticCoupling()) {
                b.automaticCoupling = true;
            } else {
                b.source = binding.getFromAttribute();
                b.target = binding.getToAttribute();
            }

            f.bindings.add(b);
        }

        pattern.flows.add(f);

        page.patterns.add(pattern);
    }
}
