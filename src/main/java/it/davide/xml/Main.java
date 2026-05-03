package it.davide.xml;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import patternsClasses.*;

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

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        NewIFMLPatternExtractor extractor = new NewIFMLPatternExtractor();

        for (String directory : args) {

            ProjectPatternsJson report = new ProjectPatternsJson();

            // initialize the pattern engine with the rules of the patterns to be detected
            GlobalPatternEngine patternEngine = new GlobalPatternEngine(
                    List.of(
                            new MultiFieldFormPattern(),
                            new BasicSearchPattern(),
                            new MulticriteriaSearchPattern(),
                            new FacetedSearchPattern(),
                            new MasterDetailPattern(),
                            new MasterMultiDetailsPattern(),
                            new MultiLevelMasterDetailPattern(),
                            new PreloadedFieldsPattern(),
                            new PreassignedSelectionFieldsPattern(),
                            new WizardPattern(),
                            new DataLookupPattern()));

            // build the global graph from the pages files and apply the patterns rules on
            // it
            patternEngine.detect(extractor.buildGraph(getPagesPaths(directory)), report);

            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            // destination path for the json report
            File outputDir = new File("output/" + directory.substring(directory.lastIndexOf("/") + 1));
            if (!outputDir.exists()) {
                outputDir.mkdirs(); // create directory folder if not exists
            }

            File outputFile = new File(outputDir, "pattern-report.json");

            mapper.writeValue(
                    outputFile,
                    report);
        }
    }
}