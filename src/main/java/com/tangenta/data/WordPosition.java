package com.tangenta.data;

import java.util.Objects;

public class WordPosition {
    public final String word;
    public final long position;
    public final boolean isDuplicate;

    private WordPosition(String word, long position, boolean isDuplicate) {
        this.word = word;
        this.position = position;
        this.isDuplicate = isDuplicate;
    }

    public static WordPosition of(String word, long position, boolean isDuplicate) {
        return new WordPosition(word, position, isDuplicate);
    }

    public static WordPosition of(String word, long position) {
        return new WordPosition(word, position, false);
    }

    public WordPosition modIsDup(boolean isDuplicate) {
        return new WordPosition(this.word, this.position, isDuplicate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WordPosition that = (WordPosition) o;
        return position == that.position &&
                isDuplicate == that.isDuplicate &&
                Objects.equals(word, that.word);
    }

    @Override
    public int hashCode() {
        return Objects.hash(word, position, isDuplicate);
    }

    @Override
    public String toString() {
        return "WordPosition{" +
                "word='" + word + '\'' +
                ", position=" + position +
                ", isDuplicate=" + isDuplicate +
                '}';
    }
}