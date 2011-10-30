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

package jetbrains.buildServer.nuget.server.feed.server.controllers;

import jetbrains.buildServer.RootUrlHolder;
import jetbrains.buildServer.nuget.server.feed.server.NuGetServerRunnerSettings;
import jetbrains.buildServer.serverSide.ServerPaths;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author Eugene Petrenko (eugene.petrenko@gmail.com)
 *         Date: 21.10.11 18:55
 */
public class MetadataControllerLocation implements NuGetServerRunnerSettings {
  private final RootUrlHolder myRootUrl;
  private final ServerPaths myPaths;
  private final MetadataControllersPaths myController;

  public MetadataControllerLocation(@NotNull final RootUrlHolder rootUrl,
                                    @NotNull final MetadataControllersPaths controller,
                                    @NotNull final ServerPaths paths) {
    myRootUrl = rootUrl;
    myController = controller;
    myPaths = paths;
  }

  @NotNull
  public String getPackagesControllerUrl() {
    return myRootUrl.getRootUrl() + myController.getBasePath();
  }

  @NotNull
  public File getLogsPath() {
    return myPaths.getLogsPath();
  }
}