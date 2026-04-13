package ru.wbif;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class Main {

    private static final List<String> lines = new ArrayList<>();
    private static final Long2IntMap groups = new Long2IntOpenHashMap();
    private static final Int2ObjectMap<IntOpenHashSet> lineIndexes = new Int2ObjectOpenHashMap<>();
    private static final Int2IntMap parents = new Int2IntOpenHashMap();

    static void main(String[] args) {
        args = new String[]{"C:\\Users\\WhiteBeef\\Downloads\\lng-4(2).txt\\lng.txt"};

        long startTime = System.currentTimeMillis();

        if (!readFile(args)) {
            return;
        }

        int groupIndex = 0;

        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            CharSequence line = lines.get(lineIndex);

            int group = -1;

            int i = 0;
            int start = 0;
            int col = 0;

            while (i <= line.length()) {
                if (i == line.length() || line.charAt(i) == ';') {

                    long value = parseLong(line, start, i);

                    if (value != Long.MIN_VALUE) {
                        value = value * 1000 + col;

                        if (!groups.containsKey(value)) {
                            if (group == -1) {
                                group = groupIndex++;
                                groups.put(value, group);
                                lineIndexes.computeIfAbsent(group, k -> new IntOpenHashSet())
                                        .add(lineIndex);
                            } else {
                                groups.put(value, group);
                            }
                        } else {
                            int tempGroup = groups.get(value);

                            if (group == -1) {
                                tempGroup = findRoot(tempGroup);
                                group = tempGroup;
                                groups.put(value, group);
                                lineIndexes.computeIfAbsent(group, k -> new IntOpenHashSet())
                                        .add(lineIndex);
                            } else {
                                tempGroup = findRoot(tempGroup);

                                if (group != tempGroup) {
                                    parents.put(tempGroup, group);
                                    lineIndexes.computeIfAbsent(group, k -> new IntOpenHashSet())
                                            .addAll(lineIndexes.get(tempGroup));
                                    lineIndexes.remove(tempGroup);
                                }
                            }
                        }
                    }

                    start = i + 1;
                    col++;
                }
                i++;
            }
        }

        writeToFile(args);

        System.out.println("Время выполнения: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    private static long parseLong(CharSequence s, int start, int end) {
        long result = 0;
        boolean hasDigits = false;

        for (int i = start; i < end; i++) {
            char c = s.charAt(i);

            if (c == '"') continue;

            if (c < '0' || c > '9') {
                return Long.MIN_VALUE;
            }

            hasDigits = true;
            result = result * 10 + (c - '0');
        }

        return hasDigits ? result : Long.MIN_VALUE;
    }

    private static int findRoot(int group) {
        while (parents.containsKey(group)) {
            group = parents.get(group);
        }
        return group;
    }

    private static boolean readFile(String[] args) {
        BufferedReader reader = null;
        try {
            if (args.length < 1) {
                System.out.println("Вы не указали файл!");
                return false;
            }

            Path filePath = Path.of(args[0]);

            if (args[0].endsWith(".gz")) {
                reader = new BufferedReader(new InputStreamReader(
                        new GZIPInputStream(Files.newInputStream(filePath))
                ));
            } else {
                reader = Files.newBufferedReader(filePath);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (fastRegex(line)) continue;
                lines.add(line);
            }

        } catch (IOException e) {
            System.out.println("Ошибка: " + e.getMessage());
            return false;
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException ignored) {
            }
        }
        return true;
    }

    private static void writeToFile(String[] args) {
        Path filePath = Path.of(args[0]);

        try (BufferedWriter writer = Files.newBufferedWriter(
                Path.of(filePath.getParent().toAbsolutePath() + "\\output.txt"))) {

            List<Set<Integer>> output = new ArrayList<>();

            for (int group : lineIndexes.keySet()) {
                group = findRoot(group);

                if (lineIndexes.containsKey(group) && lineIndexes.get(group).size() > 1) {
                    output.add(lineIndexes.get(group));
                }
            }

            output.sort((o1, o2) -> Integer.compare(o2.size(), o1.size()));

            writer.write("Количество групп: " + output.size() + "\n");
            System.out.println("Количество групп: " + output.size());

            int groupNumber = 1;
            for (Set<Integer> set : output) {
                writer.write("Группа " + groupNumber++ + "\n");
                for (int i : set) {
                    writer.write(lines.get(i));
                    writer.newLine();
                }
            }

        } catch (IOException e) {
            System.out.println("Ошибка записи: " + e.getMessage());
        }
    }

    private static final Set<Character> ALLOWED_DIGITS =
            Set.of('0', '1', '2', '3', '4', '5', '6', '7', '8', '9');

    private static boolean fastRegex(String str) {
        int i = 0;

        if (str.charAt(i++) != '"') return false;

        while (i < str.length() && ALLOWED_DIGITS.contains(str.charAt(i))) i++;

        if (i++ >= str.length()) return false;

        while (i < str.length()) {
            if (str.charAt(i++) != ';') return false;
            if (str.charAt(i++) != '"') return false;

            while (i < str.length() && ALLOWED_DIGITS.contains(str.charAt(i))) i++;

            if (i++ >= str.length() - 1) return false;
            if (str.charAt(i) != '"') return false;
        }

        return true;
    }
}