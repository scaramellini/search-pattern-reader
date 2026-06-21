package it.davide.xml;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.*;

import globalGraph.ActionDefinition;
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
        private Boolean inDialogPage;
        private Map<GraphNode.FieldElementCategory, Set<GraphNode.FieldInfo>> fieldElementIds = new HashMap<>();
        private Map<GraphNode.ConditionalExpressionCategory, Set<String>> conditionalExpressions = new HashMap<>();

        public ComponentInfo(String id, String type, String objectId, Boolean inDialogPage,
                Map<GraphNode.FieldElementCategory, Set<GraphNode.FieldInfo>> fieldElementIds,
                Map<GraphNode.ConditionalExpressionCategory, Set<String>> conditionalExpressions) {
            this.id = id;
            this.type = type;
            this.objectId = objectId;
            this.inDialogPage = inDialogPage;
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

        public Boolean isInDialogPage() {
            return inDialogPage;
        }

        public Map<GraphNode.FieldElementCategory, Set<GraphNode.FieldInfo>> getFieldElementIds() {
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
    private void collectDirectComponents(Element viewComponents, List<ComponentInfo> components, Boolean inDialog) {

        NodeList children = viewComponents.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeType() == Node.ELEMENT_NODE) {

                Element element = (Element) child;

                String id = element.getAttribute("id");

                NodeList dataBinding = element.getElementsByTagName("DataBinding");

                Element dataBindingEl = (Element) dataBinding.item(0);

                String objectId = dataBindingEl != null ? resolveAttribute(dataBindingEl, DATA_BINDINGS) : null;

                Map<GraphNode.FieldElementCategory, Set<GraphNode.FieldInfo>> fieldElementIds = new HashMap<>();
                Map<GraphNode.ConditionalExpressionCategory, Set<String>> conditionalExpressions = new HashMap<>();

                fieldElementIds.put(GraphNode.FieldElementCategory.Field, extractFieldIds(element, "Field"));
                fieldElementIds.put(GraphNode.FieldElementCategory.SelectionField,
                        extractFieldIds(element, "SelectionField"));

                conditionalExpressions.put(GraphNode.ConditionalExpressionCategory.associationCondition,
                        extractComponentConditions(element, "AssociationRoleCondition"));
                conditionalExpressions.put(GraphNode.ConditionalExpressionCategory.attributeConditions,
                        extractComponentConditions(element, "AttributesCondition"));
                conditionalExpressions.put(GraphNode.ConditionalExpressionCategory.keyCondition,
                        extractComponentConditions(element, "KeyCondition"));

                if (id != null && !id.isEmpty()) {
                    components.add(new ComponentInfo(id, element.getNodeName(), objectId, inDialog,
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

                collectDirectComponents((Element) node, components, false);
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

                    collectDirectComponents((Element) node, components, true);
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
                        component.isInDialogPage(),
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

    private boolean isFieldTriggeredFlow(Element flow) {
        Node current = flow.getParentNode();
        boolean fieldAncestor = false;

        while (current != null && current.getNodeType() == Node.ELEMENT_NODE) {
            Element el = (Element) current;
            if ("Field".equals(el.getLocalName()) || "SelectionField".equals(el.getLocalName())) {
                fieldAncestor = true;
            }
            if (navFlowParentElements.contains(el.getLocalName())) {
                return fieldAncestor;
            }
            current = current.getParentNode();
        }

        return false;
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

    private Set<GraphNode.FieldInfo> extractFieldIds(Element componentElement, String tagName) {
        NodeList fieldNodes = componentElement.getElementsByTagNameNS("*", tagName);

        if (fieldNodes.getLength() == 0)
            return null;

        Set<GraphNode.FieldInfo> fieldInfos = new HashSet<>();

        for (int i = 0; i < fieldNodes.getLength(); i++) {
            Element fieldEl = (Element) fieldNodes.item(i);
            String fieldId = fieldEl.getAttribute("id");
            if (fieldId != null && !fieldId.isEmpty()) {

                String valueAttr = null;
                String valueAssocAttr = null;

                // Check for data binding
                NodeList dataBindingNodes = fieldEl.getElementsByTagNameNS("*", "OptionsDataBinding");
                String fieldDataBinding = null;
                if (dataBindingNodes.getLength() > 0) {
                    Element dataBindingEl = (Element) dataBindingNodes.item(0);
                    fieldDataBinding = resolveAttribute(dataBindingEl, Arrays.asList("classServiceRes"));

                    NodeList valueOptNodes = fieldEl.getElementsByTagNameNS("*", "OptionsAttributes");
                    if (valueOptNodes.getLength() > 0) {
                        Element valueAttrEl = (Element) valueOptNodes.item(0);
                        valueAttr = resolveAttribute(valueAttrEl,
                                Arrays.asList("classServiceAttribute", "classServiceRole"));
                    }
                }

                // Check for ValueAttribute
                NodeList valueAttrNodes = fieldEl.getElementsByTagNameNS("*", "ValueAttribute");

                if (valueAttrNodes.getLength() > 0) {
                    Element valueAttrEl = (Element) valueAttrNodes.item(0);
                    valueAttr = resolveAttribute(valueAttrEl,
                            Arrays.asList("classServiceAttribute", "classServiceRole"));
                } else {
                    NodeList valueAssocAttrNodes = fieldEl.getElementsByTagNameNS("*", "ValueAssociationRole");
                    if (valueAssocAttrNodes.getLength() > 0) {
                        Element valueAttrEl = (Element) valueAssocAttrNodes.item(0);
                        valueAssocAttr = resolveAttribute(valueAttrEl,
                                Arrays.asList("classServiceAttribute", "classServiceRole"));
                    }
                }
                fieldInfos.add(new GraphNode.FieldInfo(fieldId, valueAttr, valueAssocAttr, fieldDataBinding));
            }
        }

        return fieldInfos.isEmpty() ? null : fieldInfos;
    }

    /**
     * read the page files and extract the flows to create the edges of the global
     * graph
     * 
     * @param pagePaths
     * @param graph
     * @throws Exception
     */
    private void extractEdges(List<String> pagePaths, IFMLGraph graph, ActionRegistry actionRegistry, Set<String> actionIds) throws Exception {

        for (String pagePath : pagePaths) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();

            if (pagePath.contains("page13w.wr")) {
                System.out.println("Debug: Processing page13w.wr");
            }

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

                    if (sourceId == "frm12w") {
                        System.out.println("Debug: Found sourceId frm12w in page " + pagePath);
                    }

                    FlowType type = flowElement.getLocalName().equals("DataFlow")
                            ? FlowType.DATA_FLOW
                            : FlowType.NAVIGATION;

                    boolean fieldTriggered = isFieldTriggeredFlow(flowElement);

                    Edge edge;
                    if (targetId != null) {
                        String targetInputPort = null;
                        if (actionRegistry != null) {
                            ActionDefinition action = actionRegistry.getAction(targetId);
                            if (action != null && !action.getInputParameters().isEmpty()) {
                                targetInputPort = action.getInputParameters().get(0).getId();
                            }
                        }
                        edge = new Edge(sourceId, targetId, targetInputPort, type);
                    } else {
                        edge = new Edge(
                                sourceId,
                                targetId,
                                type,
                                fieldTriggered);
                    }

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
        return buildGraph(pagePaths, null, Collections.emptySet());
    }

    public IFMLGraph buildGraph(List<String> pagePaths, Set<String> actionIds) throws Exception {
        return buildGraph(pagePaths, null, actionIds);
    }

    public IFMLGraph buildGraph(List<String> pagePaths, ActionRegistry actionRegistry, Set<String> actionIds) throws Exception {
        IFMLGraph graph = new IFMLGraph();

        extractNodes(pagePaths, graph);
        extractEdges(pagePaths, graph, actionRegistry, actionIds);
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
            if (graph.getNode(edge.getSourceId()) == null) {
                throw new IllegalStateException("Edge references missing source node");
            }
            if (!edge.pointsToAction() && graph.getNode(edge.getTargetId()) == null) {
                throw new IllegalStateException("Edge references missing target node");
            }
        }
    }

    /**
     * Get all page files for a specific webview
     * Pages are stored in paths like: Model/WebModel/wv1/page13w.wr
     * 
     * @param folderPath The project folder path
     * @param webviewId  The webview ID (e.g., "wv1")
     * @return List of absolute file paths to page files for the specified webview
     * @throws Exception If directory traversal fails
     */
    public List<String> getPageFilesForWebview(String folderPath, String webviewId) throws Exception {
        List<String> filesInFolder = Files.walk(Paths.get(folderPath))
                .filter(Files::isRegularFile)
                .filter(file -> file.getFileName().toString().startsWith("page"))
                .filter(file -> file.getFileName().toString().endsWith(".wr"))
                .filter(file -> {
                    // Check if the file is in a path containing the webview ID
                    String path = file.toString();
                    return path.contains(File.separator + webviewId + File.separator);
                })
                .map(Path::toString)
                .collect(Collectors.toList());

        return filesInFolder;
    }
}
