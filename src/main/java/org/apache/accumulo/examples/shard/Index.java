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
package org.apache.accumulo.examples.shard;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.accumulo.core.client.Accumulo;
import org.apache.accumulo.core.client.AccumuloClient;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.examples.cli.ClientOpts;
import org.apache.hadoop.io.Text;

import com.beust.jcommander.Parameter;

/**
 * This program indexes a set of documents given on the command line into a shard table.
 *
 * What it writes to the table is row = partition id, column family = term, column qualifier =
 * document id.
 */
public class Index {

  static Text genPartition(int partition) {
    return new Text(String.format("%08x", Math.abs(partition)));
  }

  public static void index(int numPartitions, Text docId, String doc, String splitRegex,
      BatchWriter bw) throws Exception {

    String[] tokens = doc.split(splitRegex);

    Text partition = genPartition(doc.hashCode() % numPartitions);

    Mutation m = new Mutation(partition);

    HashSet<String> tokensSeen = new HashSet<>();

    for (String token : tokens) {
      token = token.toLowerCase();

      if (!tokensSeen.contains(token)) {
        tokensSeen.add(token);
        m.put(new Text(token), docId, new Value(new byte[0]));
      }
    }

    if (m.size() > 0)
      bw.addMutation(m);
  }

  public static void index(int numPartitions, File src, String splitRegex, BatchWriter bw)
      throws Exception {
    if (src.isDirectory()) {
      File[] files = src.listFiles();
      if (files != null) {
        for (File child : files) {
          index(numPartitions, child, splitRegex, bw);
        }
      }
    } else {
      FileReader fr = new FileReader(src);

      StringBuilder sb = new StringBuilder();

      char[] data = new char[4096];
      int len;
      while ((len = fr.read(data)) != -1) {
        sb.append(data, 0, len);
      }

      fr.close();

      index(numPartitions, new Text(src.getAbsolutePath()), sb.toString(), splitRegex, bw);
    }

  }

  static class IndexOpts extends ClientOpts {

    @Parameter(names = {"-t", "--table"}, required = true, description = "table to use")
    private String tableName;

    @Parameter(names = "--partitions", required = true,
        description = "the number of shards to create")
    int partitions;

    @Parameter(required = true, description = "<file> { <file> ... }")
    List<String> files = new ArrayList<>();
  }

  public static void main(String[] args) throws Exception {
    IndexOpts opts = new IndexOpts();
    opts.parseArgs(Index.class.getName(), args);

    String splitRegex = "\\W+";

    try (AccumuloClient client = Accumulo.newClient().from(opts.getClientPropsPath()).build();
        BatchWriter bw = client.createBatchWriter(opts.tableName)) {
      for (String filename : opts.files) {
        index(opts.partitions, new File(filename), splitRegex, bw);
      }
    }
  }
}
