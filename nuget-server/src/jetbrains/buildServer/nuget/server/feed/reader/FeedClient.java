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

package jetbrains.buildServer.nuget.server.feed.reader;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpConnectionParams;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 11.08.11 16:24
 */
public class FeedClient {
  private final HttpClient myClient;

  public FeedClient() {
    myClient = new DefaultHttpClient(new ThreadSafeClientConnManager());
    HttpConnectionParams.setConnectionTimeout(myClient.getParams(), 10000);
    HttpConnectionParams.setSoTimeout(myClient.getParams(), 10000);
  }

  @NotNull
  public HttpClient getClient() {
    return myClient;
  }

  public void dispose() {
    myClient.getConnectionManager().shutdown();
  }

}