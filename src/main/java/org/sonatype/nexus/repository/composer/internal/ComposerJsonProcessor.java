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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.composer.internal.ComposerPathUtils.buildZipballPath;

/**
 * Class encapsulating JSON processing for Composer-format repositories, including operations for parsing JSON indexes
 * and rewriting them to be compatible with a proxy repository.
 */
@Named
@Singleton
public class ComposerJsonProcessor
{
  private static final String REWRITE_URL = "%s/%s/%s/%s-%s.zip";

  private static final String PACKAGE_JSON_PATH = "/p/%package%.json";

  private static final String VENDOR_AND_PROJECT = "%s/%s";

  private static final String AUTOLOAD_KEY = "autoload";

  private static final String DIST_KEY = "dist";

  private static final String PROVIDERS_URL_KEY = "providers-url";

  private static final String PROVIDERS_KEY = "providers";

  private static final String PACKAGES_KEY = "packages";

  private static final String PACKAGE_NAMES_KEY = "packageNames";

  private static final String REQUIRE_KEY = "require";

  private static final String REQUIRE_DEV_KEY = "require-dev";

  private static final String SHA256_KEY = "sha256";

  private static final String SOURCE_KEY = "source";

  private static final String SUGGEST_KEY = "suggest";

  private static final String TYPE_KEY = "type";

  private static final String URL_KEY = "url";

  private static final String NAME_KEY = "name";

  private static final String VERSION_KEY = "version";

  private static final String TIME_KEY = "time";

  private static final String UID_KEY = "uid";

  private static final String ZIP_TYPE = "zip";

  private static final ObjectMapper mapper = new ObjectMapper();

  private static final DateTimeFormatter timeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ");

  private ComposerJsonExtractor composerJsonExtractor;

  @Inject
  public ComposerJsonProcessor(final ComposerJsonExtractor composerJsonExtractor) {
    this.composerJsonExtractor = checkNotNull(composerJsonExtractor);
  }

  /**
   * Generates a packages.json file (inclusive of all projects) based on the list.json provided as a payload. Expected
   * usage is to "go remote" on the current repository to fetch a list.json copy, then pass it to this method to build
   * the packages.json for the client to use.
   */
  public Content generatePackagesFromList(final Repository repository, final Payload payload) throws IOException {
    // TODO: Parse using JSON tokens rather than loading all this into memory, it "should" work but I'd be careful.
    Map<String, Object> listJson = parseJson(payload);
    return buildPackagesJson(repository, new LinkedHashSet<>((Collection<String>) listJson.get(PACKAGE_NAMES_KEY)));
  }

  /**
   * Generates a packages.json file (inclusive of all projects) based on the components provided. Expected usage is
   * for a hosted repository to be queried for its components, which are then provided to this method to build the
   * packages.json for the client to use.
   */
  public Content generatePackagesFromComponents(final Repository repository, final Iterable<Component> components)
      throws IOException
  {
    return buildPackagesJson(repository, StreamSupport.stream(components.spliterator(), false)
        .map(component -> component.group() + "/" + component.name()).collect(Collectors.toSet()));
  }

