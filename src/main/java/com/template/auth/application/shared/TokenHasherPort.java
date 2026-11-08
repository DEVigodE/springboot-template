package com.template.auth.application.shared;

/** One-way hash for opaque tokens (refresh / reset / verification) before they hit the DB. */
public interface TokenHasherPort {

    String hash(String value);
}
