package it.davide.xml;

import java.util.ArrayList;
import java.util.List;

public class JsonPatternStructure {

    public static class JsonReport {
        public List<PagePatterns> pages = new ArrayList<>();
    }

    public static class FilterBinding {
        public String source;
        public String target;
        public Boolean automaticCoupling;
    }

    public static class Endpoint {
        public String id;
        public String type;
    }

    public static class FlowPattern {
        public String patternType;
        public Endpoint from;
        public Endpoint to;
        public List<FilterBinding> bindings = new ArrayList<>();
    }

    public static class PagePatterns {
        public String pageId;
        public List<FlowPattern> patterns = new ArrayList<>();
    }
}