  /**
   * Builds a packages.json file as a {@code Content} instance containing the actual JSON for the given providers.
   */
  private Content buildPackagesJson(final Repository repository, final Set<String> names) throws IOException {
    Map<String, Object> packagesJson = new LinkedHashMap<>();
    packagesJson.put(PROVIDERS_URL_KEY, repository.getUrl() + PACKAGE_JSON_PATH);
    packagesJson.put(PROVIDERS_KEY, names.stream()
        .collect(Collectors.toMap((each) -> each, (each) -> Collections.singletonMap(SHA256_KEY, null))));
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
            versionInfo.put(DIST_KEY, buildDistInfo(repository, packageName, packageVersion, ZIP_TYPE));
          }
        }
      }
    }
    return new StringPayload(mapper.writeValueAsString(json), payload.getContentType());
  }

  /**
   * Builds a provider JSON file for a list of components. This minimal subset will contain the packages entries with
   * the name, version, and dist information for each component. A timestamp derived from the component's last updated
   * field and a uid derived from the component group/name/version and last updated time is also included in the JSON.
   */
  public Content buildProviderJson(final Repository repository,
                                   final StorageTx storageTx,
                                   final Iterable<Component> components) throws IOException
  {
    Map<String, Map<String, Object>> packages = new LinkedHashMap<>();
    for (Component component : components) {
      Asset asset = storageTx.firstAsset(component);
      BlobRef blobRef = asset.requireBlobRef();
      Blob blob = storageTx.requireBlob(blobRef);
      Map<String, Object> composerJson = composerJsonExtractor.extractFromZip(blob);

      String vendor = component.group();
      String project = component.name();
      String version = component.version();

      String name = vendor + "/" + project;
      String time = component.requireLastUpdated().withZone(DateTimeZone.UTC).toString(timeFormatter);

      if (!packages.containsKey(name)) {
        packages.put(name, new LinkedHashMap<>());
      }

      Map<String, Object> packagesForName = packages.get(name);
      packagesForName.put(version, buildPackageInfo(repository, name, version, ZIP_TYPE, time, composerJson));
    }

    return new Content(new StringPayload(mapper.writeValueAsString(Collections.singletonMap(PACKAGES_KEY, packages)),
        ContentTypes.APPLICATION_JSON));
  }

  /**
   * Merges an incoming set of packages.json files.
   */
  public Content mergePackagesJson(final Repository repository, final List<Payload> payloads) throws IOException {
    Set<String> names = new HashSet<>();
    for (Payload payload : payloads) {
      Map<String, Object> json = parseJson(payload);
      Map<String, Object> providers = (Map<String, Object>) json.get(PROVIDERS_KEY);
      names.addAll(providers.keySet());
    }
    return buildPackagesJson(repository, names);
  }

  /**
   * Merges incoming provider JSON files, producing a merged file containing only the minimal subset of fields that we
   * need to download artifacts.
   */
  public Content mergeProviderJson(final Repository repository, final List<Payload> payloads, final DateTime now)
      throws IOException
  {
    String time = now.withZone(DateTimeZone.UTC).toString(timeFormatter);

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

            if (!packages.containsKey(packageName)) {
              packages.put(packageName, new LinkedHashMap<>());
            }

            Map<String, Object> packagesForName = packages.get(packageName);
            packagesForName.putIfAbsent(packageVersion, buildPackageInfo(repository, packageName, packageVersion,
                (String) distInfo.get(TYPE_KEY), time, versionInfo));
          }
        }
      }
    }

    return new Content(new StringPayload(mapper.writeValueAsString(Collections.singletonMap(PACKAGES_KEY, packages)),
        ContentTypes.APPLICATION_JSON));
  }

  private Map<String, Object> buildPackageInfo(final Repository repository,
                                               final String packageName,
                                               final String packageVersion,
                                               final String type,
                                               final String time,
                                               final Map<String, Object> versionInfo)
  {
    Map<String, Object> newPackageInfo = new LinkedHashMap<>();
    newPackageInfo.put(NAME_KEY, packageName);
    newPackageInfo.put(VERSION_KEY, packageVersion);
    newPackageInfo.put(DIST_KEY, buildDistInfo(repository, packageName, packageVersion, type));
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
    if (versionInfo.containsKey(REQUIRE_KEY)) {
      newPackageInfo.put(REQUIRE_KEY, versionInfo.get(REQUIRE_KEY));
    }
    if (versionInfo.containsKey(REQUIRE_DEV_KEY)) {
      newPackageInfo.put(REQUIRE_DEV_KEY, versionInfo.get(REQUIRE_DEV_KEY));
    }
    if (versionInfo.containsKey(SUGGEST_KEY)) {
      newPackageInfo.put(SUGGEST_KEY, versionInfo.get(SUGGEST_KEY));
    }

    return newPackageInfo;
  }

  private Map<String, Object> buildDistInfo(final Repository repository,
                                            final String packageName,
                                            final String packageVersion,
                                            final String type)
  {
    String packageNameParts[] = packageName.split("/");
    String packageVendor = packageNameParts[0];
    String packageProject = packageNameParts[1];
    Map<String, Object> newDistInfo = new LinkedHashMap<>();
    newDistInfo
        .put(URL_KEY, repository.getUrl() + "/" + buildZipballPath(packageVendor, packageProject, packageVersion));
    newDistInfo.put(TYPE_KEY, type);
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

  private Map<String, Object> parseJson(final Payload payload) throws IOException {
    try (InputStream in = payload.openInputStream()) {
      TypeReference<Map<String, Object>> typeReference = new TypeReference<Map<String, Object>>() { };
      return mapper.readValue(in, typeReference);
    }
  }
}
