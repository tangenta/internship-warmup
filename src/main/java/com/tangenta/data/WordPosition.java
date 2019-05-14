package com.tangenta.data;

public class WordPosition implements Comparable<WordPosition> {
    public final String word;
    public final long position;

    private WordPosition(String word, long position) {
        this.word = word;
        this.position = position;
    }

    public static WordPosition of(String word, long position) {
        return new WordPosition(word, position);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (position ^ (position >>> 32));
        result = prime * result + ((word == null) ? 0 : word.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WordPosition other = (WordPosition) obj;
        if (position != other.position)
            return false;
        if (word == null) {
            if (other.word != null)
                return false;
        } else if (!word.equals(other.word))
            return false;
        return true;
    }
    

    @Override
    public int compareTo(WordPosition that) {
        int wRes = this.word.compareTo(that.word);
        if (wRes != 0) return wRes;
        return Long.compare(this.position, that.position);
    }

    @Override
    public String toString() {
        return "WordPosition [position=" + position + ", word=" + word + "]";
    }
}