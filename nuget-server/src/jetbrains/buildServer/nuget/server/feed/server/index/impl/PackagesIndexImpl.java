/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.nuget.server.feed.server.index.impl;

import jetbrains.buildServer.dataStructures.DecoratingIterator;
import jetbrains.buildServer.dataStructures.Mapper;
import jetbrains.buildServer.nuget.server.feed.server.index.NuGetIndexEntry;
import jetbrains.buildServer.nuget.server.feed.server.index.PackagesIndex;
import jetbrains.buildServer.nuget.server.feed.server.index.impl.latest.LatestCalculator;
import jetbrains.buildServer.nuget.server.feed.server.index.impl.latest.LatestVersionsCalculator;
import jetbrains.buildServer.nuget.server.feed.server.index.impl.transform.IsLatestFieldTransformation;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.metadata.BuildMetadataEntry;
import jetbrains.buildServer.serverSide.metadata.MetadataStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static jetbrains.buildServer.nuget.server.feed.server.index.impl.NuGetArtifactsMetadataProvider.NUGET_PROVIDER_ID;

/**
 * @author Eugene Petrenko (eugene.petrenko@gmail.com)
 *         Date: 19.10.11 16:18
 */
public class PackagesIndexImpl implements PackagesIndex {
  private final MetadataStorage myStorage;
  private final PackageTransformation[] myTransformations;


  public PackagesIndexImpl(@NotNull final MetadataStorage storage,
                           @NotNull final PackageTransformation[] transformations) {
    myStorage = storage;
    myTransformations = transformations;
  }

  @NotNull
  public Iterator<NuGetIndexEntry> getNuGetEntries(long buildId) {
    return decorateMetadata(getBuildEntries(buildId));
  }

  private Iterator<BuildMetadataEntry> getBuildEntries(long buildId) {
    return myStorage.getBuildEntry(buildId, NUGET_PROVIDER_ID);
  }

  @NotNull
  public Iterator<NuGetIndexEntry> getNuGetEntries() {
    return decorateMetadata(myStorage.getAllEntries(NUGET_PROVIDER_ID));
  }

  private Iterator<NuGetIndexEntry> decorateMetadata(Iterator<BuildMetadataEntry> entries) {
    if (TeamCityProperties.getBoolean("teamcity.nuget.simple.feed.sort")) {
      return transformEntries(entries, getTranslatorsSimple());
    }

    final List<NuGetPackageBuilder> result = new ArrayList<NuGetPackageBuilder>();
    final Collection<PackageTransformation> translators = getTranslators(); //contains processing state!
    final LatestCalculator latestPackages = new LatestVersionsCalculator();
    while (entries.hasNext()) {

      final NuGetPackageBuilder builder = applyTransformation(entries.next(), translators);
      if (builder == null) continue;
      latestPackages.updatePackage(builder);
      result.add(builder);
    }
    latestPackages.updateSelectedPackages();

    //This is most consuming operation that requires to sort collection of entire packages
    Collections.sort(result, SemanticVersionsComparer.getBuildersComparator());

    return new DecoratingIterator<NuGetIndexEntry, NuGetPackageBuilder>(
            result.iterator(),
            new Mapper<NuGetPackageBuilder, NuGetIndexEntry>() {
              @Nullable
              public NuGetIndexEntry mapKey(@NotNull NuGetPackageBuilder internal) {
                return internal.build();
              }
            }
    );
  }

  @NotNull
  private Collection<PackageTransformation> getTranslators() {
    List<PackageTransformation> list = new ArrayList<PackageTransformation>(myTransformations.length + 1);
    for (PackageTransformation t : myTransformations) {
      list.add(t.createCopy());
    }
    return list;
  }

  @NotNull
  private Collection<PackageTransformation> getTranslatorsSimple() {
    final Collection<PackageTransformation> pts = getTranslators();
    pts.add(new IsLatestFieldTransformation());
    return pts;
  }

  @NotNull
  private Iterator<NuGetIndexEntry> transformEntries(@NotNull final Iterator<BuildMetadataEntry> entries,
                                                     @NotNull final Collection<PackageTransformation> trasformations) {
    return new DecoratingIterator<NuGetIndexEntry, BuildMetadataEntry>(
            entries,
            new Mapper<BuildMetadataEntry, NuGetIndexEntry>() {
              @Nullable
              public NuGetIndexEntry mapKey(@NotNull BuildMetadataEntry e) {
                final NuGetPackageBuilder pb = applyTransformation(e, trasformations);
                if (pb == null) return null;
                return pb.build();
              }
            });
  }

  @Nullable
  private static NuGetPackageBuilder applyTransformation(@NotNull final BuildMetadataEntry e,
                                                         @NotNull final Collection<PackageTransformation> trasformations) {
    final NuGetPackageBuilder pb = new NuGetPackageBuilder(e);
    for (PackageTransformation transformation : trasformations) {
      if (transformation.applyTransformation(pb) == PackageTransformation.Status.SKIP) return null;
    }
    return pb;
  }
}
