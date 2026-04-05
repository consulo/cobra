package cz.vutbr.web.csskit;

import cz.vutbr.web.css.TermVar;

/**
 * @author VISTALL
 * @since 2024-11-18
 */
public class TermVarImpl<V> extends TermImpl<V> implements TermVar<V> {
    private final String myVarName;

    public TermVarImpl(String varName) {
        myVarName = varName;
    }

    @Override
    public String toString() {
        return myVarName;
    }

    @Override
    public String getVarName() {
        return myVarName;
    }
}
