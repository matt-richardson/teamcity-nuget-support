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

package jetbrains.buildServer.nuget.tests.integration.agent;

import com.intellij.execution.configurations.GeneralCommandLine;
import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.SimpleCommandLineProcessRunner;
import jetbrains.buildServer.TestNGUtil;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.nuget.agent.parameters.NuGetPublishParameters;
import jetbrains.buildServer.nuget.agent.runner.publish.PackagesPublishRunner;
import jetbrains.buildServer.nuget.tests.integration.IntegrationTestBase;
import jetbrains.buildServer.nuget.tests.integration.NuGet;
import jetbrains.buildServer.nuget.tests.integration.http.HttpAuthServer;
import jetbrains.buildServer.nuget.tests.integration.http.SimpleThreadedHttpServer;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.hamcrest.text.StringContains;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 22.07.11 1:25
 */
public class PackagesPublishIntegrationTest extends IntegrationTestBase {
  protected NuGetPublishParameters myPublishParameters;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myPublishParameters = m.mock(NuGetPublishParameters.class);
    m.checking(new Expectations(){{
      allowing(myLogger).activityStarted(with(equal("push")), with(any(String.class)), with(any(String.class)));
      allowing(myLogger).activityFinished(with(equal("push")), with(any(String.class)));
    }});
  }

  @Test(dataProvider = NUGET_VERSIONS_18p)
  public void test_publish_wrong_files(@NotNull final NuGet nuget) throws IOException, RunBuildException {

    m.checking(new Expectations(){{
      oneOf(myLogger).warning(with(new StringContains(".zpoo")));
    }});

    final File home = createTempDir();
    final File pkg1 = new File(home, "a.b.c.4.3.zpoo"){{createNewFile(); }};
    final BuildProcess p = callPublishRunnerEx(nuget, pkg1);
    assertRunSuccessfully(p, BuildFinishedStatus.FINISHED_FAILED);

    m.assertIsSatisfied();
  }

  @Test(dataProvider = NUGET_VERSIONS)
  public void test_publish_packages(@NotNull final NuGet nuget) throws IOException, RunBuildException {
    TestNGUtil.skip("this test will publish a package to preview nuget repo");
    final File pkg = preparePackage(nuget);
    callPublishRunner(nuget, pkg);

    Assert.assertTrue(getCommandsOutput().contains("Your package was uploaded") || getCommandsOutput().contains("Your package was pushed."));
  }


  @Test(dataProvider = NUGET_VERSIONS_20p)
  public void test_publish_packages_mock_http(@NotNull final NuGet nuget) throws IOException, RunBuildException {
    final AtomicBoolean hasPUT = new AtomicBoolean();
    final SimpleThreadedHttpServer server = new SimpleThreadedHttpServer(){
      @Override
      protected void postProcessSocketData(String httpHeader, @NotNull InputStream is) throws IOException {
        if (httpHeader.startsWith("PUT")) {
          while(is.available() > 0) {
            //noinspection ResultOfMethodCallIgnored
            is.read();
          }
        }
        super.postProcessSocketData(httpHeader, is);
      }

      @Override
      protected Response getResponse(String request) {
        System.out.println(">>" + request);
        if (request.startsWith("PUT")) {
          hasPUT.set(true);
          return createStringResponse(STATUS_LINE_201, Collections.<String>emptyList(), "Created");
        }
        return createStringResponse(STATUS_LINE_200,Collections.<String>emptyList(), "Ok");
      }
    };

    server.start();
    try {
      final File pkg = preparePackage(nuget);
      BuildProcess p = callPublishRunnerEx(nuget, "http://localhost:" + server.getPort() + "/nuget", pkg);
      assertRunSuccessfully(p, BuildFinishedStatus.FINISHED_SUCCESS);

      Assert.assertTrue(getCommandsOutput().contains("Your package was uploaded") || getCommandsOutput().contains("Your package was pushed."));
      Assert.assertTrue(hasPUT.get());
    } finally {
      server.stop();
    }
  }

  @Test(dataProvider = NUGET_VERSIONS_20p)
  public void test_publish_packages_mock_http_auth(@NotNull final NuGet nuget) throws IOException, RunBuildException {
    final String username = "u-" + StringUtil.generateUniqueHash();
    final String password = "p-" + StringUtil.generateUniqueHash();

    final AtomicBoolean hasPUT = new AtomicBoolean();
    final SimpleThreadedHttpServer server = new HttpAuthServer(){
      @Override
      protected void postProcessSocketData(String httpHeader, @NotNull InputStream is) throws IOException {
        if (httpHeader.startsWith("PUT")) {
          while(is.available() > 0) {
            //noinspection ResultOfMethodCallIgnored
            is.read();
          }
        }
        super.postProcessSocketData(httpHeader, is);
      }

      @Override
      protected boolean authorizeUser(@NotNull String loginPassword) {
        return (username + ":" + password).equals(loginPassword);
      }

      @Override
      protected Response getAuthorizedResponse(String request) throws IOException {
        System.out.println(">>" + request);
        if (request.startsWith("PUT")) {
          hasPUT.set(true);
          return createStringResponse(STATUS_LINE_201, Collections.<String>emptyList(), "Created");
        }
        return createStringResponse(STATUS_LINE_200,Collections.<String>emptyList(), "Ok");
      }
    };

    server.start();
    final String feed = "http://localhost:" + server.getPort() + "/nuget";

    addGlobalSource(feed, username, password);
    try {
      final File pkg = preparePackage(nuget);

      BuildProcess p = callPublishRunnerEx(nuget, feed, pkg);
      assertRunSuccessfully(p, BuildFinishedStatus.FINISHED_SUCCESS);

      Assert.assertTrue(getCommandsOutput().contains("Your package was uploaded") || getCommandsOutput().contains("Your package was pushed."));
      Assert.assertTrue(hasPUT.get());
    } finally {
      server.stop();
    }
  }

  @Test(dataProvider = NUGET_VERSIONS)
  public void test_create_mock_package(@NotNull final NuGet nuget) throws IOException {
    final File file = preparePackage(nuget);
    System.out.println(file);
  }

  private File preparePackage(@NotNull final NuGet nuget) throws IOException {
    @NotNull final File root = createTempDir();
    final File spec = new File(root, "SamplePackage.nuspec");
    FileUtil.copy(getTestDataPath("SamplePackage.nuspec"), spec);

    GeneralCommandLine cmd = new GeneralCommandLine();
    cmd.setExePath(nuget.getPath().getPath());
    cmd.setWorkingDirectory(root);
    cmd.addParameter("pack");
    cmd.addParameter(spec.getPath());
    cmd.addParameter("-Version");
    long time = System.currentTimeMillis();
    final long max = 65536;
    String build = "";
    for(int i = 0; i <4; i++) {
      build = (Math.max(1, time % max)) + (build.length() == 0 ? "" : "." + build);
      time /= max;
    }
    cmd.addParameter(build);
    cmd.addParameter("-Verbose");

    final ExecResult result = SimpleCommandLineProcessRunner.runCommand(cmd, new byte[0]);
    System.out.println(result.getStdout());
    System.out.println(result.getStderr());

    Assert.assertEquals(0, result.getExitCode());

    File pkg = new File(root, "jonnyzzz.nuget.teamcity.testPackage." + build + ".nupkg");
    Assert.assertTrue(pkg.isFile());
    return pkg;
  }

  private BuildProcess callPublishRunnerEx(@NotNull final NuGet nuget,
                                           @NotNull final File... pkg) throws RunBuildException {
    return callPublishRunnerEx(nuget, "http://preview.nuget.org/api/v2", pkg);
  }

  private BuildProcess callPublishRunnerEx(@NotNull final NuGet nuget,
                                           @NotNull final String source,
                                           @NotNull final File... pkg) throws RunBuildException {
    final List<String> files = new ArrayList<String>();
    for (File p : pkg) {
      files.add(p.getPath());
    }
    m.checking(new Expectations(){{
      allowing(myPublishParameters).getFiles(); will(returnValue(files));
      allowing(myPublishParameters).getCreateOnly(); will(returnValue(false));
      allowing(myPublishParameters).getNuGetExeFile(); will(returnValue(nuget.getPath()));
      allowing(myPublishParameters).getPublishSource(); will(returnValue(source));
      allowing(myPublishParameters).getApiKey(); will(returnValue(getQ()));

      allowing(myParametersFactory).loadPublishParameters(myContext);will(returnValue(myPublishParameters));
    }});

    final PackagesPublishRunner runner = new PackagesPublishRunner(myActionFactory, myParametersFactory);
    return runner.createBuildProcess(myBuild, myContext);
  }

  private void callPublishRunner(@NotNull final NuGet nuget, @NotNull final File pkg) throws RunBuildException {
    final BuildProcess proc = callPublishRunnerEx(nuget, pkg);
    assertRunSuccessfully(proc, BuildFinishedStatus.FINISHED_SUCCESS);
  }

  @NotNull
  private String getQ() {
    final int i1 = 88001628;
    final int universe = 42;
    final int num = 4015;
    final String nuget = 91 + "be" + "-" + num + "cf638bcf";
    return (i1 + "-" + "cb" + universe + "-" + 4 + "c") + 35 + "-" + nuget;
  }
}
