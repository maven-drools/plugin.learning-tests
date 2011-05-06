/*
 * Copyright (c) 2009-2011 Ansgar Konermann
 *
 * This file is part of the Maven 3 Drools Plugin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.lightful.drools.apitests;

import de.lightful.maven.plugins.drools.knowledgeio.LogStream;

public class TestNGLogger implements LogStream<TestNGLogger> {

  private static final String NEWLINE = System.getProperty("line.separator");

  private org.testng.log4testng.Logger logger;
  private StringBuffer stringBuffer = new StringBuffer();

  public TestNGLogger(org.testng.log4testng.Logger logger) {
    this.logger = logger;
  }

  public TestNGLogger write(String message) {
    stringBuffer.append(message);
    if (message.endsWith(NEWLINE)) {
      return nl();
    }
    else {
      return this;
    }
  }

  public TestNGLogger nl() {
    logger.info(stringBuffer.toString());
    stringBuffer.setLength(0);
    return this;
  }
}
