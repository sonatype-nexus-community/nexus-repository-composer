package org.sonatype.nexus.repository.composer.internal;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.*;

@Named
@Singleton
public class ComposerJsonMinifier {

    private static final String V2_FORMAT = "composer/2.0";
    private static final String UNSET_VALUE = "__unset";
    private static final String MINIFIED_KEY = "minified";
    private static final String PACKAGES_KEY = "packages";

    public void expand(Map<String, Object> json) {
        Object format = json.get(MINIFIED_KEY);
        if (!(format instanceof String) || !format.equals(V2_FORMAT)) {
            return;
        }

        Map<String, List<Object>> packages = new LinkedHashMap<>();

        if (json.get(PACKAGES_KEY) instanceof Map) {
            Map<String, Object> packagesMap = (Map<String, Object>) json.get(PACKAGES_KEY);
            for (String packageName : packagesMap.keySet()) {
                List<Object> packageVersions = (List<Object>) packagesMap.get(packageName);

                Map<String, Object> expandedVersion = null;
                List<Object> expandedVersions = new ArrayList<>();

                for (Object versionObject : packageVersions) {
                    if (!(versionObject instanceof Map)) {
                        continue;
                    }

                    Map<String, Object> version = (Map<String, Object>) versionObject;

                    if (expandedVersion == null) {
                        expandedVersions.add(version);
                        expandedVersion = new LinkedHashMap<>(version);
                        continue;
                    }

                    for (Map.Entry<String, Object> versionData : version.entrySet()) {
                        if (versionData.getValue() instanceof String && versionData.getValue().equals(UNSET_VALUE)) {
                            expandedVersion.remove(versionData.getKey());
                        } else {
                            expandedVersion.put(versionData.getKey(), versionData.getValue());
                        }
                    }

                    expandedVersions.add(expandedVersion);
                    expandedVersion = new LinkedHashMap<>(expandedVersion);
                }

                packages.put(packageName, expandedVersions);
            }
        }

        json.put(PACKAGES_KEY, packages);
        json.remove(MINIFIED_KEY);
    }

    public void minify(Map<String, Object> json) {
        Map<String, List<Object>> packages = new LinkedHashMap<>();

        if (json.get(PACKAGES_KEY) instanceof Map) {
            Map<String, Object> packagesMap = (Map<String, Object>) json.get(PACKAGES_KEY);
            for (String packageName : packagesMap.keySet()) {
                List<Object> packageVersions = (List<Object>) packagesMap.get(packageName);

                Map<String, Object> lastKnownVersionData = null;
                List<Object> minifiedVersions = new ArrayList<>();

                for (Object versionObject : packageVersions) {
                    if (!(versionObject instanceof Map)) {
                        continue;
                    }

                    Map<String, Object> version = (Map<String, Object>) versionObject;

                    if (lastKnownVersionData == null) {
                        minifiedVersions.add(version);
                        lastKnownVersionData = new LinkedHashMap<>(version);
                        continue;
                    }

                    Map<String, Object> minifiedVersion = new LinkedHashMap<>();

                    for (Map.Entry<String, Object> versionData : version.entrySet()) {
                        boolean lastContains = lastKnownVersionData.containsKey(versionData.getKey());
                        Object lastData = lastKnownVersionData.get(versionData.getKey());
                        Object currentData = versionData.getValue();

                        if (!lastContains || !Objects.equals(lastData, currentData)) {
                            minifiedVersion.put(versionData.getKey(), currentData);
                            lastKnownVersionData.put(versionData.getKey(), currentData);
                        }
                    }

                    Iterator<Map.Entry<String, Object>> it = lastKnownVersionData.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, Object> lastData = it.next();
                        if (!version.containsKey(lastData.getKey())) {
                            minifiedVersion.put(lastData.getKey(), UNSET_VALUE);
                            it.remove();
                        }
                    }

                    minifiedVersions.add(minifiedVersion);
                }

                packages.put(packageName, minifiedVersions);
            }
        }

        json.put(PACKAGES_KEY, packages);
        json.put(MINIFIED_KEY, V2_FORMAT);
    }
}
