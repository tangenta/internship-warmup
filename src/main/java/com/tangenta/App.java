package com.tangenta;

import com.tangenta.data.Tuple;
import com.tangenta.data.WordPosition;
import com.tangenta.scanner.MergeScanner;
import com.tangenta.scanner.Scanner;
import com.tangenta.scanner.TextScanner;
import com.tangenta.scanner.WordPosScanner;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class App {
    public static Optional<WordPosition> findFirstNonDup(Path textFile, Path tempDir, long flushLimitSize) {
        TextScanner textScanner = TextScanner.of(textFile, StandardCharsets.UTF_8);

        List<Path> paths = new LinkedList<>();
        exportOrderedWords(textScanner, flushLimitSize, iter -> {
            Path newPath = tempDir.resolve(UUID.randomUUID().toString());
            paths.add(newPath);

            WordPosScanner.write(iter, newPath);
            return null;
        });

        return collect(MergeScanner.of(
                paths.stream()
                        .map(path -> WordPosScanner.of(path, StandardCharsets.UTF_8))
                        .collect(Collectors.toList())
        ));
    }

    protected static void exportOrderedWords(Scanner scanner, long flushLimitSize,
            Function<Iterator<WordPosition>, Void> flush) {
        TreeMap<String, WordPosition> map = new TreeMap<>();

        Optional<WordPosition> oWordPos;
        while ((oWordPos = scanner.nextWord()).isPresent()) {
            WordPosition wordPos = oWordPos.get();

            WordPosition oldWordPos = map.get(wordPos.word);
            if (oldWordPos == null) {
                map.put(wordPos.word, wordPos);
            } else {
                WordPosition newWordPos = WordPosition.of(oldWordPos.word, oldWordPos.position, true);
                map.put(newWordPos.word, newWordPos);
            }

            if (map.size() >= flushLimitSize) {
                flush.apply(map.values().iterator());
                map.clear();
            }
        }
        flush.apply(map.values().iterator());
    }

    protected static Optional<WordPosition> collect(MergeScanner mergeScanner) {
        Tuple<WordPosition, WordPosition> state = Tuple.of(null, null);
        int stateId = 0;

        while (true) {
            Optional<WordPosition> oNext = mergeScanner.nextWord();
            if (!oNext.isPresent()) break;
            WordPosition incoming = oNext.get();

            switch (stateId) {
                case 0: {
                    state = state.modLeft(incoming);
                    stateId = 1;
                    break;
                }
                case 1: {
                    if (incoming.word.equals(state.left.word)) {
                        state = state.modLeft(state.left.modIsDup(true));
                    } else {
                        if (state.left.isDuplicate) {
                            state = state.modLeft(incoming);
                        } else {
                            state = state.modRight(incoming);
                            stateId = 2;
                        }
                    }
                    break;
                }
                case 2: {
                    if (incoming.word.equals(state.right.word)) {
                        state = state.modRight(state.right.modIsDup(true));
                    } else {
                        if (state.right.isDuplicate) {
                            state = state.modRight(incoming);
                        } else {
                            WordPosition min = state.left.position < state.right.position ? state.left : state.right;
                            state = Tuple.of(min, incoming);
                        }
                    }
                    break;
                }
            }
        }

        switch (stateId) {
            case 0: return Optional.empty();
            case 1: return state.left.isDuplicate ? Optional.empty() : Optional.of(state.left);
            case 2: {
                if (state.right.isDuplicate) {
                    return Optional.of(state.left);
                } else {
                    return Optional.of(state.left.position < state.right.position ? state.left : state.right);
                }
            }
            default: throw new RuntimeException();
        }
    }

    public static void main(String[] args) {
        System.out.println();
    }
}
