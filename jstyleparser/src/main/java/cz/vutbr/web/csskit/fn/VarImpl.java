package cz.vutbr.web.csskit.fn;

import cz.vutbr.web.css.Term;
import cz.vutbr.web.css.TermFunction;
import cz.vutbr.web.css.TermVar;
import cz.vutbr.web.csskit.TermFunctionImpl;

/**
 * @author VISTALL
 * @since 2024-11-18
 */
public class VarImpl extends TermFunctionImpl implements TermFunction.VarFunction {
    @Override
    public String getVarName() {
        Term<?> first = getFirst();
        if (first instanceof TermVar) {
            return ((TermVar<?>) first).getVarName();
        }
        return null;
    }
}
