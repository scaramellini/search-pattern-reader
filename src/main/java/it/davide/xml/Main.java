package it.davide.xml;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import patternsClasses.*;
import java.util.regex.Pattern;

import globalGraph.IFMLGraph;

//main class of the application, responsible for entire process of pattern detection
public class Main {

    /**
     * function that returns the paths of all the files in the directory that start
     * with "page" and end with ".wr", which are the files that represent the pages
     * of the IFML model
     * 
     * @param folderPath
     * @return List<String>
     * @throws Exception
     */
    private static List<String> getPagesPaths(String folderPath) throws Exception {
        // return all files .wr in the directory starting with "page"
        List<String> filesInFolder = Files.walk(Paths.get(folderPath))
                .filter(Files::isRegularFile)
                .filter(file -> file.getFileName().toString().startsWith("page"))
                .filter(file -> file.getFileName().toString().endsWith(".wr"))
                .map(Path::toString)
                .collect(Collectors.toList());

        return filesInFolder;
    }

    private static List<String> getProjectDirectories(String rootPath) throws Exception {
        Path root = Paths.get(rootPath);

        if (!Files.exists(root)) {
            throw new IllegalArgumentException("Path does not exist: " + rootPath);
        }

        if (Files.isDirectory(root)) {
            boolean containsPages;
            try (Stream<Path> pageFiles = Files.walk(root, 1)) {
                containsPages = pageFiles.filter(Files::isRegularFile)
                        .anyMatch(file -> file.getFileName().toString().startsWith("page")
                                && file.getFileName().toString().endsWith(".wr"));
            }

            if (containsPages) {
                return List.of(root.toString());
            }

            try (Stream<Path> children = Files.list(root)) {
                return children.filter(Files::isDirectory)
                        .map(Path::toString)
                        .collect(Collectors.toList());
            }
        }

        if (Files.isRegularFile(root)) {
            Path parent = root.getParent();
            return parent == null ? List.of(root.toString()) : List.of(parent.toString());
        }

        return List.of();
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        NewIFMLPatternExtractor extractor = new NewIFMLPatternExtractor();

        if (args.length == 0) {
            System.err.println("Please provide at least one path to scan (root project folder).");
            return;
        }

        for (String path : args) {
            for (String projectDir : getProjectDirectories(path)) {
                // Build the page-level graph and detect existing UI patterns (unchanged
                // behavior)
                ProjectPatternsJson report = new ProjectPatternsJson();

                GlobalPatternEngine patternEngine = new GlobalPatternEngine(
                        List.of(
                                new MultiFieldFormPattern(),
                                new BasicSearchPattern(),
                                new MulticriteriaSearchPattern(),
                                new FacetedSearchPattern(),
                                new MasterDetailPattern(),
                                new MasterMultiDetailsPattern(),
                                new MultiLevelMasterDetailPattern(),
                                new PreloadedFormPattern(),
                                new PreloadedFormPatternClassVariant(),
                                new WizardPattern(),
                                new DataLookupPattern()));

                IFMLGraph pageGraph = extractor.buildGraph(getPagesPaths(projectDir));
                patternEngine.detect(pageGraph, report);

                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);

                // destination path for the json report
                String projectName = Paths.get(projectDir).getFileName().toString();
                File outputDir = new File("output" + File.separator + projectName);
                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                }

                File outputFile = new File(outputDir, "pattern-report.json");
                mapper.writeValue(outputFile, report);
            }

            for (String projectDir : getProjectDirectories(path)) {
                // Load Actions for this project
                ActionRegistry actionRegistry = new ActionRegistry();
                actionRegistry.loadActionsFromWorkspace(projectDir);
            

                // Now process each WebView within the project and run functional patterns
                List<String> webviewIds = getWebviewIds(projectDir);
                ProjectPatternsJson allFunctionalReports = new ProjectPatternsJson();
                List<FunctionalPatternInterface> functionalPatterns = List.of(new LoginFunctionalPattern());

                for (String wv : webviewIds) {
                    WebViewPatternProcessor processor = new WebViewPatternProcessor(wv, projectDir, actionRegistry);
                    IFMLGraph unified = processor.processWebView();

                    for (FunctionalPatternInterface pattern : functionalPatterns) {
                        pattern.detect(unified, actionRegistry);
                        pattern.createJsonPattern(allFunctionalReports, unified);
                    }
                }

                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);

                // destination path for the json report
                String projectName = Paths.get(projectDir).getFileName().toString();
                File outputDir = new File("output" + File.separator + projectName);
                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                }

                // Write functional patterns report
                File funcReportFile = new File(outputDir, "functional-pattern-report.json");
                mapper.writeValue(funcReportFile, allFunctionalReports);
            }
        }
    }

    private static List<String> getWebviewIds(String projectDir) throws Exception {
        Path root = Paths.get(projectDir);
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(Files::isDirectory)
                    .map(Path::toString)
                    .filter(p -> p.contains(File.separator + "Model" + File.separator + "WebModel"))
                    .map(p -> {
                        int idx = p.indexOf(File.separator + "WebModel" + File.separator);
                        return idx >= 0 ? p.substring(idx + (File.separator + "WebModel" + File.separator).length())
                                : "";
                    })
                    .map(s -> s.split(Pattern.quote(File.separator))[0])
                    .filter(s -> s.startsWith("wv"))
                    .distinct()
                    .collect(Collectors.toList());
        }
    }
}