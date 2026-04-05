package org.cobraparser.util;

import java.security.PrivilegedAction;

public class SecurityUtil {

  public static <T> T doPrivileged(final PrivilegedAction<T> action) {
    return action.run();
  }
}
