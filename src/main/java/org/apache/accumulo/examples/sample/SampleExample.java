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

package org.apache.accumulo.examples.sample;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.accumulo.core.client.AccumuloClient;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.SampleNotPresentException;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.admin.CompactionConfig;
import org.apache.accumulo.core.client.admin.PluginConfig;
import org.apache.accumulo.core.client.sample.RowSampler;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.examples.Common;
import org.apache.accumulo.examples.cli.BatchWriterOpts;
import org.apache.accumulo.examples.cli.ClientOnDefaultTable;
import org.apache.accumulo.examples.client.RandomBatchWriter;
import org.apache.accumulo.examples.shard.CutoffIntersectingIterator;

import com.google.common.collect.ImmutableMap;

/**
 * A simple example of using Accumulo's sampling feature. This example does something similar to
 * what README.sample shows using the shell. Also, see {@link CutoffIntersectingIterator} and
 * README.sample for an example of how to use sample data from within an iterator.
 */
public class SampleExample {

  // a compaction strategy that only selects files for compaction that have no sample data or sample
  // data created in a different way than the tables
  static final PluginConfig selectorCfg = new PluginConfig(
      "org.apache.accumulo.tserver.compaction.strategies.ConfigurableCompactionStrategy",
      Map.of("SF_NO_SAMPLE", ""));
  static final CompactionConfig NO_SAMPLE_STRATEGY = new CompactionConfig()
      .setSelector(selectorCfg);

  static class Opts extends ClientOnDefaultTable {
    public Opts() {
      super("examples.sampex");
    }
  }

  public static void main(String[] args) throws Exception {
    Opts opts = new Opts();
    BatchWriterOpts bwOpts = new BatchWriterOpts();
    opts.parseArgs(RandomBatchWriter.class.getName(), args, bwOpts);

    try (AccumuloClient client = opts.createAccumuloClient()) {
      Common.createTableWithNamespace(client, opts.getTableName());

      // write some data
      try (
          BatchWriter bw = client.createBatchWriter(opts.getTableName(),
              bwOpts.getBatchWriterConfig());
          Scanner scanner = client.createScanner(opts.getTableName(), Authorizations.EMPTY)) {
        bw.addMutation(createMutation("9225", "abcde", "file://foo.txt"));
        bw.addMutation(createMutation("8934", "accumulo scales", "file://accumulo_notes.txt"));
        bw.addMutation(createMutation("2317", "milk, eggs, bread, parmigiano-reggiano",
            "file://groceries/9/txt"));
        bw.addMutation(createMutation("3900", "EC2 ate my homework", "file://final_project.txt"));
        bw.flush();

        SamplerConfiguration sc1 = new SamplerConfiguration(RowSampler.class.getName());
        sc1.setOptions(ImmutableMap.of("hasher", "murmur3_32", "modulus", "3"));

        client.tableOperations().setSamplerConfiguration(opts.getTableName(), sc1);

        System.out.println("Scanning all data :");
        print(scanner);
        System.out.println();

        System.out.println(
            "Scanning with sampler configuration.  Data was written before sampler was set on table, scan should fail.");
        scanner.setSamplerConfiguration(sc1);
        try {
          print(scanner);
        } catch (SampleNotPresentException e) {
          System.out.println("  Saw sample not present exception as expected.");
        }
        System.out.println();

        // compact table to recreate sample data
        client.tableOperations().compact(opts.getTableName(), NO_SAMPLE_STRATEGY);

        System.out
            .println("Scanning after compaction (compaction should have created sample data) : ");
        print(scanner);
        System.out.println();

        // update a document in the sample data
        bw.addMutation(createMutation("2317", "milk, eggs, bread, parmigiano-reggiano, butter",
            "file://groceries/9/txt"));

        System.out.println(
            "Scanning sample after updating content for docId 2317 (should see content change in sample data) : ");
        print(scanner);
        System.out.println();

        // change tables sampling configuration...
        SamplerConfiguration sc2 = new SamplerConfiguration(RowSampler.class.getName());
        sc2.setOptions(ImmutableMap.of("hasher", "murmur3_32", "modulus", "2"));
        client.tableOperations().setSamplerConfiguration(opts.getTableName(), sc2);
        // compact table to recreate sample data using new configuration
        client.tableOperations().compact(opts.getTableName(), NO_SAMPLE_STRATEGY);

        System.out.println(
            "Scanning with old sampler configuration.  Sample data was created using new configuration with a compaction.  Scan should fail.");
        try {
          // try scanning with old sampler configuration
          print(scanner);
        } catch (SampleNotPresentException e) {
          System.out.println("  Saw sample not present exception as expected ");
        }
        System.out.println();

        // update expected sampler configuration on scanner
        scanner.setSamplerConfiguration(sc2);

        System.out.println("Scanning with new sampler configuration : ");
        print(scanner);
        System.out.println();
      }
    }

  }

  private static void print(Scanner scanner) {
    for (Entry<Key,Value> entry : scanner) {
      System.out.println("  " + entry.getKey() + " " + entry.getValue());
    }
  }

  private static Mutation createMutation(String docId, String content, String url) {
    Mutation m = new Mutation(docId);
    m.put("doc", "context", content);
    m.put("doc", "url", url);
    return m;
  }
}
