package it.davide.xml;

import java.util.ArrayList;
import java.util.List;

public class ProjectPatternsJson {

    public List<PatternEntry> patterns = new ArrayList<>();

    public static class PatternEntry {
        public String patternType;
        public List<FlowEntry> flows = new ArrayList<>();
    }

    public static class FlowEntry {
        public Endpoint from;
        public Endpoint to;
        public List<BindingEntry> bindings = new ArrayList<>();
    }

    public static class BindingEntry {
        public boolean automaticCoupling;
        public String source;
        public String target;
    }

    public static class Endpoint {
        public String id;
        public String type;
        public String pageId;
    }
}
