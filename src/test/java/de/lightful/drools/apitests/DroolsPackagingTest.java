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

import de.lightful.maven.plugins.drools.knowledgeio.KnowledgePackageFormatter;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderConfiguration;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.builder.conf.DumpDirOption;
import org.drools.core.util.DroolsStreamUtils;
import org.drools.definition.KnowledgePackage;
import org.drools.definition.type.FactType;
import org.drools.io.impl.ReaderResource;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.log4testng.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;

@Test
public class DroolsPackagingTest {

  private static Logger log = Logger.getLogger(DroolsPackagingTest.class);

  private static final String RULE_BASE_PATH = "src/test/rules";
  private byte[] dataTypesKnowledgePackages;
  private FactType personType;

  @BeforeMethod
  public void packageDataTypes() throws Exception {
    Collection<KnowledgePackage> dataTypesKnowledgeBase = packageRules(newKnowledgeBuilder(), "datatypes/person.drl");
    dataTypesKnowledgePackages = DroolsStreamUtils.streamOut(dataTypesKnowledgeBase, true);
  }

  private KnowledgeBuilder newKnowledgeBuilder() {
    final KnowledgeBuilderConfiguration configuration = configureDumpDirectory();
    return KnowledgeBuilderFactory.newKnowledgeBuilder(configuration);
  }

  private KnowledgeBuilderConfiguration configureDumpDirectory() {
    Properties properties = new Properties();
    final KnowledgeBuilderConfiguration configuration = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration(properties, (ClassLoader) null);
    configuration.setOption(DumpDirOption.get(new File("src/generated")));
    return configuration;
  }

  @Test
  public void test_can_reuse_existing_binary_package() throws Exception {
    Collection<KnowledgePackage> dataTypesKnowledgePackages = loadDataTypeKnowledge();
    KnowledgeBase existingKnowledgeBase = createKnowledgeBaseContaining(dataTypesKnowledgePackages);
    KnowledgeBuilder builderWithExistingKnowledge = KnowledgeBuilderFactory.newKnowledgeBuilder(existingKnowledgeBase, configureDumpDirectory());

    log.info("Existing Knowledge:\n");
    KnowledgePackageFormatter.dumpKnowledgePackages(new TestNGLogger(log), existingKnowledgeBase.getKnowledgePackages());

    Collection<KnowledgePackage> assessmentRulesPackage = packageRules(builderWithExistingKnowledge, "risk-assessment/check-age.drl");

    existingKnowledgeBase.addKnowledgePackages(assessmentRulesPackage);
    StatefulKnowledgeSession session = existingKnowledgeBase.newStatefulKnowledgeSession();
    session.insert(createPersonWithAge(existingKnowledgeBase, 16));
    session.fireAllRules();
    Collection<FactHandle> factHandles = session.getFactHandles();
    assertThat(factHandles.contains("VIOLATION: age < 18"));
  }

  private KnowledgeBase createKnowledgeBaseContaining(Collection<KnowledgePackage> dataTypesKnowledgePackages) {
    KnowledgeBase existingKnowledgeBase = KnowledgeBaseFactory.newKnowledgeBase();
    existingKnowledgeBase.addKnowledgePackages(dataTypesKnowledgePackages);
    return existingKnowledgeBase;
  }

  @SuppressWarnings("unchecked")
  private Collection<KnowledgePackage> loadDataTypeKnowledge() throws IOException, ClassNotFoundException {
    return (Collection<KnowledgePackage>) DroolsStreamUtils.streamIn(this.dataTypesKnowledgePackages, true);
  }

  private Object createPersonWithAge(KnowledgeBase kbase, int age) throws IllegalAccessException, InstantiationException {
    personType = kbase.getFactType("rules.datatypes.person", "Person");
    final Object person = personType.newInstance();
    personType.set(person, "age", age);
    return person;
  }

  private Collection<KnowledgePackage> packageRules(KnowledgeBuilder knowledgeBuilder, String... ruleFiles) throws Exception {
    for (String ruleFileName : ruleFiles) {
      File ruleFile = new File(RULE_BASE_PATH + File.separatorChar + ruleFileName);
      knowledgeBuilder.add(new ReaderResource(new FileReader(ruleFile)), ResourceType.DRL);
      if (knowledgeBuilder.hasErrors()) {
        handlerBuilderErrors(knowledgeBuilder, ruleFileName);
      }
    }
    return knowledgeBuilder.getKnowledgePackages();
  }

  private void handlerBuilderErrors(KnowledgeBuilder builder, String ruleFileName) {
    throw new RuntimeException("Error compiling file " + ruleFileName + ": " + builder.getErrors().toString());
  }
}
