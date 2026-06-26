package com.bookverse.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class DotenvLoader {

    private static final Path DOTENV_PATH = Path.of(".env");

    private DotenvLoader() {
    }

    public static void load() throws IOException {
        if (Files.notExists(DOTENV_PATH)) {
            return;
        }

        List<String> lines = Files.readAllLines(DOTENV_PATH);
        for (String line : lines) {
            loadLine(line.trim());
        }
    }

    private static void loadLine(String line) {
        if (line.isBlank() || line.startsWith("#")) {
            return;
        }

        String normalizedLine = line.startsWith("export ") ? line.substring("export ".length()).trim() : line;
        int separatorIndex = normalizedLine.indexOf('=');
        if (separatorIndex <= 0) {
            return;
        }

        String key = normalizedLine.substring(0, separatorIndex).trim();
        String value = normalizeValue(normalizedLine.substring(separatorIndex + 1).trim());
        if (System.getenv(key) == null && System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }

    private static String normalizeValue(String value) {
        if (value.length() >= 2) {
            char firstChar = value.charAt(0);
            char lastChar = value.charAt(value.length() - 1);
            if ((firstChar == '"' && lastChar == '"') || (firstChar == '\'' && lastChar == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }

        return value;
    }
}
