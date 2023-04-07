
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws IOException {
        String text = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        ArrayList<String> comments = new ArrayList<>();
        text = removeNewLines(text);
        text = emptyJavadoc(text, comments);
        text = emptyMultiLineComments(text, comments);
        text = restoreNewLines(text);
        text = emptySingleLineComments(text, comments);
        text = removeNewLines(text);
        for (int i = 0; i < comments.size(); i++) {
            String s = comments.get(i);
            comments.set(i, restoreNewLines(s).replaceAll("\\n\\s*", "\n "));
        }
        ArrayList<Klass> classes = getClasses(text);
        ArrayList<Method> methods = getMethodsIndexes(text);
        ArrayList<int[]> singleIndexes = getSingleLineCommentsIndexes(text);
        ArrayList<int[]> multiIndexes = getMultiLineCommentIndexes(text);
        ArrayList<int[]> docIndexes = getJavaDocIndexes(text);
        linkCommentsToMethods(methods, singleIndexes, multiIndexes, docIndexes, comments, text);
        linkMethodsToClasses(classes, methods);
        writeSingleLineComments(methods);
        writeMultiLineComments(methods);
        writeJavaDocs(methods);
        print(classes);
    }

    private static void print(ArrayList<Klass> classes) {
        for (Klass klass : classes) {
            System.out.println("Sinif: " + klass.name);
            for (Method method : klass.methods) {
                System.out.println("\tFonksiyon: " + method.name);
                System.out.println("\t\tTek Satir Yorum Sayisi: " + method.singleLineComments.size());
                System.out.println("\t\tCok Satirli Yorum Sayisi: " + method.multiLineCommentsCount.size());
                System.out.println("\t\tJavadoc Sayisi: " + method.javaDocsCount.size());
                System.out.println("-------------------------------------------");
            }
        }
    }

    private static void writeSingleLineComments(ArrayList<Method> methods) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("teksatir.txt"));
        for (Method method : methods) {
            writer.write("Fonksiyon: " + method.name);
            writer.newLine();
            for (String comment : method.singleLineComments) {
                writer.write(comment);
                writer.newLine();
            }
            writer.write("-----------------------------------------");
            writer.newLine();
        }
        writer.flush();
        writer.close();
    }

    private static void writeMultiLineComments(ArrayList<Method> methods) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("coksatir.txt"));
        for (Method method : methods) {
            writer.write("Fonksiyon: " + method.name);
            writer.newLine();
            for (String comment : method.multiLineCommentsCount) {
                writer.write(comment);
                writer.newLine();
            }
            writer.write("-----------------------------------------");
            writer.newLine();
        }
        writer.flush();
        writer.close();
    }

    private static void writeJavaDocs(ArrayList<Method> methods) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("javadoc.txt"));
        for (Method method : methods) {
            writer.write("Fonksiyon: " + method.name);
            writer.newLine();
            for (String comment : method.javaDocsCount) {
                writer.write(comment);
                writer.newLine();
            }
            writer.write("-----------------------------------------");
            writer.newLine();
        }
        writer.flush();
        writer.close();
    }

    private static void linkMethodsToClasses(ArrayList<Klass> classes, ArrayList<Method> methods) {
        for (Klass klass : classes) {
            for (Method method : methods) {
                if (method.start > klass.start && method.end < klass.end ) {
                    klass.methods.add(method);
                }
            }
        }
    }

    private static void linkCommentsToMethods(ArrayList<Method> methods,
                                              ArrayList<int[]> singleCommentsIndexes,
                                              ArrayList<int[]> multiLineCommentsIndexes,
                                              ArrayList<int[]> javaDocsIndexes,
                                              ArrayList<String> comments,
                                              String text) {
        for (int[] indexes : singleCommentsIndexes) {
            int commentIndex = Integer.parseInt(text.substring(indexes[0], indexes[1]).split("\\.")[1]);
            for (Method method : methods) {
                if (method.start < indexes[0] && method.end > indexes[0]) {
                    method.singleLineComments.add(comments.get(commentIndex));
                    break;
                }
            }
        }
        for (int[] indexes : multiLineCommentsIndexes) {
            int commentIndex = Integer.parseInt(text.substring(indexes[0], indexes[1]).split("\\.")[1]);
            for (Method method : methods) {
                if (method.start < indexes[0] && method.end > indexes[0]) {
                    method.multiLineCommentsCount.add(comments.get(commentIndex));
                    break;
                }
            }
        }
        for (int[] indexes : javaDocsIndexes) {
            int prevMethodEnd = 0;
            int commentIndex = Integer.parseInt(text.substring(indexes[0], indexes[1]).split("\\.")[1]);
            for (Method method : methods) {
                if (prevMethodEnd < indexes[0] && method.end > indexes[0]) {
                    method.javaDocsCount.add(comments.get(commentIndex));
                    break;
                }
                prevMethodEnd = method.end;
            }
        }
    }

    static String emptySingleLineComments(String text, ArrayList<String> comments) {
        StringBuilder builder = new StringBuilder(text);
        Pattern pattern = Pattern.compile("//.*");
        Matcher matcher = pattern.matcher(text);
        ArrayList<int[]> lines = new ArrayList<>();
        while (matcher.find()) {
            lines.add(new int[]{matcher.start(), matcher.end()});
        }
        for (int i = lines.size() - 1; i >= 0; i--) {
            int[] indexes = lines.get(i);
            builder.replace(indexes[0], indexes[1], "//." + comments.size());
            comments.add(text.substring(indexes[0], indexes[1]));
        }
        return builder.toString();
    }

    static String removeNewLines(String text) {
        return text.replaceAll("\\n", "!~!");
    }

    static String restoreNewLines(String text) {
        return text.replaceAll("!~!", "\n");
    }

    static String emptyJavadoc(String text, ArrayList<String> comments) {
        StringBuilder builder = new StringBuilder(text);
        Pattern pattern = Pattern.compile("/\\*\\*.*?\\*/");
        Matcher matcher = pattern.matcher(text);
        ArrayList<int[]> lines = new ArrayList<>();
        while (matcher.find()) {
            lines.add(new int[]{matcher.start(), matcher.end()});
        }
        for (int i = lines.size() - 1; i >= 0; i--) {
            int[] indexes = lines.get(i);
            builder.replace(indexes[0], indexes[1], "/**." + comments.size() + ".*/");
            comments.add(text.substring(indexes[0], indexes[1]));
        }
        return builder.toString();
    }

    static String emptyMultiLineComments(String text, ArrayList<String> comments) {
        StringBuilder builder = new StringBuilder(text);
        Pattern pattern = Pattern.compile("/\\*[^\\*].*?\\*/");
        Matcher matcher = pattern.matcher(text);
        ArrayList<int[]> lines = new ArrayList<>();
        while (matcher.find()) {
            lines.add(new int[]{matcher.start(), matcher.end()});
        }
        for (int i = lines.size() - 1; i >= 0; i--) {
            int[] indexes = lines.get(i);
            builder.replace(indexes[0], indexes[1], "/*." + comments.size() + ".*/");
            comments.add(text.substring(indexes[0], indexes[1]));
        }
        return builder.toString();
    }

    static ArrayList<Klass> getClasses(String text) {
        Pattern pattern = Pattern.compile("(?<=class).*?(?=\\{)");
        Matcher matcher = pattern.matcher(text);
        ArrayList<Klass> klasses = new ArrayList<>();
        while (matcher.find()) {
            Klass klass = new Klass(matcher.start(), 0, matcher.group().trim());
            klasses.add(klass);
            int openBraces = 1;
            int i = matcher.end() + 1;
            while (openBraces != 0 && i < text.length() - 1) {
                if (text.charAt(i) == '{')
                    openBraces++;
                else if (text.charAt(i) == '}')
                    openBraces--;
                i++;
            }
            klass.end = i;
        }
        return klasses;
    }

    static ArrayList<Method> getMethodsIndexes(String text) {
        Pattern pattern = Pattern.compile("[A-Za-z]+\\s*\\([^}]*?\\{");
        Matcher matcher = pattern.matcher(text);
        ArrayList<Method> methods = new ArrayList<>();
        while (matcher.find()) {
            if (isNotMethod(matcher.group()))
                continue;
            Method method = new Method(matcher.start(), 0, matcher.group().trim().split("\\(")[0]);
            methods.add(method);
            int openBraces = 1;
            int i = matcher.end();
            while (openBraces != 0 && i < text.length() - 1) {
                if (text.charAt(i) == '{')
                    openBraces++;
                else if (text.charAt(i) == '}')
                    openBraces--;
                i++;
            }
            method.end = i - 1;
        }
        return methods;
    }

    private static boolean isNotMethod(String group) {
        group = group.trim();
        return group.startsWith("for") || group.startsWith("if") ||
                group.startsWith("while") || group.startsWith("catch") ||
                group.startsWith("switch") || group.startsWith("else");
    }

    static ArrayList<int[]> getSingleLineCommentsIndexes(String text) {
        Pattern pattern = Pattern.compile("//\\.[0-9]+?");
        Matcher matcher = pattern.matcher(text);
        ArrayList<int[]> lines = new ArrayList<>();
        while (matcher.find()) {
            lines.add(new int[]{matcher.start(), matcher.end()});
        }
        return lines;
    }

    static ArrayList<int[]> getJavaDocIndexes(String text) {
        Pattern pattern = Pattern.compile("/\\*\\*\\.[0-9]+?\\.\\*/");
        Matcher matcher = pattern.matcher(text);
        ArrayList<int[]> lines = new ArrayList<>();
        while (matcher.find()) {
            lines.add(new int[]{matcher.start(), matcher.end()});
        }
        return lines;
    }

    static ArrayList<int[]> getMultiLineCommentIndexes(String text) {
        Pattern pattern = Pattern.compile("/\\*\\.[0-9]+?\\.\\*/");
        Matcher matcher = pattern.matcher(text);
        ArrayList<int[]> lines = new ArrayList<>();
        while (matcher.find()) {
            lines.add(new int[]{matcher.start(), matcher.end()});
        }
        return lines;
    }
}

class Klass {

    public String name;
    public int start;
    public int end;
    public ArrayList<Method> methods = new ArrayList<>();

    Klass(int start, int end, String name) {
        this.name = name;
        this.end = end;
        this.start = start;
    }
}

class Method {

    public ArrayList<String> singleLineComments = new ArrayList<>();
    public ArrayList<String> multiLineCommentsCount = new ArrayList<>();
    public ArrayList<String> javaDocsCount = new ArrayList<>();

    public int start;
    public int end;

    public String name;

    Method(int start, int end, String name) {
        this.name = name;
        this.start = start;
        this.end = end;
    }

}

