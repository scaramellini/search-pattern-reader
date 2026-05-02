package it.davide.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.*;

import globalGraph.Edge;
import globalGraph.EdgeBinding;
import globalGraph.FlowType;
import globalGraph.IFMLGraph;
import globalGraph.GraphNode;
import globalGraph.NodeType;

//class responsible for extracting the components and flows from the IFML model to build the global graph that will be used for pattern detection
public class NewIFMLPatternExtractor {

    public class ComponentInfo {
        private String id;
        private String type;
        private String objectId;
        private Map<GraphNode.FieldElementCategory, Set<String>> fieldElementIds = new HashMap<>();
        private Map<GraphNode.ConditionalExpressionCategory, Set<String>> conditionalExpressions = new HashMap<>();


        public ComponentInfo(String id, String type, String objectId,
                Map<GraphNode.FieldElementCategory, Set<String>> fieldElementIds,
                Map<GraphNode.ConditionalExpressionCategory, Set<String>> conditionalExpressions) {
            this.id = id;
            this.type = type;
            this.objectId = objectId;
            this.fieldElementIds = fieldElementIds;
            this.conditionalExpressions = conditionalExpressions;
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public String getObjectId() {
            return objectId;
        }

        public Map<GraphNode.FieldElementCategory, Set<String>> getFieldElementIds() {
            return fieldElementIds;
        }

        public Map<GraphNode.ConditionalExpressionCategory, Set<String>> getConditionalExpressions() {
            return conditionalExpressions;
        }
    }

    private static final List<String> SOURCE_ATTRS = List.of(
            "source",
            "sourceValue",
            "sourceParameter",
            "sourceImplicitParameter",
            "sourceParameterBinding",
            "blank");

    private static final List<String> TARGET_ATTRS = List.of(
            "target",
            "targetValue",
            "targetParameter",
            "targetImplicitParameter",
            "targetParameterBinding",
            "targetExpressionVariable",
            "blank");

    private static final List<String> DATA_BINDINGS = List.of(
            "class",
            "classServiceRes");

    private static List<String> navFlowParentElements = Arrays.asList("Form", "List", "Details", "Hierarchy",
            "ViewComponent");

