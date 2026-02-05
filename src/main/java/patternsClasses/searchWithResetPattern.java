package patternsClasses;

import java.util.ArrayList;
import java.util.List;

import IFMLElements.Binding;
import IFMLElements.NavigationFlow;
import it.davide.xml.JsonPatternStructure;
import it.davide.xml.JsonPatternStructure.FilterBinding;
import it.davide.xml.JsonPatternStructure.Flow;
import it.davide.xml.JsonPatternStructure.PagePatterns;

public class searchWithResetPattern extends GenericPattern {

    public searchWithResetPattern() {
        this.name = "Search with reset pattern";
    }

    @Override
    public List<NavigationFlow> matches(List<NavigationFlow> flows, NavigationFlow current,
            List<NavigationFlow> propertiesFlows) {
        List<NavigationFlow> matchingFlows = new ArrayList<NavigationFlow>();

        if (current.getFromElement().equals("Form") && current.getToElement().equals("List")) {
            flows.stream()
                    .filter(f1 -> f1.getFromId().equals(current.getFromId()) &&
                            f1.getToId() != null &&
                            f1.getToId()
                                    .substring(f1.getToId().lastIndexOf("#") + 1)
                                    .startsWith("act"))
                    .flatMap(f1 -> propertiesFlows.stream()
                            .filter(f2 -> f2.getFromId().equals(f1.getToId()) &&
                                    f2.getToId().equals(current.getToId()))
                            .map(f2 -> {
                                matchingFlows.addAll(List.of(current, f1, f2));
                                return matchingFlows;
                            }))
                    .findAny();

            return matchingFlows;
        }
        return null;
    }

    @Override
    public void createJsonPattern(PagePatterns page) {
         JsonPatternStructure.FlowPattern pattern = new JsonPatternStructure.FlowPattern();
        pattern.patternType = name;

        getFlows().forEach(flow -> {
            JsonPatternStructure.Endpoint from = new JsonPatternStructure.Endpoint();
            from.id = flow.getFromId();
            from.type = flow.getFromElement();

            JsonPatternStructure.Endpoint to = new JsonPatternStructure.Endpoint();
            to.id = flow.getToId();
            to.type = resolveElementType(flow.getToId(), flow.getToElement());

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
        });

        page.patterns.add(pattern);
    }
}
