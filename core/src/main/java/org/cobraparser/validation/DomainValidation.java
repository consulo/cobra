package org.cobraparser.validation;

public interface DomainValidation {
  boolean isValidCookieDomain(String domain, final String requestHostName);
}
