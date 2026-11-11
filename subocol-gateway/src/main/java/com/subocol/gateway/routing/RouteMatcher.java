package com.subocol.gateway.routing;

/**
 * Matches a request path against a route pattern.
 *
 * <p>
 * Only two shapes are supported, which is all the workshop needs:
 * <ul>
 * <li>{@code /api/orders/**} — every path under the prefix {@code /api/orders}.</li>
 * <li>{@code /api/orders} — exactly that path.</li>
 * </ul>
 *
 * <p>
 * The prefix match respects the segment boundary, so {@code /api/orders/**} matches
 * {@code /api/orders} and {@code /api/orders/3f9d} but never {@code /api/orders-legacy}.
 */
public final class RouteMatcher {

	private final String pattern;

	private final boolean prefixMatch;

	private final String prefix;

	private RouteMatcher(String pattern, boolean prefixMatch, String prefix) {
		this.pattern = pattern;
		this.prefixMatch = prefixMatch;
		this.prefix = prefix;
	}

	/**
	 * Compiles a route pattern.
	 *
	 * @param pattern the configured pattern, for example {@code /api/orders/**}
	 * @return the compiled matcher
	 * @throws IllegalArgumentException when the pattern is blank or does not start with {@code /}
	 */
	public static RouteMatcher compile(String pattern) {
		if (pattern == null || pattern.isBlank()) {
			throw new IllegalArgumentException("Route pattern must not be blank");
		}
		String normalized = normalize(pattern.trim());
		if (normalized.endsWith("/**")) {
			String base = stripTrailingSlash(normalized.substring(0, normalized.length() - 3));
			return new RouteMatcher(normalized, true, base);
		}
		return new RouteMatcher(normalized, false, normalized);
	}

	/**
	 * Tells whether the given request path belongs to this route.
	 *
	 * @param path the request path, without the query string
	 * @return {@code true} when the path is handled by this route
	 */
	public boolean matches(String path) {
		if (path == null) {
			return false;
		}
		if (!prefixMatch) {
			return path.equals(pattern);
		}
		if (prefix.isEmpty()) {
			return path.startsWith("/");
		}
		return path.equals(prefix) || path.startsWith(prefix + "/");
	}

	/** @return the normalized pattern this matcher was compiled from */
	public String getPattern() {
		return pattern;
	}

	@Override
	public String toString() {
		return "RouteMatcher[" + pattern + "]";
	}

	private static String normalize(String pattern) {
		if (!pattern.startsWith("/")) {
			throw new IllegalArgumentException("Route pattern must start with '/': " + pattern);
		}
		return stripTrailingSlash(pattern);
	}

	private static String stripTrailingSlash(String value) {
		if (value.length() > 1 && value.endsWith("/")) {
			return value.substring(0, value.length() - 1);
		}
		return value;
	}

}