    /**
     * collect the direct components under a view component
     * 
     * @param viewComponents
     * @param components
     */
    private void collectDirectComponents(Element viewComponents, List<ComponentInfo> components) {

        NodeList children = viewComponents.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeType() == Node.ELEMENT_NODE) {

                Element element = (Element) child;

                String id = element.getAttribute("id");

                NodeList dataBinding = element.getElementsByTagName("DataBinding");

                Element dataBindingEl = (Element) dataBinding.item(0);

                String objectId = dataBindingEl != null ? resolveAttribute(dataBindingEl, DATA_BINDINGS) : null;

                Map<GraphNode.FieldElementCategory, Set<String>> fieldElementIds = new HashMap<>();
                Map<GraphNode.ConditionalExpressionCategory, Set<String>> conditionalExpressions = new HashMap<>();

                fieldElementIds.put(GraphNode.FieldElementCategory.Field, extractFieldIds(element, "Field"));
                fieldElementIds.put(GraphNode.FieldElementCategory.SelectionField, extractFieldIds(element, "SelectionField"));

                conditionalExpressions.put(GraphNode.ConditionalExpressionCategory.associationCondition, extractComponentConditions(element, "AssociationRoleCondition"));
                conditionalExpressions.put(GraphNode.ConditionalExpressionCategory.attributeConditions, extractComponentConditions(element, "AttributesCondition"));
                conditionalExpressions.put(GraphNode.ConditionalExpressionCategory.keyCondition, extractComponentConditions(element, "KeyCondition"));

                if (id != null && !id.isEmpty()) {
                    components.add(new ComponentInfo(id, element.getNodeName(), objectId,
                            fieldElementIds, conditionalExpressions));
                }
            }
        }
    }

    /**
     * extract the view components as couples of (id, type) from the IFML document
     * 
     * @param document
     * @return List<ComponentInfo>
     */
    public List<ComponentInfo> getViewComponents(Document document) {

        List<ComponentInfo> components = new ArrayList<>();

        Element root = document.getDocumentElement(); // <Page>

        // get view components directly under the page tag
        NodeList pageChildren = root.getChildNodes();

        // for each viewcomponent tag it gets the actual components (Form, List...)
        // directly under it and adds them to the list of components
        for (int i = 0; i < pageChildren.getLength(); i++) {
            Node node = pageChildren.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE &&
                    node.getNodeName().equals("ViewComponents")) {

                collectDirectComponents((Element) node, components);
            }
        }

        // also get the components inside dialog pages, they are not directly under the
        // page tag but they are still part of the model and can be involved in patterns
        NodeList dialogPages = root.getElementsByTagName("DialogPage");

        for (int i = 0; i < dialogPages.getLength(); i++) {
            Element dialogPage = (Element) dialogPages.item(i);

            NodeList dialogChildren = dialogPage.getChildNodes();

            for (int j = 0; j < dialogChildren.getLength(); j++) {
                Node node = dialogChildren.item(j);

                if (node.getNodeType() == Node.ELEMENT_NODE &&
                        node.getNodeName().equals("ViewComponents")) {

                    collectDirectComponents((Element) node, components);
                }
            }
        }

        return components;
    }

    /**
     * read the page files and extract the components to create the nodes of the
     * global graph
     * 
     * @param pagePaths
     * @param graph
     * @throws Exception
     */
    private void extractNodes(List<String> pagePaths, IFMLGraph graph) throws Exception {

        for (String pagePath : pagePaths) {

            String pageId = pagePath.substring(pagePath.lastIndexOf("\\") + 1);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc = builder.parse(new File(pagePath));

            getViewComponents(doc).forEach(component -> {
                GraphNode node = new GraphNode(component.getId(), resolveNodeType(component.getType()), pageId,
                        component.getObjectId(),
                        component.getFieldElementIds(),
                        component.getConditionalExpressions());

                graph.addNode(node);
            });
        }
    }

    /**
     * resolve the type of the component to create the node of the graph, if the
     * component is not recognized it will be classified as UNKNOWN
     * 
     * @param component
     * @return NodeType
     */
    private NodeType resolveNodeType(String component) {

        if (component.equals("Form"))
            return NodeType.FORM;
        if (component.equals("Details"))
            return NodeType.DETAILS;
        if (component.equals("List"))
            return NodeType.LIST;
        if (component.equals("ViewContainer"))
            return NodeType.VIEW_CONTAINER;
        if (component.equals("Hierarchy"))
            return NodeType.HIERARCHY;

        return NodeType.UNKNOWN;
    }

    /**
     * retrieve the source component id of a flow by traversing upward the parent
     * nodes of the flow element until it finds a node that is a direct child of a
     * view component
     * if it doesn't find it returns null
     * 
     * @param flow
     * @return String
     */
    private static String findSource(Element flow) {
        Node current = flow.getParentNode();

        while (current != null && current.getNodeType() == Node.ELEMENT_NODE) {
            Element el = (Element) current;

            if (navFlowParentElements.contains(el.getLocalName())) {
                return el.getAttribute("id");
            }

            current = current.getParentNode();
        }

        return null;
    }

    /**
     * check if the parameter binding is a passing binding
     * 
     * @param el
     * @return boolean
     */
    private static boolean isPassing(Element el) {
        String val = el.getAttribute("passing");
        return "true".equals(val);
    }

    /**
     * gets the name of the source and target attributes of a parameter binding
     * 
     * @param el         element representing the parameter binding
     * @param candidates list of possible attribute names for the source and target
     *                   attributes of the parameter binding
     * @return String
     */
    private static String resolveAttribute(Element el, List<String> candidates) {
        NamedNodeMap attrs = el.getAttributes();

        for (String name : candidates) {
            Node attr = attrs.getNamedItem(name);
            if (attr != null) {
                return attr.getNodeValue();
            }
        }
        return null;
    }

    private Set<String> extractComponentConditions(Element componentElement, String conditionType) {

        NodeList condNodes = componentElement.getElementsByTagNameNS("*", conditionType);

        if (condNodes.getLength() == 0)
            return null;

        Set<String> conditionIds = new HashSet<>();

        for (int j = 0; j < condNodes.getLength(); j++) {
            Element condEl = (Element) condNodes.item(j);

            String condId = condEl.getAttribute("id");
            if (condId != null && !condId.isEmpty()) {
                conditionIds.add(condId);
            }
        }

        return conditionIds;
    }

    private Set<String> extractFieldIds(Element componentElement, String tagName) {
        NodeList fieldNodes = componentElement.getElementsByTagNameNS("*", tagName);

        if (fieldNodes.getLength() == 0)
            return null;

        Set<String> fieldIds = new HashSet<>();

        for (int i = 0; i < fieldNodes.getLength(); i++) {
            Element fieldEl = (Element) fieldNodes.item(i);
            String fieldId = fieldEl.getAttribute("id");
            if (fieldId != null && !fieldId.isEmpty()) {
                fieldIds.add(fieldId);
            }
        }

        return fieldIds.isEmpty() ? null : fieldIds;
    }

    /**
     * read the page files and extract the flows to create the edges of the global
     * graph
     * 
     * @param pagePaths
     * @param graph
     * @throws Exception
     */
    private void extractEdges(List<String> pagePaths, IFMLGraph graph) throws Exception {

        for (String pagePath : pagePaths) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();

            try {
                Document doc = builder.parse(new File(pagePath));

                // gets all the navigation and data flows in the page
                NodeList navNodeList = doc.getElementsByTagNameNS("*", "NavigationFlow");
                NodeList dataNodeList = doc.getElementsByTagNameNS("*", "DataFlow");

                List<Element> documentFlows = new ArrayList<>();
                for (int i = 0; i < navNodeList.getLength(); i++) {
                    documentFlows.add((Element) navNodeList.item(i));
                }
                for (int i = 0; i < dataNodeList.getLength(); i++) {
                    documentFlows.add((Element) dataNodeList.item(i));
                }

                for (int i = 0; i < documentFlows.size(); i++) {

                    Element flowElement = documentFlows.get(i);

                    // gets source and target components id
                    String sourceId = findSource(flowElement);
                    String targetId = flowElement.getAttribute("to");

                    FlowType type = flowElement.getLocalName().equals("DataFlow")
                            ? FlowType.DATA_FLOW
                            : FlowType.NAVIGATION;

                    Edge edge = new Edge(
                            sourceId,
                            targetId,
                            type);

                    NodeList bindingNodes = flowElement.getElementsByTagNameNS("*", "ParameterBinding");

                    // for each flow it gets the parameter bindings, if the binding is automatic it
                    // creates an edge binding with only the automatic flag set to true
                    // otherwise it resolves the source and target attributes and creates an edge
                    // binding with all the information that will be used for pattern detection
                    for (int j = 0; j < bindingNodes.getLength(); j++) {

                        Element bindingEl = (Element) bindingNodes.item(j);

                        boolean automatic = Boolean.parseBoolean(
                                bindingEl.getAttribute("automaticCoupling"));

                        if (automatic) {
                            edge.addBinding(new EdgeBinding(true, null, null));
                            continue;
                        }

                        if (isPassing(bindingEl)) {
                            continue;
                        }

                        // simply gets the source and target name attributes of the parameter binding
                        String sourceAttr = resolveAttribute(bindingEl, SOURCE_ATTRS);
                        String targetAttr = resolveAttribute(bindingEl, TARGET_ATTRS);

                        EdgeBinding binding = new EdgeBinding(
                                automatic,
                                sourceAttr,
                                targetAttr);

                        edge.addBinding(binding);
                    }

                    graph.addEdge(edge);
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * create the global graph
     * 
     * @param pagePaths
     * @return IFMLGraph
     * @throws Exception
     */
    public IFMLGraph buildGraph(List<String> pagePaths) throws Exception {
        IFMLGraph graph = new IFMLGraph();

        extractNodes(pagePaths, graph);
        extractEdges(pagePaths, graph);
        validateGraph(graph);
        return graph;
    }

    /**
     * validate the graph by checking that all the edges reference existing nodes
     * 
     * @param graph
     */
    private void validateGraph(IFMLGraph graph) {

        for (Edge edge : graph.getAllEdges()) {
            if (graph.getNode(edge.getSourceId()) == null ||
                    graph.getNode(edge.getTargetId()) == null) {
                throw new IllegalStateException("Edge references missing node");
            }
        }
    }

}
