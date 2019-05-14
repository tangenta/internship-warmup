package com.tangenta;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.tangenta.scanner.*;
import com.tangenta.data.*;

public class AppTest extends TestCase {

    public AppTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(AppTest.class);
    }

    public void testScannerNext() {
        Scanner scanner = TextScanner.of(Paths.get("resources/scanner-test.txt"), StandardCharsets.UTF_8);
        int counter = 0;
        Optional<WordPosition> owp;
        String[] strs = { "Talk", "is", "cheap", "show", "me", "the", "code" };
        while ((owp = scanner.nextWord()).isPresent()) {
            WordPosition wp = owp.get();
            assertEquals(strs[counter], wp.word);
            counter += 1;
        }
        assertEquals(counter, 7);
    }

    public void testExportOrderedWords() {
        String[] origin = { "is", "Talk", "cheap", "show", "the", "me", "code" };
        Scanner mockScanner = createMockScanner(origin);

        WordPosition[] expected = { 
                WordPosition.of("Talk", 1), WordPosition.of("is", 0), WordPosition.of("cheap", 2),
                WordPosition.of("show", 3), WordPosition.of("me", 5), WordPosition.of("the", 4),
                WordPosition.of("code", 6), 
        };

        Int calledTimes = new Int(0);
        Int index = new Int(0);
        App.exportOrderedWords(mockScanner, 2, iter -> {
            calledTimes.value += 1;
            while (iter.hasNext()) {
                assertEquals(expected[index.value], iter.next());
                index.value += 1;
            }
            return null;
        });
        assertEquals(4, calledTimes.value);
    }

    public void testMerger() {
        String[] a1 = { "a", "b", "c" };
        String[] a2 = { "a", "d", "e" };
        String[] a3 = { "e", "e", "f" };

        List<Scanner> scanners = new LinkedList<>();
        scanners.add(createMockScanner(a1));
        scanners.add(createMockScanner(a2));
        scanners.add(createMockScanner(a3));
        Scanner merger = MergeScanner.of(scanners);

        String[] expected = { "a", "a", "b", "c", "d", "e", "e", "e", "f" };
        int i = 0;
        Optional<WordPosition> oWp;
        while ((oWp = merger.nextWord()).isPresent()) {
            assertEquals(expected[i++], oWp.get().word);
        }
        
    }

    public void testCollect() {
        String[] origin = { "b", "c", "a", "a", "d", "e", "c", "f", "c" };
        Scanner originScanner = createMockScanner(origin);

        List<Scanner> scatteredFiles = new LinkedList<>();
        App.exportOrderedWords(originScanner, 3, iter -> {
            List<WordPosition> wpList = new LinkedList<>();
            while (iter.hasNext()) {
                wpList.add(iter.next());
            }

            WordPosition[] wpArr = new WordPosition[wpList.size()];
            wpList.toArray(wpArr);
            scatteredFiles.add(createMockScanner(wpArr));
            return null;
        });

        Optional<WordPosition> oWp = App.collect(MergeScanner.of(scatteredFiles));
        assertTrue(oWp.isPresent());
        assertEquals("b", oWp.get().word);
        assertEquals(0, oWp.get().position);
    }

    private static Scanner createMockScanner(String[] backingArr) {
        return new Scanner() {
            Int counter = new Int(0);
            String[] origin = backingArr;
            public Optional<WordPosition> nextWord() {
                Optional<WordPosition> res = counter.value >= origin.length ? Optional.empty()
                        : Optional.of(WordPosition.of(origin[counter.value], counter.value));
                counter.value += 1;
                return res;
            }
        };
    }

    private static Scanner createMockScanner(WordPosition[] backingArr) {
        return new Scanner() {
            Int counter = new Int(0);
            WordPosition[] origin = backingArr;
            public Optional<WordPosition> nextWord() {
                Optional<WordPosition> res = counter.value >= origin.length ? Optional.empty()
                        : Optional.of(origin[counter.value]);
                counter.value += 1;
                return res;
            }
        };
    }

    private static class Int {
        public int value;

        public Int(int v) {
            value = v;
        }
    }
}
