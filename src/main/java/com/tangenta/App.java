package com.tangenta;

import com.tangenta.data.State;
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
        State state = new State();
        int stateId = 0;

        while (true) {
            Optional<WordPosition> oNext = mergeScanner.nextWord();
            if (!oNext.isPresent()) break;
            WordPosition incoming = oNext.get();

            switch (stateId) {
                case 0: {
                    state.lastWord = incoming;
                    state.lastIsDup = false;
                    stateId = 1;
                    break;
                }
                case 1: {
                    if (incoming.word.equals(state.lastWord.word)) {
                        state.lastIsDup = true;
                        stateId = 2;
                    } else {
                        state.curWord = incoming;
                        state.curIsDup = false;
                        stateId = 3;
                    }
                    break;
                }
                case 2: {
                    if (incoming.word.equals(state.lastWord.word)) {
                        break;
                    } else {
                        state.lastWord = incoming;
                        state.curIsDup = false;
                        stateId = 1;
                        break;
                    }
                }
                case 3: {
                    if (incoming.word.equals(state.curWord.word)) {
                        state.curIsDup = true;
                        stateId = 4;
                    } else {
                        WordPosition minPosWord = state.lastWord.position < state.curWord.position ? 
                                state.lastWord : state.curWord;
                        state.lastWord = minPosWord;
                        state.curWord = incoming;
                    }
                    break;
                }
                case 4: {
                    if (incoming.word.equals(state.curWord.word)) {
                        break;
                    } else {
                        state.curWord = incoming;
                        state.curIsDup = false;
                        stateId = 3;
                        break;
                    }
                }
            }
        }
        switch (stateId) {
            case 0: 
            case 2: return Optional.empty();
            case 1: 
            case 4: return Optional.of(state.lastWord);
            case 3: return state.lastWord.position < state.curWord.position ? Optional.of(state.lastWord) : Optional.of(state.curWord);
            default: throw new RuntimeException();
        }
    }

    public static void main(String[] args) {
        System.out.println();
    }
}
