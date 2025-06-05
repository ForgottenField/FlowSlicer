package FlowSlicer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import soot.Unit;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Utility {
    // Method to export the source and sink maps to JSON files
    public static void exportSourceSinkInfo(ConcurrentHashMap<String, List<Unit>> sourceMap, ConcurrentHashMap<String, List<Unit>> sinkMap, String sourceFilePath, String sinkFilePath) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // Write source map to JSON file
            objectMapper.writeValue(new File(sourceFilePath), sourceMap);
            System.out.println("Source map exported to: " + sourceFilePath);

            // Write sink map to JSON file
            objectMapper.writeValue(new File(sinkFilePath), sinkMap);
            System.out.println("Sink map exported to: " + sinkFilePath);

        } catch (IOException e) {
            System.err.println("Error writing to JSON file: " + e.getMessage());
        }
    }
}
