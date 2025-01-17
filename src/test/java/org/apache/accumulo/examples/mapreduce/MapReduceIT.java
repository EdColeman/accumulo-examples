/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.examples.mapreduce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.accumulo.core.client.Accumulo;
import org.apache.accumulo.core.client.AccumuloClient;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.conf.ClientProperty;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.examples.ExamplesIT;
import org.apache.accumulo.miniclusterImpl.MiniAccumuloClusterImpl;
import org.apache.accumulo.miniclusterImpl.MiniAccumuloConfigImpl;
import org.apache.accumulo.test.functional.ConfigurableMacBase;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.junit.jupiter.api.Test;

public class MapReduceIT extends ConfigurableMacBase {

  @Override
  protected Duration defaultTimeout() {
    return Duration.ofMinutes(1);
  }

  @Override
  protected void configure(MiniAccumuloConfigImpl cfg, Configuration hadoopCoreSite) {
    cfg.setProperty(Property.TSERV_NATIVEMAP_ENABLED, "false");
  }

  public static final String hadoopTmpDirArg = "-Dhadoop.tmp.dir=" + System.getProperty("user.dir")
      + "/target/hadoop-tmp";

  static final String tablename = "mapredf";
  static final String input_cf = "cf-HASHTYPE";
  static final String input_cq = "cq-NOTHASHED";
  static final String input_cfcq = input_cf + ":" + input_cq;
  static final String output_cq = "cq-MD4BASE64";
  static final String output_cfcq = input_cf + ":" + output_cq;

  @Test
  public void test() throws Exception {
    String confFile = System.getProperty("user.dir") + "/target/accumulo-client.properties";
    Properties props = getClientProperties();
    String instance = ClientProperty.INSTANCE_NAME.getValue(props);
    String keepers = ClientProperty.INSTANCE_ZOOKEEPERS.getValue(props);
    ExamplesIT.writeClientPropsFile(confFile, instance, keepers, "root", ROOT_PASSWORD);
    try (AccumuloClient client = Accumulo.newClient().from(props).build()) {
      client.tableOperations().create(tablename);
      BatchWriter bw = client.createBatchWriter(tablename);
      for (int i = 0; i < 10; i++) {
        Mutation m = new Mutation("" + i);
        m.put(input_cf, input_cq, "row" + i);
        bw.addMutation(m);
      }
      bw.close();
      MiniAccumuloClusterImpl.ProcessInfo hash = getCluster().exec(RowHash.class,
          Collections.singletonList(hadoopTmpDirArg), "-c", confFile, "-t", tablename, "--column",
          input_cfcq);
      assertEquals(0, hash.getProcess().waitFor());

      Scanner s = client.createScanner(tablename, Authorizations.EMPTY);
      s.fetchColumn(new Text(input_cf), new Text(output_cq));
      int i = 0;
      for (Entry<Key,Value> entry : s) {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] check = Base64.getEncoder().encode(md.digest(("row" + i).getBytes()));
        assertEquals(entry.getValue().toString(), new String(check));
        i++;
      }
    }
  }
}
