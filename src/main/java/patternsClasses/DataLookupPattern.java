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
import java.util.stream.Stream;

public class DataLookupPattern extends GenericGraphPattern {

    private MasterDetailPattern masterDetailPattern = new MasterDetailPattern();

    private MasterMultiDetailsPattern masterMultiDetailsPattern = new MasterMultiDetailsPattern();

    private MultiLevelMasterDetailPattern multiLevelMasterDetailPattern = new MultiLevelMasterDetailPattern();

    public DataLookupPattern() {
        this.name = "Data Lookup Pattern";
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

        List<PatternInstance> instances = new ArrayList<>();

        int preloadedFieldsCount = 0;

        for (Edge formToList : graph.getOutgoing(startNode.getId())) {

            if(formToList.getType() != FlowType.NAVIGATION)
                continue;

            GraphNode listNode = graph.getNode(formToList.getTargetId());

            if (listNode == null ||
                    listNode.getType() != NodeType.LIST)
                continue;

            List<PatternInstance> mdInstances = Stream.of(
                    masterDetailPattern.matches(graph, listNode),
                    masterMultiDetailsPattern.matches(graph, listNode),
                    multiLevelMasterDetailPattern.matches(graph, listNode))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            if (mdInstances == null)
                continue;

            for (PatternInstance mdInstance : mdInstances) {

                Edge listToDetails = mdInstance.getEdges().get(mdInstance.getEdges().size() - 1);

                GraphNode detailsNode = graph.getNode(
                        listToDetails.getTargetId());

                if (detailsNode == null)
                    continue;

                for (Edge detailsToForm : graph.getOutgoing(detailsNode.getId())) {

                    if(detailsToForm.getType() != FlowType.NAVIGATION)
                        continue;

                    GraphNode returnTarget = graph.getNode(
                            detailsToForm.getTargetId());

                    if (returnTarget == null)
                        continue;

                    if (!returnTarget.getId()
                            .equals(startNode.getId()))
                        continue;

                    Set<GraphNode.FieldInfo> fields = new HashSet<>();

                    if (startNode != null && startNode.getFieldElementIds() != null) {
                        for (Set<GraphNode.FieldInfo> set : startNode.getFieldElementIds().values()) {
                            if (set != null) {
                                fields.addAll(set);
                            }
                        }
                    }

                    Set<String> fieldIds = utilityTools.extractAllFields(startNode);

                    for (EdgeBinding binding : detailsToForm.getBindings()) {

                        String targetAttr = binding.getTargetAttribute();

                        if (targetAttr == null)
                            continue;

                        if (fieldIds.contains(utilityTools.extractFieldId(targetAttr))) {
                            preloadedFieldsCount++;
                        }
                    }

                    List<Edge> matched = new ArrayList<>();

                    if (preloadedFieldsCount > 0) {
                        matched.add(formToList);
                        matched.addAll(mdInstance.getEdges());
                        matched.add(detailsToForm);

                        instances.add(new PatternInstance(matched, fields, null));
                    }
                }
            }
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

                ProjectPatternsJson.BindingEntry jsonBinding = new ProjectPatternsJson.BindingEntry();

                jsonBinding.automaticCoupling = b.isAutomaticCoupling();

                if (!b.isAutomaticCoupling()) {
                    jsonBinding.source = b.getSourceAttribute();
                    jsonBinding.target = b.getTargetAttribute();
                }

                flow.bindings.add(jsonBinding);
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