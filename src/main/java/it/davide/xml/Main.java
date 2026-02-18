package it.davide.xml;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import IFMLElements.NavigationFlow;
import patternsClasses.MultiPageMasterDetailPattern;
import patternsClasses.NewBasicSearchPattern;

public class Main {

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

    private static List<String> getPropertiesPaths(String folderPath) throws Exception {
        // return all files .wr in the directory starting with "page"
        List<String> filesInFolder = Files.walk(Paths.get(folderPath))
                .filter(Files::isRegularFile)
                .filter(file -> file.getFileName().toString().startsWith("Properties"))
                .filter(file -> file.getFileName().toString().endsWith(".wr"))
                .map(Path::toString)
                .collect(Collectors.toList());

        return filesInFolder;
    }

    public static void main(String[] args) throws Exception {
        NewIFMLPatternExtractor extractor = new NewIFMLPatternExtractor();

        String directoryName;

        for (String directory : args) {
            //JsonPatternStructure.JsonReport report = new JsonPatternStructure.JsonReport();

            List<NavigationFlow> propertiesFlows = new ArrayList<NavigationFlow>();

            /* getPropertiesPaths(directory).forEach(dir -> {
                try {
                    propertiesFlows.addAll(extractor.extractFlows(dir));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }); */
            ProjectPatternsJson report = new ProjectPatternsJson();

            GlobalPatternEngine patternEngine = new GlobalPatternEngine(
                    List.of(
                            //new NewBasicSearchPattern(),
                            new MultiPageMasterDetailPattern()
                    )
            );

            patternEngine.detect(extractor.buildGraph(getPagesPaths(directory)), report);

            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            // path di destinazione
            File outputDir = new File("output/" + directory.substring(directory.lastIndexOf("/") + 1));
            if (!outputDir.exists()) {
                outputDir.mkdirs(); // create directory folder if not exists
            }

            // path completo del file
            File outputFile = new File(outputDir, "pattern-report.json");

            mapper.writeValue(
                    outputFile,
                    report);

            /*for (String pathToFile : getPagesPaths(directory)) { // for each file in the directory

                directoryName = directory.endsWith("\\")
                        ? Paths.get(directory).toString().substring(directory.lastIndexOf("\\") + 1)
                        : Paths.get(directory).toString().substring(directory.lastIndexOf("/") + 1);
                extractor.patternFinder(report, pathToFile, directoryName, propertiesFlows);
            }*/
        }
    }
}