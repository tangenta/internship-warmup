package com.tangenta.scanner;

import com.tangenta.data.WordPosition;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class WordPosScanner implements Scanner {

    private LineNumberReader reader;
    private boolean isClosed = false;

    private WordPosScanner(Path filePath, Charset charset) throws IOException {
        this.reader = new LineNumberReader(new InputStreamReader(new BufferedInputStream(
                new GZIPInputStream(new FileInputStream(filePath.toFile()))), charset));
    }

    public static WordPosScanner of(Path filePath, Charset charset) {
        return uncheckify(() -> new WordPosScanner(filePath, charset));
    }

    @Override
    public Optional<WordPosition> nextWord() {
        return uncheckify(() -> nextWordImpl());
    }

    private Optional<WordPosition> nextWordImpl() throws IOException {
        if (isClosed) return Optional.empty();
        String line = reader.readLine();
        if (line == null) {
            reader.close();
            isClosed = true;
            return Optional.empty();
        } else {
            String[] wordPos = line.split(" ");
            String word = wordPos[0];
            long position = Long.parseLong(wordPos[1]);
            return Optional.of(WordPosition.of(word, position));
        }
    }

    public static void write(Iterator<WordPosition> iterator, Path filePath) {
        uncheckify(() -> {
            writeImpl(iterator, filePath);
            return null;
        });
    }

    private static void writeImpl(Iterator<WordPosition> iterator, Path filePath) throws IOException {
        PrintWriter out = new PrintWriter(new BufferedOutputStream(
                new GZIPOutputStream(new FileOutputStream(filePath.toFile()))));

        while (iterator.hasNext()) {
            WordPosition wp = iterator.next();
            out.println(wp.word + " " + wp.position);
        }

        out.close();
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
