package com.tangenta.scanner;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.tangenta.data.WordPosition;

public class TextScanner implements Scanner {
    private static final char SPLITTER = ' ';

    private final BufferedReader reader;
    private long counter = 0;

    private TextScanner(Path filePath, Charset charset) throws IOException {
        reader = Files.newBufferedReader(filePath, charset);
    }

    public static TextScanner of(Path filePath, Charset charset) {
        return uncheckify(() -> new TextScanner(filePath, charset));
    }

    @Override
    public Optional<WordPosition> nextWord() {
        return uncheckify(() -> nextWordImpl());
    }

    private Optional<WordPosition> nextWordImpl() throws IOException {
        StringBuilder builder = new StringBuilder();
        int nextCh;
        while (true) {
            nextCh = reader.read();
            if (isEof(nextCh) || nextCh == SPLITTER)
                break;
            builder.append((char) nextCh);
        }
        if (builder.length() == 0) {
            return isEof(nextCh) ? Optional.<WordPosition>empty() : nextWord();
        }
        return Optional.of(WordPosition.of(builder.toString(), counter++));
    }

    private static boolean isEof(int ch) {
        return ch == -1;
    }

    private interface CheckedFn<T> {
        T call() throws IOException;
    }

    private static <T> T uncheckify(CheckedFn<T> checkFn) {
        try {
            return checkFn.call();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage());
        }
    }
}