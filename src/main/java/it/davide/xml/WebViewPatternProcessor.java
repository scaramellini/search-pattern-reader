package it.davide.xml;

import globalGraph.*;

import java.util.ArrayList;
import java.util.List;

/**
 * WebViewPatternProcessor processes a single WebView at a time.
 * It is responsible for:
 * 1. Loading all pages of the WebView
 * 2. Loading all Action definitions for that WebView
 * 3. Building a unified graph that contains both page components and actions
 * 4. Providing the unified graph for pattern detection
 * 
 * Since each WebView is isolated, no links can exist between different WebViews.
 */
public class WebViewPatternProcessor {
    private final String webviewId;
    private final String projectPath;
    private final ActionRegistry actionRegistry;
    private final NewIFMLPatternExtractor extractor;
    
    private IFMLGraph unifiedGraph;

    public WebViewPatternProcessor(String webviewId, String projectPath, ActionRegistry actionRegistry) {
        this.webviewId = webviewId;
        this.projectPath = projectPath;
        this.actionRegistry = actionRegistry;
        this.extractor = new NewIFMLPatternExtractor();
    }

    /**
     * Process the WebView by building the unified graph
     * 
     * @return IFMLGraph - unified graph containing both page components and actions
     * @throws Exception If XML parsing or file operations fail
     */
    public IFMLGraph processWebView() throws Exception {
        // Get all page files for this webview
        List<String> pageFilePaths = getPageFilesForWebview();
        
        if (pageFilePaths.isEmpty()) {
            System.out.println("No page files found for webview: " + webviewId);
            unifiedGraph = new IFMLGraph();
            return unifiedGraph;
        }

        // Build the initial graph from pages, preserving any flows targeting actions
        unifiedGraph = extractor.buildGraph(pageFilePaths, actionRegistry, actionRegistry.getActionIds());

        // Add action nodes to the graph
        addActionNodesToGraph();

        // Connect page components to actions via edges
        connectPagesToActions();

        System.out.println("WebView " + webviewId + " processed: " + 
                unifiedGraph.getAllNodes().size() + " nodes, " + 
                unifiedGraph.getAllEdges().size() + " edges");

        return unifiedGraph;
    }

    /**
     * Get all page files (.wr files starting with "page") for this webview
     * 
     * @return List of absolute file paths
     * @throws Exception 
     */
    private List<String> getPageFilesForWebview() throws Exception {
        return extractor.getPageFilesForWebview(projectPath, webviewId);
    }

    /**
     * Add action nodes to the unified graph
     */
    private void addActionNodesToGraph() {
        List<ActionDefinition> actions = actionRegistry.getAllActions();
        
        for (ActionDefinition action : actions) {
            GraphNode actionNode = new GraphNode(action.getId(), action);
            unifiedGraph.addNode(actionNode);
        }
        
        System.out.println("Added " + actions.size() + " action nodes for webview: " + webviewId + ", project: " + projectPath);
    }

    /**
     * Connect page components to actions by updating edges
     * When a NavigationFlow points to an action ID, modify the edge to reference the action
     */
    private void connectPagesToActions() {
        List<Edge> edges = new ArrayList<>(unifiedGraph.getAllEdges());

        for (Edge edge : edges) {
            String targetId = edge.getTargetId();

            if (targetId != null && actionRegistry.hasAction(targetId)) {
                ActionDefinition action = actionRegistry.getAction(targetId);
                if (action == null) {
                    continue;
                }

                String targetInputPort = action.getInputParameters().isEmpty() ? null : action.getInputParameters().get(0).getId();
                Edge actionEdge = new Edge(edge.getSourceId(), action.getId(), targetInputPort, edge.getType());

                for (EdgeBinding binding : edge.getBindings()) {
                    actionEdge.addBinding(binding);
                }

                unifiedGraph.replaceEdge(edge, actionEdge);
                System.out.println("Converted edge to action call: " + edge.getSourceId() + " -> " + action.getId());
            }
        }
    }

    /**
     * Get the unified graph for pattern detection
     * 
     * @return IFMLGraph - the unified graph
     */
    public IFMLGraph getUnifiedGraph() {
        return unifiedGraph;
    }

    /**
     * Get the webview ID this processor is handling
     * 
     * @return String - webview ID
     */
    public String getWebviewId() {
        return webviewId;
    }

    /**
     * Get statistics about the processed webview
     * 
     * @return String - statistics summary
     */
    public String getStatistics() {
        if (unifiedGraph == null) {
            return "WebView " + webviewId + " not processed yet";
        }
        
        int nodeCount = (int) unifiedGraph.getAllNodes().size();
        int edgeCount = unifiedGraph.getAllEdges().size();
        
        long pageComponentCount = unifiedGraph.getAllNodes().stream()
                .filter(node -> !node.isAction())
                .count();
        
        long actionNodeCount = unifiedGraph.getAllNodes().stream()
                .filter(GraphNode::isAction)
                .count();
        
        return String.format(
                "WebView %s - Nodes: %d (Components: %d, Actions: %d), Edges: %d",
                webviewId, nodeCount, pageComponentCount, actionNodeCount, edgeCount
        );
    }
}
