package com.tangenta.scanner;

import java.util.Optional;

import com.tangenta.data.WordPosition;

public interface Scanner {
    Optional<WordPosition> nextWord();
}