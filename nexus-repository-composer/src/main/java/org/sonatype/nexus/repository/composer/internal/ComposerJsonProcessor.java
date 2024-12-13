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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import org.apache.commons.lang3.StringUtils;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.composer.ComposerContentFacet;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.fluent.FluentQuery;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonMap;
import static org.sonatype.nexus.repository.composer.internal.ComposerPathUtils.buildZipballPath;
import static org.sonatype.nexus.repository.composer.internal.recipe.ComposerRecipeSupport.*;

/**
 * Class encapsulating JSON processing for Composer-format repositories, including operations for parsing JSON indexes
 * and rewriting them to be compatible with a proxy repository.
 */
@Named
@Singleton
public class ComposerJsonProcessor
{
  private static final String PACKAGE_JSON_PATH = "/p/%package%.json";

  private static final String PACKAGE_V2_JSON_PATH = "/p2/%package%.json";
  private static final String LIST_JSON_PATH = "/packages/list.json";

  private static final String VENDOR_AND_PROJECT = "%s/%s";

  private static final String AUTOLOAD_KEY = "autoload";

  private static final String AUTOLOAD_DEV_KEY = "autoload-dev";

  private static final String AUTHORS_KEY = "authors";

  private static final String BIN_KEY = "bin";

  private static final String CONFLICT_KEY = "conflict";

  private static final String DESCRIPTION_KEY = "description";

  private static final String DIST_KEY = "dist";

  private static final String EXTRA_KEY = "extra";

  private static final String HOMEPAGE_KEY = "homepage";

  private static final String INCLUDE_PATH_KEY = "include-path";

  private static final String KEYWORDS_KEY = "keywords";

  private static final String LICENSE_KEY = "license";

  private static final String PROVIDERS_URL_KEY = "providers-url";

  private static final String METADATA_URL_KEY = "metadata-url";

  private static final String LIST_KEY = "list";

  private static final String AVAILABLE_PACKAGES_KEY = "available-packages";
  private static final String AVAILABLE_PACKAGE_PATTERNS_KEY = "available-package-patterns";

  private static final String PROVIDERS_KEY = "providers";

  private static final String PACKAGES_KEY = "packages";

  private static final String PACKAGE_NAMES_KEY = "packageNames";

  private static final String PROVIDE_KEY = "provide";

  private static final String REPLACE_KEY = "replace";

  private static final String REFERENCE_KEY = "reference";

  private static final String REQUIRE_KEY = "require";

  private static final String REQUIRE_DEV_KEY = "require-dev";

  private static final String SHASUM_KEY = "shasum";

  private static final String SCRIPTS_KEY = "scripts";

  private static final String SOURCE_KEY = "source";

  private static final String SUGGEST_KEY = "suggest";

  private static final String SUPPORT_KEY = "support";

  private static final String TYPE_KEY = "type";

  private static final String URL_KEY = "url";

  private static final String NAME_KEY = "name";

  private static final String VERSION_KEY = "version";

  private static final String TARGET_DIR_KEY = "target-dir";

  private static final String TIME_KEY = "time";

  private static final String UID_KEY = "uid";

  private static final String ZIP_TYPE = "zip";

  /**
   * List of supported fields in rewritten packages.json
   */
  private static final List<String> PACAKGES_JSON_FIELDS = Arrays.asList(
      METADATA_URL_KEY,
      PROVIDERS_URL_KEY,
      LIST_KEY,
      AVAILABLE_PACKAGES_KEY,
      AVAILABLE_PACKAGE_PATTERNS_KEY
  );

  private static final int MAX_AVAILABLE_PACKAGES = 100;
  private static final int PAGE_SIZE = 50;

  private static final ObjectMapper mapper = new ObjectMapper();

  private static final DateTimeFormatter timeFormatter = new DateTimeFormatterBuilder()
      .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
      .parseLenient()
      .appendOffset("+HH:MM", "+00:00")
      .toFormatter();

