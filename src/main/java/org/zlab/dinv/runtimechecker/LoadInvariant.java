package org.zlab.dinv.runtimechecker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoadInvariant {

    public static Map<String, List<String>> load(Path filename) {
        Map<String, List<String>> resultMap = new HashMap<>();
        String currentKey = null;
        List<String> currentList = null;

        try (BufferedReader br = new BufferedReader(new FileReader(filename.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("===")) {
                    // Start of a new block, so create a new list for the current key
                    currentKey = br.readLine();
                    if (currentKey == null) {
                        break; // stop here
                    }
                    currentList = new ArrayList<>();
                    resultMap.put(currentKey, currentList);
                } else if (currentKey != null && currentList != null) {
                    // Add subsequent lines to the current list
                    currentList.add(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultMap;
    }


    public static void main(String[] args) {
        // use the following command to print the inv to a file, then parse it using load function.
        // java -cp $DAIKONDIR/daikon.jar daikon.PrintInvariants  /Users/hanke/Desktop/Project/daikon/examples/java-examples/StackAr/StackArTester.inv.gz > inv

        Path p = Paths.get("/Users/hanke/Desktop/Project/daikon/examples/java-examples/StackAr/inv");
        Map<String, List<String>> invs = load(p);

    }
}
