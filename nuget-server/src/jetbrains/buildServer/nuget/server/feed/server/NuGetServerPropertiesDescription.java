/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package jetbrains.buildServer.nuget.server.feed.server;

import jetbrains.buildServer.nuget.common.NuGetServerConstants;
import jetbrains.buildServer.serverSide.parameters.AbstractParameterDescriptionProvider;
import org.jetbrains.annotations.NotNull;

/**
 * @author Eugene Petrenko (eugene.petrenko@gmail.com)
 *         Date: 09.02.12 18:55
 */
public class NuGetServerPropertiesDescription extends AbstractParameterDescriptionProvider {
  @Override
  public String describe(@NotNull String paramName) {
    if (NuGetServerConstants.FEED_REFERENCE.equals(paramName)) return "Contains URL to TeamCity provided NuGet feed";
    if (NuGetServerConstants.FEED_AUTH_REFERENCE_AGENT_PROVIDED.equals(paramName)) return "Contains URL to TeamCity provided NuGet feed with basic authentication";
    return null;
  }
}
