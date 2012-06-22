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

package jetbrains.buildServer.nuget.tests.agent.factory;

import jetbrains.buildServer.RunBuildException;
import org.jmock.Expectations;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static jetbrains.buildServer.agent.BuildFinishedStatus.FINISHED_SUCCESS;

/**
 * @author Eugene Petrenko (eugene.petrenko@gmail.com)
 *         Date: 22.06.12 15:26
 */
public class NuGetDeAuthorizeFactoryTest extends NuGetActionFactoryTestCase {

  @Test
  public void testDeAuthorizeCommand() throws IOException, RunBuildException {
    m.checking(new Expectations(){{
      oneOf(myProcessFactory).executeCommandLine(
              ctx,
              myNuGetRunnerPath.getPath(),
              Arrays.asList(
                      myNuGetPath.getPath(),
                      "TeamCity.DeAuthorizeFeed"
              ),
              myWorkDir,
              Collections.<String, String>emptyMap()
      );
      will(returnValue(createMockBuildProcess("run")));
    }});


    assertRunSuccessfully(i.createDeAuthenticateFeeds(ctx, nugetParams), FINISHED_SUCCESS);
    assertExecutedMockProcesses("run");
    m.assertIsSatisfied();

  }

}
