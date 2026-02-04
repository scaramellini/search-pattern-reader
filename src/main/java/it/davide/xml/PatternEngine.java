package it.davide.xml;

import patternsClasses.*;

import java.util.ArrayList;
import java.util.List;

import IFMLElements.NavigationFlow;
import it.davide.xml.JsonPatternStructure.PagePatterns;

public class PatternEngine {
    //list of pattern to search from
    private final List<GenericPattern> rules = List.of(
            new basicSearchPattern(),
            new multicriteriaSearchPattern(),
            new facetedSearchPattern(),
            new quickSearchPattern(),
            new MasterDetailPattern());

    public List<GenericPattern> detectPatterns(List<NavigationFlow> flows, PagePatterns page) {
        List<GenericPattern> result = new ArrayList<>();

        for (NavigationFlow flow : flows) {
            for (GenericPattern rule : rules) {
                //check if the pattern matches
                List<NavigationFlow> matchingFlows = rule.matches(flows, flow);
                if (matchingFlows != null && !matchingFlows.isEmpty()) { 
                    try {
                        GenericPattern pattern = rule.getClass().getDeclaredConstructor().newInstance();

                        pattern.setFlows(matchingFlows);
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