  private ComposerJsonExtractor composerJsonExtractor;
  private ComposerJsonMinifier composerJsonMinifier;

  @Inject
  public ComposerJsonProcessor(final ComposerJsonExtractor composerJsonExtractor, final ComposerJsonMinifier composerJsonMinifier) {
    this.composerJsonExtractor = checkNotNull(composerJsonExtractor);
    this.composerJsonMinifier = checkNotNull(composerJsonMinifier);
  }

  /**
   * Generates a packages.json file (inclusive of all projects) based on the list.json provided as a payload. Expected
   * usage is to "go remote" on the current repository to fetch a list.json copy, then pass it to this method to build
   * the packages.json for the client to use.
   */
  public Content generatePackagesFromList(final Repository repository, final Payload payload) throws IOException {
    // TODO: Parse using JSON tokens rather than loading all this into memory, it "should" work but I'd be careful.
    Map<String, Object> listJson = parseJson(payload);
    Content packagesJson = buildPackagesJson(repository, new LinkedHashSet<>((Collection<String>) listJson.get(PACKAGE_NAMES_KEY)));

    // Preserve caching info from list, if present.
    if (payload instanceof Content) {
      packagesJson.getAttributes().set(CacheInfo.class, ((Content) payload).getAttributes().get(CacheInfo.class));
    }

    return packagesJson;
  }

  /**
   * Generates a packages.json file (inclusive of all projects) based on the components provided. Expected usage is
   * for a hosted repository to be queried for its components, which are then provided to this method to build the
   * packages.json for the client to use.
   */
  public Content generatePackagesFromComponents(final Repository repository, final FluentComponents components)
      throws IOException
  {
    Set<String> packages = new HashSet<>();

    // Only populate "available-packages", if the repository is "small"
    if (components.count() <= MAX_AVAILABLE_PACKAGES) {
      Continuation<FluentComponent> comps = components.browse(PAGE_SIZE, null);
      while (!comps.isEmpty()) {
        comps.stream().map(comp -> comp.namespace() + "/" + comp.name()).forEach(packages::add);
        comps = components.browse(PAGE_SIZE, comps.nextContinuationToken());
      }
    }

    return buildPackagesJson(repository, packages);
  }

  /**
   * Builds a packages.json file as a {@code Content} instance containing the actual JSON for the given providers.
   */
  private Content buildPackagesJson(final Repository repository, final Set<String> names) throws IOException {
    Map<String, Object> packagesJson = new LinkedHashMap<>();
    packagesJson.put(PROVIDERS_URL_KEY, repository.getUrl() + PACKAGE_JSON_PATH);
    packagesJson.put(METADATA_URL_KEY, repository.getUrl() + PACKAGE_V2_JSON_PATH);
    if (!names.isEmpty()) {
      packagesJson.put(AVAILABLE_PACKAGES_KEY, names);
    }
    return new Content(new StringPayload(mapper.writeValueAsString(packagesJson), ContentTypes.APPLICATION_JSON));
  }

  /**
   * Generates a list.json file based on the components provided.
   *
   * @param components Components to process
   * @return JSON list with package names
   */
  public Content generateListFromComponents(@Nullable final FluentQuery<FluentComponent> components) throws IOException {
    Set<String> packages = new HashSet<>();
    if (components != null) {
      Continuation<FluentComponent> comps = components.browse(PAGE_SIZE, null);
      while (!comps.isEmpty()) {
        comps.stream().map(comp -> comp.namespace() + "/" + comp.name()).forEach(packages::add);
        comps = components.browse(PAGE_SIZE, comps.nextContinuationToken());
      }
    }

    Map<String, Object> packagesJson = singletonMap(PACKAGE_NAMES_KEY, packages.stream().sorted().collect(Collectors.toList()));

    return new Content(new StringPayload(mapper.writeValueAsString(packagesJson), ContentTypes.APPLICATION_JSON));
  }

