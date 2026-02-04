package it.davide.xml;

import patternsClasses.*;

import java.util.ArrayList;
import java.util.List;

import IFMLElements.NavigationFlow;
import it.davide.xml.JsonPatternStructure.PagePatterns;

public class PatternEngine {
    //list of pattern to search from
    private final List<GenericPattern> rules = List.of(
            new SearchPattern(),
            new MasterDetailPattern());

    public List<GenericPattern> detectPatterns(List<NavigationFlow> flows, PagePatterns page) {
        List<GenericPattern> result = new ArrayList<>();

        for (NavigationFlow flow : flows) {
            for (GenericPattern rule : rules) {
                if (rule.matches(flows, flow)) { //check if the pattern matches
                    try {
                        GenericPattern pattern = rule.getClass().getDeclaredConstructor().newInstance();

                        pattern.setFlows(List.of(flow));
                        pattern.createJsonPattern(page);

                        result.add(pattern);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return result;
    }
}
