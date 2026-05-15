package patternsClasses;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import globalGraph.GraphNode;
import globalGraph.GraphNode.FieldInfo;
import globalGraph.IFMLGraph;
import globalGraph.NodeType;
import it.davide.xml.PatternInstance;
import it.davide.xml.ProjectPatternsJson;

public class MultiFieldFormPattern extends GenericGraphPattern {

    public MultiFieldFormPattern() {
        this.name = "Multi-Field Form Pattern";
    }

    @Override
    public List<PatternInstance> matches(IFMLGraph graph, GraphNode startNode) {
        if (startNode.getType() != NodeType.FORM)
            return null;

        Set<GraphNode.FieldInfo> fields = new HashSet<>();

        if (startNode != null && startNode.getFieldElementIds() != null) {
            for (Set<GraphNode.FieldInfo> set : startNode.getFieldElementIds().values()) {
                if (set != null) {
                    fields.addAll(set);
                }
            }
        }

        if (fields.size() == 0)
            return null;

        for (FieldInfo field : fields) {
            if ((field.getValueAttribute() == null || field.getValueAttribute().isEmpty())
                    && (field.getValueAssociationAttribute() == null
                            || field.getValueAssociationAttribute().isEmpty())) {
                return null; // If any field lacks both value attributes, the pattern does not match
            }
        }

        return List.of(new PatternInstance(null, fields, startNode)); // Return a dummy PatternInstance to indicate a
                                                                      // match
    }

    @Override
    public void createJsonPattern(ProjectPatternsJson projectJson, PatternInstance instance, IFMLGraph graph) {
        ProjectPatternsJson.SingleComponentPatternEntry entry = new ProjectPatternsJson.SingleComponentPatternEntry();

        entry.patternType = getPatternTypeWithVariant(name, graph, instance,
                null, "Multifield Form Dialog Variant Pattern");
        entry.component = buildSingleEndpoint(instance.getSingleNode());

        entry.fields = buildFieldsEndpoint(instance.getFields());

        projectJson.patterns.add(entry);

    }

    /**
     * @param node
     * @return Endpoint
     */
    private ProjectPatternsJson.Endpoint buildSingleEndpoint(GraphNode node) {

        ProjectPatternsJson.Endpoint ep = new ProjectPatternsJson.Endpoint();

        ep.id = node.getId();
        ep.type = node.getType().name();
        ep.pageId = node.getPageId();
        ep.dataBinding = node.getObjectId();
        ep.isInDialogPage = node.isInDialogPage();

        return ep;
    }

    /**
     * @param node
     * @return Endpoint
     */
    private List<ProjectPatternsJson.FieldEndpoint> buildFieldsEndpoint(Set<FieldInfo> fields) {

        List<ProjectPatternsJson.FieldEndpoint> fieldEndpoints = new ArrayList<ProjectPatternsJson.FieldEndpoint>();

        for (FieldInfo field : fields) {

            ProjectPatternsJson.FieldEndpoint ep = new ProjectPatternsJson.FieldEndpoint();

            ep.fieldId = field.getId();
            ep.valueAttribute = field.getValueAttribute();
            ep.valueAssociationRole = field.getValueAssociationAttribute();
            fieldEndpoints.add(ep);
        }

        return fieldEndpoints;
    }
}
