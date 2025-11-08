package frontend.utils;

import java.io.*;
import java.util.*;

public class FileIO {
    private FileIO() {
    }

    public static ArrayList<String> readlines(String path) throws IOException {
        ArrayList<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    public static void writefile(String path, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)))) {
            writer.write(content);
        }
    }
}
