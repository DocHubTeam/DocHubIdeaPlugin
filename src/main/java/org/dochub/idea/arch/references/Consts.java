package org.dochub.idea.arch.references;

import com.intellij.openapi.util.Key;

public class Consts {
    static public final String ID_PATTERN = "(([-a-zA-Z0-9\\\\_]*)(\\..[-a-zA-Z0-9\\\\_]*|)*)";
    static public Key cacheKey = new Key<>("#/$refs/");
}
