package it.davide.xml;

import patternsClasses.*;

import java.util.ArrayList;
import java.util.List;

import IFMLElements.NavigationFlow;

public class PatternEngine {
    private final List<GenericPattern> rules = List.of(
            new SearchPattern(),
            new MasterDetailPattern());

    public List<GenericPattern> detect(List<NavigationFlow> flows) {
        List<GenericPattern> result = new ArrayList<>();

        for (NavigationFlow flow : flows) {
            for (GenericPattern rule : rules) {
                if (rule.matches(flows, flow)) {
                    try {
                        GenericPattern pattern = rule.getClass().getDeclaredConstructor().newInstance();

                        pattern.setFlows(List.of(flow));

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
