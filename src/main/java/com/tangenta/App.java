package com.tangenta;

import java.util.Iterator;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;

import com.tangenta.data.State;
import com.tangenta.data.WordPosition;
import com.tangenta.scanner.Merger;
import com.tangenta.scanner.Scanner;

public class App {
    public static void exportOrderedWords(Scanner scanner, long flushLimitSize,
            Function<Iterator<WordPosition>, Void> flush) {
        TreeSet<WordPosition> words = new TreeSet<>();
        long counter = 0;

        Optional<WordPosition> oWordPos;
        while ((oWordPos = scanner.nextWord()).isPresent()) {
            WordPosition wordPos = oWordPos.get();
            words.add(wordPos);
            counter += 1;
            if (counter >= flushLimitSize) {
                flush.apply(words.iterator());
                words.clear();
                counter = 0;
            }
        }
        flush.apply(words.iterator());
    }

    public static Optional<WordPosition> collect(Merger merger) {
        State state = new State();
        int stateId = 0;

        while (true) {
            Optional<WordPosition> oNext = merger.nextWord();
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
        System.out.println("Hello World!");
    }
}
