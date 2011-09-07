/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package jetbrains.buildServer.nuget.server.feed.render.impl;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.nuget.server.feed.render.NuGetAtomItem;
import jetbrains.buildServer.nuget.server.feed.render.NuGetItem;
import jetbrains.buildServer.nuget.server.feed.render.NuGetProperties;
import jetbrains.buildServer.util.FileUtil;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 06.09.11 22:03
 */
public class LocalNuGetPackageItemsFactory {
  private static final Logger LOG = Logger.getInstance(LocalNuGetPackageItemsFactory.class.getName());
  public static final String NS = "http://schemas.microsoft.com/packaging/2010/07/nuspec.xsd";

  @NotNull
  public NuGetItem createPackage(@NotNull final String detailsUrl, @NotNull final File nupkg) throws PackageLoadException {
    final Element root = parseNuSpec(nupkg);

    if (root == null) {
      throw new PackageLoadException("Failed to fetch .nuspec from package");
    }

    final String id = parseProperty(root, "id");
    final String version = parseProperty(root, "version");
    final String authors = parseProperty(root, "authors");
    final String owners = parseProperty(root, "owners");
    final String requireLicenseAcceptanse = parseProperty(root, "requireLicenseAcceptance");
    final String description = parseProperty(root, "description");
    final String summary = parseProperty(root, "summary");
    final Date updated = new Date(nupkg.lastModified());
    final long size = nupkg.length();
    final String licenseUrl = parseProperty(root, "licenseUrl");
    final String projectUrl = parseProperty(root, "projectUrl");
    final String iconUrl = parseProperty(root, "iconUrl");
    final String dependencies = parseDependencies(root);
    final String tags = parseProperty(root,"tags");

    return new NuGetItem() {
      @NotNull
      public NuGetAtomItem getAtomItem() {
        return new NuGetAtomItem() {
          public String getItemName() {
            return id;
          }

          public String getItemVersion() {
            return version;
          }

          public String getItemTitle() {
            return id;
          }

          public String getItemSummary() {
            return summary;
          }

          public Date getItemUpdated() {
            return updated;
          }

          public String getItemAuthors() {
            return authors;
          }

          public String getDownloadPath() {
            //TODO: connect to context to make it easy to resolve
            return id + "." + version + ".nupkg";
          }
        };
      }

      @NotNull
      public NuGetProperties getProperties() {
        return new NuGetProperties() {
          public String getId() {
            return id;
          }

          public String getVersion() {
            return version;
          }

          public String getTitle() {
            return id;
          }

          public String getAuthors() {
            return authors;
          }

          public String getPackageType() {
            return "Packages";
          }

          public String getSummaty() {
            return summary;
          }

          public String getCopyright() {
            return null;
          }

          public String getPackageHashAlgorithm() {
            return "SHA512";
          }

          public String getPackageHash() {
            return "TBD";
          }

          public long getPackageSize() {
            return size;
          }

          public BigDecimal getPrice() {
            return BigDecimal.ZERO;
          }

          public boolean getRequireLicenseAcceptance() {
            return "true".equalsIgnoreCase(requireLicenseAcceptanse);
          }

          public boolean getIsLatestVersion() {
            //TODO
            return false;
          }

          public String getReleaseNotes() {
            //TODO:
            return null;
          }

          public double getVersionRating() {
            return 0;
          }

          public int getVersionRatingsCount() {
            return 0;
          }

          public Date getCreated() {
            return updated;
          }

          public Date getLastUpdated() {
            return updated;
          }

          public Date getPublished() {
            return updated;
          }

          public String getExternalPackageUrl() {
            return null;
          }

          public String getProjectUrl() {
            return projectUrl;
          }

          public String getLicenseUrl() {
            return licenseUrl;
          }

          public String getIconUrl() {
            return iconUrl;
          }

          public double getRating() {
            return 0;
          }

          public int getRatingsCount() {
            return 0;
          }

          public int getDownloadCount() {
            return 42;
          }

          public String getCategories() {
            return null;
          }

          public String getTags() {
            return tags;
          }

          public String getDependencies() {
            return dependencies;
          }

          public String getReportAbuseUrl() {
            return detailsUrl;
          }

          public String getGalleryDetailsUrl() {
            return detailsUrl;
          }
        };
      }
    };

  }

  @Nullable
  private String parseProperty(@NotNull final Element root, final @NotNull String name) {
    final Element child = getChild(getChild(root, "metadata"), name);
    return child == null ? null : child.getTextNormalize();
  }

  @Nullable
  private Element getChild(@Nullable final Element root, final String child) {
    if (root == null) return null;
    Element metadata = root.getChild(child);
    if (metadata != null) return metadata;
    return root.getChild(child, root.getNamespace(NS));
  }

  @NotNull
  private List<Element> getChildren(@Nullable final Element root, final String child) {
    if (root == null) return Collections.emptyList();
    List<Element> result = new ArrayList<Element>();
    for (List list : Arrays.asList(root.getChildren(child), root.getChildren(child, root.getNamespace(NS)))) {
      for (Object o : list) {
        result.add((Element)o);
      }
    }
    return result;
  }

  private String parseDependencies(@NotNull final Element root) {
    final Element metadata = getChild(root, "metadata");
    final Element dependencies = getChild(metadata, "dependencies");
    final StringBuilder sb = new StringBuilder();
    for (Object _dependency : getChildren(dependencies, "dependency")) {
      Element dep = (Element) _dependency;
      final String id = dep.getAttributeValue("id");
      final String versionConstraint = dep.getAttributeValue("version");
      if (sb.length() != 0) sb.append("|");
      sb.append(id).append(":").append(versionConstraint);
    }
    return sb.toString();
  }


  @Nullable
  private Element parseNuSpec(@NotNull final File nupkg) {
    //TODO: parse version number avay
    final String name = nupkg.getName();
    ZipInputStream zos = null;
    try {
      zos = new ZipInputStream(new BufferedInputStream(new FileInputStream(nupkg)));
      ZipEntry ze;
      while ((ze = zos.getNextEntry()) != null) {
        if (ze.getName().endsWith(".nuspec")) {
          try {
            return FileUtil.parseDocument(zos, false);
          } catch (JDOMException e) {
            LOG.warn("Failed to parse " + ze + " in " + nupkg);
          }
        }
      }
    } catch (IOException e) {
      LOG.warn("Failed to read " + nupkg + ". " + e.getMessage(), e);
    } finally {
      close(zos);
    }

    return null;
  }

  private void close(@Nullable final ZipInputStream zos) {
    if (zos != null) {
      try {
      zos.close();
      } catch (IOException e) {
        //NOP
      }
    }
  }
}
