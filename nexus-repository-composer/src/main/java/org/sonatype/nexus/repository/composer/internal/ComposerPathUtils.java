/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2018-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.composer.internal;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import org.eclipse.sisu.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.composer.internal.recipe.ComposerRecipeSupport.NAME_TOKEN;
import static org.sonatype.nexus.repository.composer.internal.recipe.ComposerRecipeSupport.PROJECT_TOKEN;
import static org.sonatype.nexus.repository.composer.internal.recipe.ComposerRecipeSupport.VENDOR_TOKEN;
import static org.sonatype.nexus.repository.composer.internal.recipe.ComposerRecipeSupport.VERSION_TOKEN;

/**
 * Utility class containing methods for working with Composer routes and paths.
 */
public final class ComposerPathUtils
{
  private static final String ZIPBALL_PATH = "/%s/%s/%s/%s.zip";

  private static final String PROVIDER_JSON_PATH = "/p/%s/%s.json";

  private static final String PACKAGE_JSON_PATH = "/p2/%s/%s.json";

  private static final String PACKAGE_JSON_PATH_DEV_VERSIONS = "/p2/%s/%s~dev.json";

  private static final String NAME_PATTERN = "%s-%s-%s";

  /**
   * Returns the vendor token from a path in a context. The vendor token must be present or the operation will fail.
   */
  public static String getVendorToken(final Context context) {
    TokenMatcher.State state = context.getAttributes().require(TokenMatcher.State.class);
    return checkNotNull(state.getTokens().get(VENDOR_TOKEN));
  }

  /**
   * Returns the project token from a path in a context. The project token must be present or the operation will fail.
   */
  public static String getProjectToken(final Context context) {
    TokenMatcher.State state = context.getAttributes().require(TokenMatcher.State.class);
    return checkNotNull(state.getTokens().get(PROJECT_TOKEN));
  }

  /**
   * Returns the version token from a path in a context. The version token must be present or the operation will fail.
   */
  public static String getVersionToken(final Context context) {
    TokenMatcher.State state = context.getAttributes().require(TokenMatcher.State.class);
    return checkNotNull(state.getTokens().get(VERSION_TOKEN));
  }

  /**
   * Builds the path to a zipball based on the path contained in a particular context. For download routes the full
   * path including the name token will be present and will be constructed accordingly. For upload routes the full
   * path will not be known because the filename will not be present, so the name portion will be constructed from
   * the vendor, project, and version information contained in the other path segments.
   */
  public static String buildZipballPath(final Context context) {
    TokenMatcher.State state = context.getAttributes().require(TokenMatcher.State.class);
    Map<String, String> tokens = state.getTokens();
    return buildZipballPath(
        tokens.get(VENDOR_TOKEN),
        tokens.get(PROJECT_TOKEN),
        tokens.get(VERSION_TOKEN),
        tokens.get(NAME_TOKEN));
  }

  /**
   * Builds the zipball path based on the provided vendor, project, and version. The filename will be constructed based
   * on the values of those parameters.
   */
  public static String buildZipballPath(final String vendor, final String project, final String version) {
    return buildZipballPath(vendor, project, version, null);
  }

  private static String buildZipballPath(final String vendor,
                                         final String project,
                                         final String version,
                                         @Nullable final String name)
  {
    return String.format(ZIPBALL_PATH, vendor, project, version,
        name == null ? String.format(NAME_PATTERN, vendor, project, version) : name);
  }

  /**
   * Builds the path to a provider json file based on the path contained in a particular context. The vendor token and
   * the project token must be present in the context in order to successfully generate the path.
   */
  public static String buildProviderPath(final Context context) {
    TokenMatcher.State state = context.getAttributes().require(TokenMatcher.State.class);
    Map<String, String> tokens = state.getTokens();
    return buildProviderPath(tokens.get(VENDOR_TOKEN), tokens.get(PROJECT_TOKEN));
  }

  /**
   * Builds the path to a provider json file based on the specified vendor and project tokens.
   */
  public static String buildProviderPath(final String vendor, final String project) {
    checkNotNull(vendor);
    checkNotNull(project);
    return String.format(PROVIDER_JSON_PATH, vendor, project);
  }

  /**
   * Builds the path to a package json file based on the path contained in a particular context. The vendor token and
   * the project token must be present in the context in order to successfully generate the path.
   */
  public static String buildPackagePath(final Context context) {
    TokenMatcher.State state = context.getAttributes().require(TokenMatcher.State.class);
    Map<String, String> tokens = state.getTokens();
    return buildPackagePath(tokens.get(VENDOR_TOKEN), tokens.get(PROJECT_TOKEN));
  }

  /**
   * Builds the path to a package json file based on the specified vendor and project tokens.
   */
  public static String buildPackagePath(final String vendor, final String project) {
    checkNotNull(vendor);
    checkNotNull(project);
    return String.format(PACKAGE_JSON_PATH, vendor, project);
  }

    /**
   * Builds the path to a dev package json file based on the specified vendor and project tokens.
   */
  public static String buildPackagePathForDevVersions(final String vendor, final String project) {
    checkNotNull(vendor);
    checkNotNull(project);
    return String.format(PACKAGE_JSON_PATH_DEV_VERSIONS, vendor, project);
  }

  /**
   * Returns path with appended string on the beginning
   *
   * @param path - Any path e.g. 'some/path/example'
   * @return - e.g. '/some/path/example'
   */
  public static String normalizeAssetPath(String path) {
    return StringUtils.prependIfMissing(path, BrowsePath.SLASH);
  }

  private ComposerPathUtils() {
    // empty
  }
}
