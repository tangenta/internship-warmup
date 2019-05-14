package com.tangenta.scanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import com.tangenta.data.Tuple;
import com.tangenta.data.WordPosition;

public class MergeScanner implements Scanner {
    private List<Scanner> scanners;
    private List<Optional<WordPosition>> lookahead;
    
    private MergeScanner(List<Scanner> scanners) {
        this.scanners = scanners;

        int size = scanners.size();
        lookahead = new ArrayList<>(size);
        for (Scanner scanner : scanners) {
            lookahead.add(scanner.nextWord());
        }
    }

    public static MergeScanner of(List<Scanner> scanners) {
        return new MergeScanner(scanners);
    }

    @Override
    public Optional<WordPosition> nextWord() {
        return IntStream.range(0, lookahead.size())
                .mapToObj(i -> Tuple.of(i, lookahead.get(i)))
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
                    Optional<WordPosition> ret = lookahead.get(index);
                    lookahead.set(index, scanners.get(index).nextWord());
                    return ret;
                });
    }
}