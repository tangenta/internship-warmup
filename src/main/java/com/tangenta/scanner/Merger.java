package com.tangenta.scanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import com.tangenta.data.Tuple;
import com.tangenta.data.WordPosition;

public class Merger implements Scanner {
    private List<Scanner> scanners;
    private List<Optional<WordPosition>> lookAheads;
    
    private Merger(List<Scanner> scanners) {
        this.scanners = scanners;

        int size = scanners.size();
        lookAheads = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            lookAheads.add(scanners.get(i).nextWord());
        }
    }

    public static Merger of(List<Scanner> scanners) {
        return new Merger(scanners);
    }

    @Override
    public Optional<WordPosition> nextWord() {
        return IntStream.range(0, lookAheads.size())
                .mapToObj(i -> Tuple.of(i, lookAheads.get(i)))
                .reduce((t1, t2) -> {
                    if (!t1.right.isPresent() && !t2.right.isPresent()) {
                        return t1;
                    } else if (!t1.right.isPresent()) {
                        return t2;
                    } else if (!t2.right.isPresent()) {
                        return t1;
                    } else {
                        return t1.right.get().word.compareTo(t2.right.get().word) < 0 ? t1 : t2;
                    }
                }).flatMap(t -> {
                    int index = t.left;
                    Optional<WordPosition> ret = lookAheads.get(index);
                    lookAheads.set(index, scanners.get(index).nextWord());
                    return ret;
                });
    }
}