package de.bstreit.java.engineeringtools.testing;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.SneakyThrows;

/**
 * From a JSON returned by a server response, which in spring can be obtained via
 * <pre>
 *        mockMvc
 *         .perform(...)
 *         .andDo(print());</pre>
 * this tool can generate the corresponding asserts that can be appended to the .perform method:
 * <pre>    mockMvc
 *         .perform(...)
 *         .andDo(print())
 *         // this you need to do manually
 *         .andExpect(status().isOk())
 *         // this is generated code:
 *         .andExpect(jsonPath("$.*", hasSize(5)))
 *         .andExpect(jsonPath("$.id").value("123456"))
 *         .andExpect(jsonPath("$.name").value("Peter"))
 *         .andExpect(jsonPath("$.age").value(33))
 *         ...</pre>
 * <p>
 * Usage: Place the JSON body printed by the <code>.andDo(print())</code> into the file
 * <code>src/main/resources/jsonPathInput.json</code>. Create it if it is not not there yet; it
 * is ignored by git.
 * <p>
 * Run this class.
 * <p>
 * The generated code will be displayed on the command line and also outputted to the file
 * <code>out/jsonPathOutput</code>.
 */
public class JsonPathGenerator {

    @SneakyThrows
    public static void main(String[] args) {
        DocumentContext ctx = JsonPath.parse(json());

        File file = new File("out/jsonPathOutput");
        FileUtils.forceMkdirParent(file);

        List<String> lines = new ArrayList<>();

        traverse(ctx.read("$", LinkedHashMap.class), "$", lines);

        try (PrintStream out = openOutputStream(file)) {
            out.println(".andExpect(\n  matchAll(");
            out.println(lines.stream().collect(Collectors.joining(",\n")));
            out.println("))");
        }

        System.out.println("\nResult (dumped into " + file + "):\n\n");
        System.out.println(FileUtils.readFileToString(file, StandardCharsets.UTF_8));
        System.out.println("");
    }

    private static void traverse(Object tree, String prefix, List<String> lines) {
        if (tree instanceof Map) {

            lines.add("    jsonPath(\"" + prefix + ".*\", hasSize(" + ((Map<?, ?>) tree)
                    .size() + "))");

            for (Object v :
                    ((Map<?, ?>) tree).keySet()) {

                traverse(((Map<?, ?>) tree).get(v), prefix + "." + v, lines);
            }

        } else if (tree instanceof List) {

            lines.add("    jsonPath(\"" + prefix + "\", hasSize(" + ((List<?>) tree)
                    .size() + "))");

            for (int i = 0; i < ((List<?>) tree).size(); i++) {
                traverse(((List<?>) tree).get(i), prefix + "[" + i + "]", lines);
            }

        } else if (tree instanceof Number) {
            lines.add("    jsonPath(\"" + prefix + "\").value(" + tree + ")");
        } else {
            lines.add("    jsonPath(\"" + prefix + "\").value(\"" + tree + "\")");
        }

    }

    @SneakyThrows
    private static String json() {

        try (InputStream stream = JsonPathGenerator.class.getClassLoader()
                .getResourceAsStream("jsonPathInput.json")) {

            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
    }

    @SneakyThrows
    private static PrintStream openOutputStream(File file) {
        return new PrintStream(FileUtils.openOutputStream(file));
    }


}
