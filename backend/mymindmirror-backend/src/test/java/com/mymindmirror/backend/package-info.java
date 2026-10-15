/**
 * Top-level test package.
 *
 * <p>Contains:
 * <ul>
 *   <li>{@link com.mymindmirror.backend.AbstractIntegrationTest} — shared base for full-stack ITs
 *       (MySQL + Redis containers, mocks for AI, running against the
 *       complete Spring context).</li>
 *   <li>{@code *IT.java} classes — cross-cutting integration tests that
 *       exercise multiple layers per scenario. Run via {@code mvn verify}
 *       (Failsafe), excluded from {@code mvn test} (Surefire).</li>
 * </ul>
 *
 * <p>Subpackages contain unit and slice tests for their respective layers.
 *
 * <p>The {@code *IT} suffix is what routes a class through Failsafe.
 * Do not rename; do not relocate without updating {@code pom.xml}.
 */
package com.mymindmirror.backend;