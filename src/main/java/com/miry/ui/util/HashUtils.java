package com.miry.ui.util;

/**
 * Hash utilities for generating widget IDs and other hashing needs.
 * Uses FNV-1a hash algorithm.
 */
public final class HashUtils {
    private HashUtils() {
        throw new AssertionError("No instances");
    }

    private static final int FNV_OFFSET_BASIS = 0x811C9DC5;
    private static final int FNV_PRIME = 0x01000193;

    /**
     * Computes FNV-1a 32-bit hash of a string.
     * Never returns 0 (returns 1 instead for consistency).
     *
     * @param s string to hash
     * @return hash value (never 0)
     */
    public static int hash32(String s) {
        if (s == null) {
            return 1;
        }

        int h = FNV_OFFSET_BASIS;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= FNV_PRIME;
        }
        return h == 0 ? 1 : h;
    }

    /**
     * Mixes a seed hash with an integer value.
     * Never returns 0 (returns 1 instead for consistency).
     *
     * @param seed seed hash value
     * @param value value to mix in
     * @return mixed hash value (never 0)
     */
    public static int mix(int seed, int value) {
        int h = seed;
        h ^= value;
        h *= FNV_PRIME;
        h ^= (h >>> 16);
        return h == 0 ? 1 : h;
    }

    /**
     * Mixes a seed hash with a string value.
     */
    public static int mix(int seed, String value) {
        return mix(seed, hash32(value));
    }

    /**
     * Combines multiple hash values into a single hash.
     * Useful for creating composite IDs from multiple components.
     *
     * @param hashes hash values to combine
     * @return combined hash value
     */
    public static int combine(int... hashes) {
        int h = FNV_OFFSET_BASIS;
        for (int hash : hashes) {
            h = mix(h, hash);
        }
        return h;
    }

    /**
     * Computes hash of multiple strings.
     */
    public static int hashStrings(String... strings) {
        int h = FNV_OFFSET_BASIS;
        for (String s : strings) {
            h = mix(h, hash32(s));
        }
        return h;
    }
}
