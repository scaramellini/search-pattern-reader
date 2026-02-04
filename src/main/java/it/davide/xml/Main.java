package it.davide.xml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    private static List<String> getFilePaths(String folderPath) throws Exception {
        //return all files .wr in the directory starting with "page"
        List<String> filesInFolder = Files.walk(Paths.get(folderPath))
                .filter(Files::isRegularFile)
                .filter(file -> file.getFileName().toString().startsWith("page"))
                .filter(file -> file.getFileName().toString().endsWith(".wr"))
                .map(Path::toString)
                .collect(Collectors.toList());

        return filesInFolder;
    }

    public static void main(String[] args) throws Exception {
        IFMLPatternExtractor extractor = new IFMLPatternExtractor();

        String directoryName;

        for (String directory : args) {
            JsonPatternStructure.JsonReport report = new JsonPatternStructure.JsonReport();

            for (String pathToFile : getFilePaths(directory)) { //for each file in the directory

                directoryName = directory.endsWith("\\")
                        ? Paths.get(directory).toString().substring(directory.lastIndexOf("\\") + 1)
                        : Paths.get(directory).toString().substring(directory.lastIndexOf("/") + 1);
                extractor.patternFinder(report, pathToFile, directoryName);
            }
        }
    }
}