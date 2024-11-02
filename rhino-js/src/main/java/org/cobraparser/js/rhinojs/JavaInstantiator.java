package org.cobraparser.js.rhinojs;

public interface JavaInstantiator {
  public Object newInstance(Object[] args) throws InstantiationException, IllegalAccessException;
}
