package patternsClasses;

import globalGraph.*;
import it.davide.xml.PatternInstance;
import it.davide.xml.ProjectPatternsJson;

import java.util.ArrayList;
import java.util.List;

public class DataLookupPattern extends GenericGraphPattern {

    private MasterDetailPattern masterDetailPattern =
            new MasterDetailPattern();

    public DataLookupPattern() {
        this.name = "Data Lookup Pattern";
    }

    @Override
    public List<PatternInstance> matches(IFMLGraph graph,
                                         GraphNode startNode) {

        if (startNode.getType() != NodeType.FORM)
            return null;

        List<PatternInstance> instances = new ArrayList<>();

        // FORM → LIST
        for (Edge formToList : graph.getOutgoing(startNode.getId())) {

            GraphNode listNode =
                    graph.getNode(formToList.getTargetId());

            if (listNode == null ||
                listNode.getType() != NodeType.LIST)
                continue;

            // FORM e LIST devono essere su pagine diverse
            if (startNode.getPageId()
                    .equals(listNode.getPageId()))
                continue;

            // 🔥 Riutilizzo MasterDetailPattern
            List<PatternInstance> mdInstances =
                    masterDetailPattern.matches(graph, listNode);

            if (mdInstances == null)
                continue;

            for (PatternInstance mdInstance : mdInstances) {

                // MasterDetail ha un solo edge LIST → DETAILS
                Edge listToDetails =
                        mdInstance.getEdges().get(0);

                GraphNode detailsNode =
                        graph.getNode(
                                listToDetails.getTargetId());

                if (detailsNode == null)
                    continue;

                // LIST e DETAILS devono stare sulla stessa pagina
                if (!listNode.getPageId()
                        .equals(detailsNode.getPageId()))
                    continue;

                // DETAILS → FORM (ritorno lookup)
                for (Edge detailsToForm :
                        graph.getOutgoing(detailsNode.getId())) {

                    GraphNode returnTarget =
                            graph.getNode(
                                    detailsToForm.getTargetId());

                    if (returnTarget == null)
                        continue;

                    if (!returnTarget.getId()
                            .equals(startNode.getId()))
                        continue;

                    // Deve valorizzare almeno un campo
                    if (!hasNonAutomaticBinding(detailsToForm))
                        continue;

                    List<Edge> matched = new ArrayList<>();

                    matched.add(formToList);
                    matched.add(listToDetails);
                    matched.add(detailsToForm);

                    instances.add(new PatternInstance(matched));
                }
            }
        }

        return instances.isEmpty() ? null : instances;
    }

    private boolean hasNonAutomaticBinding(Edge edge) {

        for (EdgeBinding b : edge.getBindings()) {
            if (!b.isAutomaticCoupling())
                return true;
        }

        return false;
    }

    // =====================================================
    // JSON EXPORT
    // =====================================================

    @Override
    public void createJsonPattern(ProjectPatternsJson projectJson,
                                  PatternInstance instance,
                                  IFMLGraph graph) {

        ProjectPatternsJson.PatternEntry entry =
                new ProjectPatternsJson.PatternEntry();

        entry.patternType = name;

        for (Edge edge : instance.getEdges()) {

            GraphNode from =
                    graph.getNode(edge.getSourceId());
            GraphNode to =
                    graph.getNode(edge.getTargetId());

            ProjectPatternsJson.FlowEntry flow =
                    new ProjectPatternsJson.FlowEntry();

            flow.from = buildEndpoint(from);
            flow.to = buildEndpoint(to);

            for (EdgeBinding b : edge.getBindings()) {

                ProjectPatternsJson.BindingEntry jsonBinding =
                        new ProjectPatternsJson.BindingEntry();

                jsonBinding.automaticCoupling =
                        b.isAutomaticCoupling();

                if (!b.isAutomaticCoupling()) {
                    jsonBinding.source =
                            b.getSourceAttribute();
                    jsonBinding.target =
                            b.getTargetAttribute();
                }

                flow.bindings.add(jsonBinding);
            }

            entry.flows.add(flow);
        }

        projectJson.patterns.add(entry);
    }

    private ProjectPatternsJson.Endpoint buildEndpoint(GraphNode node) {

        ProjectPatternsJson.Endpoint ep =
                new ProjectPatternsJson.Endpoint();

        ep.id = node.getId();
        ep.type = node.getType().name();
        ep.pageId = node.getPageId();

        return ep;
    }
}