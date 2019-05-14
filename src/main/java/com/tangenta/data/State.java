package com.tangenta.data;

public class State {
    public WordPosition lastWord = null;
    public boolean lastIsDup;

    public WordPosition curWord = null;
    public boolean curIsDup;
    
    public State() { }

    public interface StateConverter {
        void update(State state);
    }
}