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

package jetbrains.buildServer.nuget.server.trigger.impl;

import jetbrains.buildServer.nuget.server.exec.ListPackagesCommand;
import jetbrains.buildServer.nuget.server.exec.SourcePackageInfo;
import jetbrains.buildServer.nuget.server.toolRegistry.NuGetInstalledTool;
import jetbrains.buildServer.nuget.server.toolRegistry.NuGetToolManager;
import jetbrains.buildServer.util.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @author Eugene Petrenko (eugene.petrenko@gmail.com)
 *         Date: 30.09.11 16:51
 */
public class PackageCheckerNuGet implements PackageChecker {
  private final ListPackagesCommand myCommand;
  private final NuGetToolManager myToolManager;

  public PackageCheckerNuGet(@NotNull final ListPackagesCommand command,
                             @NotNull final NuGetToolManager toolManager) {
    myCommand = command;
    myToolManager = toolManager;
  }

  public boolean accept(@NotNull PackageCheckRequest request) {
    return getNuGetPath(request) != null;
  }

  public void update(@NotNull ExecutorService executor, @NotNull Collection<PackageCheckEntry> data) {
    final MultiMap<File, PackageCheckEntry> entries = new MultiMap<File, PackageCheckEntry>();
    for (PackageCheckEntry entry : data) {
      entries.putValue(getNuGetPath(entry.getRequest()), entry);
    }

    //TODO: join into one request
    for (Map.Entry<File, List<PackageCheckEntry>> nuget : entries.entrySet()) {
      final File nugetPath = nuget.getKey();
      for (final PackageCheckEntry packageCheckEntry : nuget.getValue()) {
        packageCheckEntry.setExecuting();
        executor.execute(new Runnable() {
          public void run() {
            final PackageCheckRequest req = packageCheckEntry.getRequest();
            try {
              final Collection<SourcePackageInfo> infos = myCommand.checkForChanges(nugetPath, req.getPackageSource(), req.getPackageId(), req.getVersionSpec());
              packageCheckEntry.setResult(CheckResult.succeeded(infos));
            } catch (Throwable t) {
              packageCheckEntry.setResult(CheckResult.failed(t.getMessage()));
            }
          }
        });
      }
    }
  }

  @Nullable
  private File getNuGetPath(@NotNull PackageCheckRequest entry) {
    final CheckRequestMode mode = entry.getMode();
    if (mode instanceof CheckRequestModeNuGet) {
      return ((CheckRequestModeNuGet) mode).getNuGetPath();
    }

    final NuGetInstalledTool tool = myToolManager.getLatestNuGetTool();
    if (tool != null) {
      return tool.getPath();
    }

    return null;
  }
}
