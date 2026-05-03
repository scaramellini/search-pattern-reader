package it.davide.xml;

import java.util.ArrayList;
import java.util.List;


// class representing the json object that will be used to store the detected patterns in the IFML model
// it contains a list of pattern entries, each entry represents a detected pattern and contains the information about the flows and bindings that matched the pattern rules
public class ProjectPatternsJson {

    public List<PatternEntry> patterns = new ArrayList<>();

    public static class PatternEntry {
        public String patternType;
        public List<FlowEntry> flows = new ArrayList<>();
        public List<FieldEndpoint> fields;
    }

    public static class SingleComponentPatternEntry extends PatternEntry {
        public String patternType;
        public Endpoint component;
        public List<FieldEndpoint> fields;
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
        public String dataBinding;
    }

    public static class FieldEndpoint {
        public String fieldId;
        public String valueAttribute;
        public String valueAssociationRole;
    }
}