  /**
   * Rewrites the provider JSON so that source entries are removed and dist entries are pointed back to Nexus.
   */
  public Payload rewriteProviderJson(final Repository repository, final Payload payload) throws IOException {
    Map<String, Object> json = parseJson(payload);
    if (json.get(PACKAGES_KEY) instanceof Map) {
      Map<String, Object> packagesMap = (Map<String, Object>) json.get(PACKAGES_KEY);
      for (String packageName : packagesMap.keySet()) {
        Map<String, Object> packageVersions = (Map<String, Object>) packagesMap.get(packageName);
        for (String packageVersion : packageVersions.keySet()) {
          // TODO: Make this more robust, right now it makes a lot of assumptions and doesn't deal with bad things well
          Map<String, Object> versionInfo = (Map<String, Object>) packageVersions.get(packageVersion);
          versionInfo.remove(SOURCE_KEY); // TODO: For now don't allow sources, probably should make this configurable?

          Map<String, Object> distInfo = (Map<String, Object>) versionInfo.get(DIST_KEY);
          if (distInfo != null && ZIP_TYPE.equals(distInfo.get(TYPE_KEY))) {
            versionInfo.put(DIST_KEY,
                buildDistInfo(repository, packageName, packageVersion, (String) distInfo.get(REFERENCE_KEY),
                    (String) distInfo.get(SHASUM_KEY), ZIP_TYPE));
          }
        }
      }
    }
    return new StringPayload(mapper.writeValueAsString(json), payload.getContentType());
  }

  /**
   * Rewrites the provider JSON so that source entries are removed and dist entries are pointed back to Nexus.
   */
  public Payload rewritePackageJson(final Repository repository, final Payload payload) throws IOException {
    Map<String, Object> json = parseJson(payload);

    this.composerJsonMinifier.expand(json);

    if (json.get(PACKAGES_KEY) instanceof Map) {
      Map<String, Object> packagesMap = (Map<String, Object>) json.get(PACKAGES_KEY);
      for (String packageName : packagesMap.keySet()) {
        List<Map<String, Object>> packageVersions = (List<Map<String, Object>>) packagesMap.get(packageName);
        for (Map<String, Object> versionInfo : packageVersions) {
          // TODO: Make this more robust, right now it makes a lot of assumptions and doesn't deal with bad things well
          versionInfo.remove(SOURCE_KEY); // TODO: For now don't allow sources, probably should make this configurable?

          Map<String, Object> distInfo = (Map<String, Object>) versionInfo.get(DIST_KEY);
          if (distInfo != null && ZIP_TYPE.equals(distInfo.get(TYPE_KEY))) {
            versionInfo.put(DIST_KEY,
                buildDistInfo(repository, packageName, (String) versionInfo.get(VERSION_KEY), (String) distInfo.get(REFERENCE_KEY),
                    (String) distInfo.get(SHASUM_KEY), ZIP_TYPE));
          }
        }
      }
    }

    this.composerJsonMinifier.minify(json);

    return new StringPayload(mapper.writeValueAsString(json), payload.getContentType());
  }

  /**
   * Rewrites the packages JSON so that source entries are removed and dist entries are pointed back to Nexus.
   */
  public Payload rewritePackagesJson(final Repository repository, final Payload payload) throws IOException {
    Map<String, Object> packagesJson = parseJson(payload);

    // Strip all fields we do not want to pass-through or rewrite
    packagesJson.keySet().retainAll(PACAKGES_JSON_FIELDS);

    // Rewrite URLs to our proxy instance, if present in upstream response
    if (packagesJson.containsKey(METADATA_URL_KEY)) {
      packagesJson.put(METADATA_URL_KEY, repository.getUrl() + PACKAGE_V2_JSON_PATH);
    }
    if (packagesJson.containsKey(PROVIDERS_URL_KEY)) {
      packagesJson.put(PROVIDERS_URL_KEY, repository.getUrl() + PACKAGE_JSON_PATH);
    }
    if (packagesJson.containsKey(LIST_KEY)) {
      packagesJson.put(LIST_KEY, repository.getUrl() + LIST_JSON_PATH);
    }

    return new StringPayload(mapper.writeValueAsString(packagesJson), payload.getContentType());
  }

