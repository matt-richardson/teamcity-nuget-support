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

package jetbrains.buildServer.nuget.tests.integration.feed.server;

import org.testng.annotations.Test;

/**
 * @author Evgeniy.Koshkin
 */
public class FindPackagesByIdFunctionIntegrationTest extends NuGetJavaFeedIntegrationTestBase {
  @Test
  public void testIdParameterCaseInsensitivity() throws Exception {
    enableDebug();
    addMockPackage("MyPackage", "1.0.0.0");
    assertContainsPackageVersion(openRequest("FindPackagesById()?id='MyPackage'"), "1.0.0.0");
    assertContainsPackageVersion(openRequest("FindPackagesById()?Id='MyPackage'"), "1.0.0.0");
    assert204("FindPackagesById()?ID='MyPackage'");
  }
}
