package patternsClasses;

import globalGraph.*;
import globalGraph.GraphNode.FieldInfo;
import it.davide.xml.PatternInstance;
import it.davide.xml.ProjectPatternsJson;
import it.davide.xml.utilityTools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class FacetedSearchPattern extends GenericGraphPattern {

    public FacetedSearchPattern() {
        this.name = "Faceted Search Pattern";
    }

    /**
     * @param graph
     * @param startNode
     * @return List<PatternInstance>
     */
    @Override
    public List<PatternInstance> matches(IFMLGraph graph,
            GraphNode startNode) {

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

        List<PatternInstance> instances = new ArrayList<>();

        int valorizedMasterListConditions = 0;

        for (Edge formToList : graph.getOutgoing(startNode.getId())) {

            if(formToList.getType() != FlowType.NAVIGATION)
                continue;

            GraphNode masterList = graph.getNode(formToList.getTargetId());

            if (masterList == null || masterList.getType() != NodeType.LIST)
                continue;

            valorizedMasterListConditions += utilityTools.getConditionCount(formToList, masterList);

            List<GraphNode> supportingLists = new ArrayList<>();
            List<Edge> matched = new ArrayList<>();
            matched.add(formToList);

            for (Edge incoming : graph.getIncoming(masterList.getId())) {

                if(!incoming.getType().equals(FlowType.NAVIGATION))
                    continue;

                GraphNode source = graph.getNode(incoming.getSourceId());

                if (source == null)
                    continue;

                if (source.getType() == NodeType.LIST &&
                        !source.getId().equals(masterList.getId())) {

                    valorizedMasterListConditions += utilityTools.getConditionCount(incoming, masterList);
                    supportingLists.add(source);
                    matched.add(incoming);
                }
            }

            if (supportingLists.isEmpty())
                continue;

            // if the total number of conditions in the master list is greater than the number of valorized conditions, then the pattern is not valid
            if (valorizedMasterListConditions < masterList.getConditionalExpressions().values()
                    .stream()
                    .filter(Objects::nonNull)
                    .mapToInt(Set::size)
                    .sum())
                continue;

            instances.add(new PatternInstance(matched, fields, null));
        }

        return instances.isEmpty() ? null : instances;
    }

    /**
     * @param projectJson
     * @param instance
     * @param graph
     */
    @Override
    public void createJsonPattern(ProjectPatternsJson projectJson,
            PatternInstance instance,
            IFMLGraph graph) {

        ProjectPatternsJson.PatternEntry entry = new ProjectPatternsJson.PatternEntry();

        entry.patternType = name;

        entry.fields = buildFieldsEndpoint(instance.getFields());

        for (Edge edge : instance.getEdges()) {

            GraphNode from = graph.getNode(edge.getSourceId());
            GraphNode to = graph.getNode(edge.getTargetId());

            ProjectPatternsJson.FlowEntry flow = new ProjectPatternsJson.FlowEntry();

            flow.from = buildEndpoint(from);
            flow.to = buildEndpoint(to);

            for (EdgeBinding b : edge.getBindings()) {

                ProjectPatternsJson.BindingEntry jb = new ProjectPatternsJson.BindingEntry();

                jb.automaticCoupling = b.isAutomaticCoupling();

                if (!b.isAutomaticCoupling()) {
                    jb.source = b.getSourceAttribute();
                    jb.target = b.getTargetAttribute();
                }

                flow.bindings.add(jb);
            }

            entry.flows.add(flow);
        }

        projectJson.patterns.add(entry);
    }

    /**
     * @param node
     * @return Endpoint
     */
    private ProjectPatternsJson.Endpoint buildEndpoint(GraphNode node) {

        ProjectPatternsJson.Endpoint ep = new ProjectPatternsJson.Endpoint();

        ep.id = node.getId();
        ep.type = node.getType().name();
        ep.pageId = node.getPageId();
        ep.dataBinding = node.getObjectId();

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