  private String getAttributeFromAsset(FluentAsset asset, String name) {
    return asset.attributes().get(name, String.class, null);
  }

  /**
   * Builds a provider JSON file for a list of components. This minimal subset will contain the packages entries with
   * the name, version, and dist information for each component. A timestamp derived from the component's last updated
   * field and a uid derived from the component group/name/version and last updated time is also included in the JSON.
   */
  public Optional<Content> buildProviderJson(final Repository repository,
                                             final ComposerContentFacet content,
                                             final FluentQuery<FluentComponent> componentQuery) throws IOException
  {
    Map<String, Map<String, Object>> packages = new LinkedHashMap<>();

    Continuation<FluentComponent> components = componentQuery.browse(PAGE_SIZE, null);
    while (!components.isEmpty()) {
      for (FluentComponent component : components) {
        FluentAsset asset = component.assets().stream().findFirst().orElse(null);
        if (!asset.hasBlob()) {
          return null;
        }
        AssetBlob assetBlob = asset.blob().get();
        Blob blob = content.blobs().blob(assetBlob.blobRef()).orElse(null);
        Map<String, Object> composerJson = composerJsonExtractor.extractFromZip(blob);

        String vendor = component.namespace();
        String project = component.name();
        String version = component.version();

        String name = vendor + "/" + project;
        String time = formatUtc(component.lastUpdated());

        if (!packages.containsKey(name)) {
          packages.put(name, new LinkedHashMap<>());
        }

        String sha1 = assetBlob.checksums().get(HashAlgorithm.SHA1.name());
        Map<String, Object> sourceInfo = null;
        String sourceType = getAttributeFromAsset(asset, SOURCE_TYPE_FIELD_NAME);
        String sourceUrl = getAttributeFromAsset(asset, SOURCE_URL_FIELD_NAME);
        String sourceReference = getAttributeFromAsset(asset, SOURCE_REFERENCE_FIELD_NAME);
        if (StringUtils.isNotBlank(sourceType) && StringUtils.isNotBlank(sourceUrl) && StringUtils.isNotBlank(sourceReference)) {
          sourceInfo = new LinkedHashMap<>();
          sourceInfo.put(TYPE_KEY, sourceType);
          sourceInfo.put(URL_KEY, sourceUrl);
          sourceInfo.put(REFERENCE_KEY, sourceReference);
        }
        Map<String, Object> packagesForName = packages.get(name);
        packagesForName
            .put(version, buildPackageInfo(repository, name, version, sha1, sha1, ZIP_TYPE, time, composerJson, sourceInfo));
      }

      components = componentQuery.browse(PAGE_SIZE, components.nextContinuationToken());
    }

    if (packages.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(
        new Content(
            new StringPayload(mapper.writeValueAsString(singletonMap(PACKAGES_KEY, packages)),
                ContentTypes.APPLICATION_JSON)
        )
    );
  }

  /**
   * Builds a provider JSON file for a list of components. This minimal subset will contain the packages entries with
   * the name, version, and dist information for each component. A timestamp derived from the component's last updated
   * field and a uid derived from the component group/name/version and last updated time is also included in the JSON.
   */
  public Optional<Content> buildPackageJson(final Repository repository,
                                            final ComposerContentFacet content,
                                            final FluentQuery<FluentComponent> componentQuery) throws IOException
  {
    Map<String, List<Object>> packages = new LinkedHashMap<>();

    Continuation<FluentComponent> components = componentQuery.browse(PAGE_SIZE, null);
    while (!components.isEmpty()) {
      for (FluentComponent component : components) {
        FluentAsset asset = component.assets().stream().findFirst().orElse(null);
        if (!asset.hasBlob()) {
          continue;
        }
        AssetBlob assetBlob = asset.blob().get();
        Blob blob = content.blobs().blob(assetBlob.blobRef()).orElse(null);
        Map<String, Object> composerJson = composerJsonExtractor.extractFromZip(blob);

        String vendor = component.namespace();
        String project = component.name();
        String version = component.version();

        String name = vendor + "/" + project;
        String time = formatUtc(component.lastUpdated());

        if (!packages.containsKey(name)) {
          packages.put(name, new ArrayList<>());
        }

        String sha1 = assetBlob.checksums().get(HashAlgorithm.SHA1.name());
        Map<String, Object> sourceInfo = null;
        String sourceType = getAttributeFromAsset(asset, SOURCE_TYPE_FIELD_NAME);
        String sourceUrl = getAttributeFromAsset(asset, SOURCE_URL_FIELD_NAME);
        String sourceReference = getAttributeFromAsset(asset, SOURCE_REFERENCE_FIELD_NAME);
        if (StringUtils.isNotBlank(sourceType) && StringUtils.isNotBlank(sourceUrl) && StringUtils.isNotBlank(sourceReference)) {
          sourceInfo = new LinkedHashMap<>();
          sourceInfo.put(TYPE_KEY, sourceType);
          sourceInfo.put(URL_KEY, sourceUrl);
          sourceInfo.put(REFERENCE_KEY, sourceReference);
        }
        List<Object> packagesForName = packages.get(name);
        packagesForName.add(
            buildPackageInfo(repository, name, version, sha1, sha1, ZIP_TYPE, time, composerJson, sourceInfo)
        );
      }

      components = componentQuery.browse(PAGE_SIZE, components.nextContinuationToken());
    }

    if (packages.isEmpty()) {
      return Optional.empty();
    }

    Map<String, Object> json = new LinkedHashMap<>();
    json.put(PACKAGES_KEY, packages);
    this.composerJsonMinifier.minify(json);

    return Optional.of(
        new Content(new StringPayload(mapper.writeValueAsString(json), ContentTypes.APPLICATION_JSON))
    );
  }

  /**
   * Merges an incoming set of packages.json files.
   */
  public Content mergePackagesJson(final Repository repository, final List<Payload> payloads) throws IOException {
    boolean useAvailablePackages = true;
    SortedSet<String> names = new TreeSet<>();

    for (Payload payload : payloads) {
      Map<String, Object> json = parseJson(payload);

      // Only merge "available-packages", if all repositories provide a list
      if (useAvailablePackages && json.containsKey(AVAILABLE_PACKAGES_KEY)) {
        names.addAll((Collection<String>) json.get(AVAILABLE_PACKAGES_KEY));
      } else {
        useAvailablePackages = false;
        names.clear();
      }
    }

    return buildPackagesJson(repository, names);
  }

  /**
   * Merges incoming provider JSON files, producing a merged file containing only the minimal subset of fields that we
   * need to download artifacts.
   */
  public Content mergeProviderJson(final Repository repository, final List<Payload> payloads, final OffsetDateTime now)
      throws IOException
  {
    String currentTime = formatUtc(now);

    // TODO: Make this more robust, right now it makes a lot of assumptions and doesn't deal with bad things well,
    // can probably consolidate this with the handling for rewrites for proxy (or at least make it more rational).
    Map<String, Map<String, Object>> packages = new LinkedHashMap<>();
    for (Payload payload : payloads) {
      Map<String, Object> json = parseJson(payload);
      if (json.get(PACKAGES_KEY) instanceof Map) {

        Map<String, Object> packagesMap = (Map<String, Object>) json.get(PACKAGES_KEY);
        for (String packageName : packagesMap.keySet()) {

          Map<String, Object> packageVersions = (Map<String, Object>) packagesMap.get(packageName);
          for (String packageVersion : packageVersions.keySet()) {

            Map<String, Object> versionInfo = (Map<String, Object>) packageVersions.get(packageVersion);
            Map<String, Object> distInfo = (Map<String, Object>) versionInfo.get(DIST_KEY);
            if (distInfo == null) {
              continue;
            }
            Map<String, Object> sourceInfo = (Map<String, Object>) versionInfo.get(SOURCE_KEY);

            if (!packages.containsKey(packageName)) {
              packages.put(packageName, new LinkedHashMap<>());
            }

            String time = (String) versionInfo.get(TIME_KEY);
            if (time == null) {
              time = currentTime;
            }

            Map<String, Object> packagesForName = packages.get(packageName);
            packagesForName.putIfAbsent(packageVersion, buildPackageInfo(repository, packageName, packageVersion,
                (String) distInfo.get(REFERENCE_KEY), (String) distInfo.get(SHASUM_KEY),
                (String) distInfo.get(TYPE_KEY), time, versionInfo, sourceInfo));
          }
        }
      }
    }

    return new Content(new StringPayload(mapper.writeValueAsString(singletonMap(PACKAGES_KEY, packages)),
        ContentTypes.APPLICATION_JSON));
  }

  /**
   * Merges incoming provider JSON files, producing a merged file containing only the minimal subset of fields that we
   * need to download artifacts.
   */
  public Content mergePackageJson(final Repository repository, final List<Payload> payloads, final OffsetDateTime now)
      throws IOException
  {
    String currentTime = formatUtc(now);

    // TODO: Make this more robust, right now it makes a lot of assumptions and doesn't deal with bad things well,
    // can probably consolidate this with the handling for rewrites for proxy (or at least make it more rational).
    Map<String, Map<String, Object>> packages = new LinkedHashMap<>();
    for (Payload payload : payloads) {
      Map<String, Object> json = parseJson(payload);
      this.composerJsonMinifier.expand(json);
      if (json.get(PACKAGES_KEY) instanceof Map) {

        Map<String, Object> packagesMap = (Map<String, Object>) json.get(PACKAGES_KEY);
        for (String packageName : packagesMap.keySet()) {

          List<Map<String, Object>> packageVersions = (List<Map<String, Object>>) packagesMap.get(packageName);
          for (Map<String, Object> versionInfo : packageVersions) {
            String packageVersion = (String) versionInfo.get(VERSION_KEY);

            Map<String, Object> distInfo = (Map<String, Object>) versionInfo.get(DIST_KEY);
            if (distInfo == null) {
              continue;
            }
            Map<String, Object> sourceInfo = (Map<String, Object>) versionInfo.get(SOURCE_KEY);

            if (!packages.containsKey(packageName)) {
              packages.put(packageName, new LinkedHashMap<>());
            }

            String time = (String) versionInfo.get(TIME_KEY);
            if (time == null) {
              time = currentTime;
            }

            Map<String, Object> packagesForName = packages.get(packageName);
            packagesForName.putIfAbsent(packageVersion, buildPackageInfo(repository, packageName, packageVersion,
                (String) distInfo.get(REFERENCE_KEY), (String) distInfo.get(SHASUM_KEY),
                (String) distInfo.get(TYPE_KEY), time, versionInfo, sourceInfo));
          }
        }
      }
    }

    Map<String, List<Object>> packagesNormalized = new LinkedHashMap<>();
    for (Map.Entry<String, Map<String, Object>> pkg : packages.entrySet()) {
      packagesNormalized.put(pkg.getKey(), new ArrayList<>(pkg.getValue().values()));
    }

    Map<String, Object> json = new LinkedHashMap<>();
    json.put(PACKAGES_KEY, packagesNormalized);
    this.composerJsonMinifier.minify(json);

    return new Content(new StringPayload(mapper.writeValueAsString(json),
        ContentTypes.APPLICATION_JSON));
  }

  private Map<String, Object> buildPackageInfo(final Repository repository,
                                               final String packageName,
                                               final String packageVersion,
                                               final String reference,
                                               final String shasum,
                                               final String type,
                                               final String time,
                                               final Map<String, Object> versionInfo,
                                               final Map<String, Object> sourceInfo)
  {
    Map<String, Object> newPackageInfo = new LinkedHashMap<>();
    newPackageInfo.put(NAME_KEY, packageName);
    newPackageInfo.put(VERSION_KEY, packageVersion);
    newPackageInfo.put(DIST_KEY, buildDistInfo(repository, packageName, packageVersion, reference, shasum, type));
    if (sourceInfo != null) {
      newPackageInfo.put(SOURCE_KEY, sourceInfo);
    }
    newPackageInfo.put(TIME_KEY, time);
    newPackageInfo.put(UID_KEY, Integer.toUnsignedLong(
        Hashing.md5().newHasher()
            .putString(packageName, StandardCharsets.UTF_8)
            .putString(packageVersion, StandardCharsets.UTF_8)
            .putString(time, StandardCharsets.UTF_8)
            .hash()
            .asInt()));

    if (versionInfo.containsKey(AUTOLOAD_KEY)) {
      newPackageInfo.put(AUTOLOAD_KEY, versionInfo.get(AUTOLOAD_KEY));
    }
    if (versionInfo.containsKey(AUTOLOAD_DEV_KEY)) {
      newPackageInfo.put(AUTOLOAD_DEV_KEY, versionInfo.get(AUTOLOAD_DEV_KEY));
    }
    if (versionInfo.containsKey(REQUIRE_KEY)) {
      newPackageInfo.put(REQUIRE_KEY, versionInfo.get(REQUIRE_KEY));
    }
    if (versionInfo.containsKey(REPLACE_KEY)) {
      newPackageInfo.put(REPLACE_KEY, versionInfo.get(REPLACE_KEY));
    }
    if (versionInfo.containsKey(REQUIRE_DEV_KEY)) {
      newPackageInfo.put(REQUIRE_DEV_KEY, versionInfo.get(REQUIRE_DEV_KEY));
    }
    if (versionInfo.containsKey(SUGGEST_KEY)) {
      newPackageInfo.put(SUGGEST_KEY, versionInfo.get(SUGGEST_KEY));
    }

    if (versionInfo.containsKey(AUTHORS_KEY)) {
      newPackageInfo.put(AUTHORS_KEY, versionInfo.get(AUTHORS_KEY));
    }
    if (versionInfo.containsKey(BIN_KEY)) {
      newPackageInfo.put(BIN_KEY, versionInfo.get(BIN_KEY));
    }
    if (versionInfo.containsKey(CONFLICT_KEY)) {
      newPackageInfo.put(CONFLICT_KEY, versionInfo.get(CONFLICT_KEY));
    }
    if (versionInfo.containsKey(DESCRIPTION_KEY)) {
      newPackageInfo.put(DESCRIPTION_KEY, versionInfo.get(DESCRIPTION_KEY));
    }
    if (versionInfo.containsKey(EXTRA_KEY)) {
      newPackageInfo.put(EXTRA_KEY, versionInfo.get(EXTRA_KEY));
    }
    if (versionInfo.containsKey(HOMEPAGE_KEY)) {
      newPackageInfo.put(HOMEPAGE_KEY, versionInfo.get(HOMEPAGE_KEY));
    }
    if (versionInfo.containsKey(INCLUDE_PATH_KEY)) {
      newPackageInfo.put(INCLUDE_PATH_KEY, versionInfo.get(INCLUDE_PATH_KEY));
    }
    if (versionInfo.containsKey(KEYWORDS_KEY)) {
      newPackageInfo.put(KEYWORDS_KEY, versionInfo.get(KEYWORDS_KEY));
    }
    if (versionInfo.containsKey(LICENSE_KEY)) {
      newPackageInfo.put(LICENSE_KEY, versionInfo.get(LICENSE_KEY));
    }
    if (versionInfo.containsKey(PROVIDE_KEY)) {
      newPackageInfo.put(PROVIDE_KEY, versionInfo.get(PROVIDE_KEY));
    }
    if (versionInfo.containsKey(TARGET_DIR_KEY)) {
      newPackageInfo.put(TARGET_DIR_KEY, versionInfo.get(TARGET_DIR_KEY));
    }
    if (versionInfo.containsKey(SCRIPTS_KEY)) {
      newPackageInfo.put(SCRIPTS_KEY, versionInfo.get(SCRIPTS_KEY));
    }
    if (versionInfo.containsKey(SUPPORT_KEY)) {
      newPackageInfo.put(SUPPORT_KEY, versionInfo.get(SUPPORT_KEY));
    }
    if (versionInfo.containsKey(TYPE_KEY)) {
      newPackageInfo.put(TYPE_KEY, versionInfo.get(TYPE_KEY));
    }

    return newPackageInfo;
  }

  private Map<String, Object> buildDistInfo(final Repository repository,
                                            final String packageName,
                                            final String packageVersion,
                                            final String reference,
                                            final String shasum,
                                            final String type)
  {
    String packageNameParts[] = packageName.split("/");
    String packageVendor = packageNameParts[0];
    String packageProject = packageNameParts[1];
    Map<String, Object> newDistInfo = new LinkedHashMap<>();
    newDistInfo
        .put(URL_KEY, repository.getUrl() + buildZipballPath(packageVendor, packageProject, packageVersion));
    newDistInfo.put(TYPE_KEY, type);
    newDistInfo.put(REFERENCE_KEY, reference);
    newDistInfo.put(SHASUM_KEY, shasum);
    return newDistInfo;
  }

  /**
   * Obtains the dist URL for a particular vendor/project and version within a provider JSON payload.
   */
  public String getDistUrl(final String vendor, final String project, final String version, final Payload payload)
      throws IOException
  {
    String vendorAndProject = String.format(VENDOR_AND_PROJECT, vendor, project);
    Map<String, Object> json = parseJson(payload);
    Map<String, Object> packagesMap = (Map<String, Object>) json.get(PACKAGES_KEY);
    Map<String, Object> packageInfo = (Map<String, Object>) packagesMap.get(vendorAndProject);
    Map<String, Object> versionInfo = (Map<String, Object>) packageInfo.get(version);
    Map<String, Object> distInfo = (Map<String, Object>) versionInfo.get(DIST_KEY);
    return (String) distInfo.get(URL_KEY);
  }

  /**
   * Obtains the dist URL for a particular vendor/project and version within a provider JSON payload.
   */
  public String getDistUrlFromPackage(final String vendor, final String project, final String version, final Payload payload)
      throws IOException
  {
    String vendorAndProject = String.format(VENDOR_AND_PROJECT, vendor, project);
    Map<String, Object> json = parseJson(payload);
    composerJsonMinifier.expand(json);
    Map<String, Object> packagesMap = (Map<String, Object>) json.get(PACKAGES_KEY);
    List<Map<String, Object>> packageInfo = (List<Map<String, Object>>) packagesMap.get(vendorAndProject);

    Map<String, Object> versionInfo = packageInfo
        .stream()
        .filter((v) -> version.equals(v.get(VERSION_KEY)))
        .findFirst()
        .orElseThrow(() -> new IOException("version not found"));

    Map<String, Object> distInfo = (Map<String, Object>) versionInfo.get(DIST_KEY);
    return (String) distInfo.get(URL_KEY);
  }

  private Map<String, Object> parseJson(final Payload payload) throws IOException {
    try (InputStream in = payload.openInputStream()) {
      TypeReference<Map<String, Object>> typeReference = new TypeReference<Map<String, Object>>()
      {
      };
      return mapper.readValue(in, typeReference);
    }
  }

  private static String formatUtc(OffsetDateTime dateTime) {
    return dateTime.atZoneSameInstant(ZoneId.of("UTC")).format(timeFormatter);
  }
}
