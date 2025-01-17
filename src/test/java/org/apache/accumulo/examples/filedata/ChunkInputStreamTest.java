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
package org.apache.accumulo.examples.filedata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.KeyValue;
import org.apache.accumulo.core.data.Value;
import org.apache.hadoop.io.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

public class ChunkInputStreamTest {
  private static final Logger log = LoggerFactory.getLogger(ChunkInputStream.class);
  private List<Entry<Key,Value>> data;
  private List<Entry<Key,Value>> baddata;
  private List<Entry<Key,Value>> multidata;

  @BeforeEach
  public void setupData() {
    data = new ArrayList<>();
    addData(data, "a", "refs", "id\0ext", "A&B", "ext");
    addData(data, "a", "refs", "id\0name", "A&B", "name");
    addData(data, "a", "~chunk", 100, 0, "A&B", "asdfjkl;");
    addData(data, "a", "~chunk", 100, 1, "A&B", "");
    addData(data, "b", "refs", "id\0ext", "A&B", "ext");
    addData(data, "b", "refs", "id\0name", "A&B", "name");
    addData(data, "b", "~chunk", 100, 0, "A&B", "qwertyuiop");
    addData(data, "b", "~chunk", 100, 0, "B&C", "qwertyuiop");
    addData(data, "b", "~chunk", 100, 1, "A&B", "");
    addData(data, "b", "~chunk", 100, 1, "B&C", "");
    addData(data, "b", "~chunk", 100, 1, "D", "");
    addData(data, "c", "~chunk", 100, 0, "A&B", "asdfjkl;");
    addData(data, "c", "~chunk", 100, 1, "A&B", "asdfjkl;");
    addData(data, "c", "~chunk", 100, 2, "A&B", "");
    addData(data, "d", "~chunk", 100, 0, "A&B", "");
    addData(data, "e", "~chunk", 100, 0, "A&B", "asdfjkl;");
    addData(data, "e", "~chunk", 100, 1, "A&B", "");
    baddata = new ArrayList<>();
    addData(baddata, "a", "~chunk", 100, 0, "A", "asdfjkl;");
    addData(baddata, "b", "~chunk", 100, 0, "B", "asdfjkl;");
    addData(baddata, "b", "~chunk", 100, 2, "C", "");
    addData(baddata, "c", "~chunk", 100, 0, "D", "asdfjkl;");
    addData(baddata, "c", "~chunk", 100, 2, "E", "");
    addData(baddata, "d", "~chunk", 100, 0, "F", "asdfjkl;");
    addData(baddata, "d", "~chunk", 100, 1, "G", "");
    addData(baddata, "d", "~zzzzz", "colq", "H", "");
    addData(baddata, "e", "~chunk", 100, 0, "I", "asdfjkl;");
    addData(baddata, "e", "~chunk", 100, 1, "J", "");
    addData(baddata, "e", "~chunk", 100, 2, "I", "asdfjkl;");
    addData(baddata, "f", "~chunk", 100, 2, "K", "asdfjkl;");
    addData(baddata, "g", "~chunk", 100, 0, "L", "");
    multidata = new ArrayList<>();
    addData(multidata, "a", "~chunk", 100, 0, "A&B", "asdfjkl;");
    addData(multidata, "a", "~chunk", 100, 1, "A&B", "");
    addData(multidata, "a", "~chunk", 200, 0, "B&C", "asdfjkl;");
    addData(multidata, "b", "~chunk", 100, 0, "A&B", "asdfjkl;");
    addData(multidata, "b", "~chunk", 200, 0, "B&C", "asdfjkl;");
    addData(multidata, "b", "~chunk", 200, 1, "B&C", "asdfjkl;");
    addData(multidata, "c", "~chunk", 100, 0, "A&B", "asdfjkl;");
    addData(multidata, "c", "~chunk", 100, 1, "B&C", "");
  }

  private static void addData(List<Entry<Key,Value>> data, String row, String cf, String cq,
      String vis, String value) {
    data.add(new KeyValue(new Key(new Text(row), new Text(cf), new Text(cq), new Text(vis)),
        value.getBytes()));
  }

  private static void addData(List<Entry<Key,Value>> data, String row, String cf, int chunkSize,
      int chunkCount, String vis, String value) {
    Text chunkCQ = new Text(FileDataIngest.intToBytes(chunkSize));
    chunkCQ.append(FileDataIngest.intToBytes(chunkCount), 0, 4);
    data.add(new KeyValue(new Key(new Text(row), new Text(cf), chunkCQ, new Text(vis)),
        value.getBytes()));
  }

  @Test
  public void testExceptionOnMultipleSetSourceWithoutClose() throws IOException {
    ChunkInputStream cis = new ChunkInputStream();
    PeekingIterator<Entry<Key,Value>> pi = Iterators.peekingIterator(data.iterator());
    cis.setSource(pi);
    try {
      cis.setSource(pi);
      fail();
    } catch (IOException e) {
      /* expected */
    }
    cis.close();
  }

  @Test
  public void testExceptionOnGetVisBeforeClose() throws IOException {
    ChunkInputStream cis = new ChunkInputStream();
    PeekingIterator<Entry<Key,Value>> pi = Iterators.peekingIterator(data.iterator());

    cis.setSource(pi);
    try {
      cis.getVisibilities();
      fail();
    } catch (RuntimeException e) {
      /* expected */
    }
    cis.close();
    cis.getVisibilities();
  }

  @Test
  public void testReadIntoBufferSmallerThanChunks() throws IOException {
    ChunkInputStream cis = new ChunkInputStream();
    byte[] b = new byte[5];

    PeekingIterator<Entry<Key,Value>> pi = Iterators.peekingIterator(data.iterator());

    cis.setSource(pi);
    int read;
    assertEquals(read = cis.read(b), 5);
    assertEquals(new String(b, 0, read), "asdfj");
    assertEquals(read = cis.read(b), 3);
    assertEquals(new String(b, 0, read), "kl;");
    assertEquals(read = cis.read(b), -1);

    cis.setSource(pi);
    assertEquals(read = cis.read(b), 5);
    assertEquals(new String(b, 0, read), "qwert");
    assertEquals(read = cis.read(b), 5);
    assertEquals(new String(b, 0, read), "yuiop");
    assertEquals(read = cis.read(b), -1);
    assertEquals(cis.getVisibilities().toString(), "[A&B, B&C, D]");
    cis.close();

    cis.setSource(pi);
    assertEquals(read = cis.read(b), 5);
    assertEquals(new String(b, 0, read), "asdfj");
    assertEquals(read = cis.read(b), 5);
    assertEquals(new String(b, 0, read), "kl;as");
    assertEquals(read = cis.read(b), 5);
    assertEquals(new String(b, 0, read), "dfjkl");
    assertEquals(read = cis.read(b), 1);
    assertEquals(new String(b, 0, read), ";");
    assertEquals(read = cis.read(b), -1);
    assertEquals(cis.getVisibilities().toString(), "[A&B]");
    cis.close();

    cis.setSource(pi);
    assertEquals(read = cis.read(b), -1);
    cis.close();

    cis.setSource(pi);
    assertEquals(read = cis.read(b), 5);
    assertEquals(new String(b, 0, read), "asdfj");
    assertEquals(read = cis.read(b), 3);
    assertEquals(new String(b, 0, read), "kl;");
    assertEquals(read = cis.read(b), -1);
    cis.close();

    assertFalse(pi.hasNext());
  }

  @Test
  public void testReadIntoBufferLargerThanChunks() throws IOException {
    ChunkInputStream cis = new ChunkInputStream();
    byte[] b = new byte[20];
    int read;
    PeekingIterator<Entry<Key,Value>> pi = Iterators.peekingIterator(data.iterator());

    cis.setSource(pi);
    assertEquals(read = cis.read(b), 8);
    assertEquals(new String(b, 0, read), "asdfjkl;");
    assertEquals(read = cis.read(b), -1);

    cis.setSource(pi);
    assertEquals(read = cis.read(b), 10);
    assertEquals(new String(b, 0, read), "qwertyuiop");
    assertEquals(read = cis.read(b), -1);
    assertEquals(cis.getVisibilities().toString(), "[A&B, B&C, D]");
    cis.close();

    cis.setSource(pi);
    assertEquals(read = cis.read(b), 16);
    assertEquals(new String(b, 0, read), "asdfjkl;asdfjkl;");
    assertEquals(read = cis.read(b), -1);
    assertEquals(cis.getVisibilities().toString(), "[A&B]");
    cis.close();

    cis.setSource(pi);
    assertEquals(read = cis.read(b), -1);
    cis.close();

    cis.setSource(pi);
    assertEquals(read = cis.read(b), 8);
    assertEquals(new String(b, 0, read), "asdfjkl;");
    assertEquals(read = cis.read(b), -1);
    cis.close();

    assertFalse(pi.hasNext());
  }

  private static void assumeExceptionOnRead(ChunkInputStream cis, byte[] b) {
    try {
      assertEquals(0, cis.read(b));
      fail();
    } catch (IOException e) {
      log.debug("EXCEPTION {}", e.getMessage());
      // expected, ignore
    }
  }

  private static void assumeExceptionOnClose(ChunkInputStream cis) {
    try {
      cis.close();
      fail();
    } catch (IOException e) {
      log.debug("EXCEPTION {}", e.getMessage());
      // expected, ignore
    }
  }

  @Test
  public void testBadData() throws IOException {
    ChunkInputStream cis = new ChunkInputStream();
    byte[] b = new byte[20];
    int read;
    PeekingIterator<Entry<Key,Value>> pi = Iterators.peekingIterator(baddata.iterator());

    cis.setSource(pi);
    assumeExceptionOnRead(cis, b);
    assumeExceptionOnClose(cis);
    // can still get visibilities after exception -- bad?
    assertEquals(cis.getVisibilities().toString(), "[A]");

    cis.setSource(pi);
    assumeExceptionOnRead(cis, b);
    assumeExceptionOnClose(cis);
    assertEquals(cis.getVisibilities().toString(), "[B, C]");

    cis.setSource(pi);
    assumeExceptionOnRead(cis, b);
    assumeExceptionOnClose(cis);
    assertEquals(cis.getVisibilities().toString(), "[D, E]");

    cis.setSource(pi);
    assertEquals(read = cis.read(b), 8);
    assertEquals(new String(b, 0, read), "asdfjkl;");
    assertEquals(read = cis.read(b), -1);
    assertEquals(cis.getVisibilities().toString(), "[F, G]");
    cis.close();

    cis.setSource(pi);
    assumeExceptionOnRead(cis, b);
    cis.close();
    assertEquals(cis.getVisibilities().toString(), "[I, J]");

    try {
      cis.setSource(pi);
      fail();
    } catch (IOException e) {
      // expected, ignore
    }
    assumeExceptionOnClose(cis);
    assertEquals(cis.getVisibilities().toString(), "[K]");

    cis.setSource(pi);
    assertEquals(read = cis.read(b), -1);
    assertEquals(cis.getVisibilities().toString(), "[L]");
    cis.close();

    assertFalse(pi.hasNext());

    pi = Iterators.peekingIterator(baddata.iterator());
    cis.setSource(pi);
    assumeExceptionOnClose(cis);
  }

  @Test
  public void testBadDataWithoutClosing() throws IOException {
    ChunkInputStream cis = new ChunkInputStream();
    byte[] b = new byte[20];
    int read;
    PeekingIterator<Entry<Key,Value>> pi = Iterators.peekingIterator(baddata.iterator());

    cis.setSource(pi);
    assumeExceptionOnRead(cis, b);
    // can still get visibilities after exception -- bad?
    assertEquals(cis.getVisibilities().toString(), "[A]");

    cis.setSource(pi);
    assumeExceptionOnRead(cis, b);
    assertEquals(cis.getVisibilities().toString(), "[B, C]");

    cis.setSource(pi);
    assumeExceptionOnRead(cis, b);
    assertEquals(cis.getVisibilities().toString(), "[D, E]");

    cis.setSource(pi);
    assertEquals(read = cis.read(b), 8);
    assertEquals(new String(b, 0, read), "asdfjkl;");
    assertEquals(read = cis.read(b), -1);
    assertEquals(cis.getVisibilities().toString(), "[F, G]");
    cis.close();

    cis.setSource(pi);
    assumeExceptionOnRead(cis, b);
    assertEquals(cis.getVisibilities().toString(), "[I, J]");

    try {
      cis.setSource(pi);
      fail();
    } catch (IOException e) {
      // expected, ignore
    }
    assertEquals(cis.getVisibilities().toString(), "[K]");

    cis.setSource(pi);
    assertEquals(read = cis.read(b), -1);
    assertEquals(cis.getVisibilities().toString(), "[L]");
    cis.close();

    assertFalse(pi.hasNext());

    pi = Iterators.peekingIterator(baddata.iterator());
    cis.setSource(pi);
    assumeExceptionOnClose(cis);
  }

  @Test
  public void testMultipleChunkSizes() throws IOException {
    ChunkInputStream cis = new ChunkInputStream();
    byte[] b = new byte[20];
    int read;
    PeekingIterator<Entry<Key,Value>> pi = Iterators.peekingIterator(multidata.iterator());

    b = new byte[20];

    cis.setSource(pi);
    assertEquals(read = cis.read(b), 8);
    assertEquals(read = cis.read(b), -1);
    cis.close();
    assertEquals(cis.getVisibilities().toString(), "[A&B]");

    cis.setSource(pi);
    assumeExceptionOnRead(cis, b);
    assertEquals(cis.getVisibilities().toString(), "[A&B]");

    cis.setSource(pi);
    assertEquals(read = cis.read(b), 8);
    assertEquals(new String(b, 0, read), "asdfjkl;");
    assertEquals(read = cis.read(b), -1);
    cis.close();
    assertEquals(cis.getVisibilities().toString(), "[A&B, B&C]");

    assertFalse(pi.hasNext());
  }

  @Test
  public void testSingleByteRead() throws IOException {
    ChunkInputStream cis = new ChunkInputStream();
    PeekingIterator<Entry<Key,Value>> pi = Iterators.peekingIterator(data.iterator());

    cis.setSource(pi);
    assertEquals((byte) 'a', (byte) cis.read());
    assertEquals((byte) 's', (byte) cis.read());
    assertEquals((byte) 'd', (byte) cis.read());
    assertEquals((byte) 'f', (byte) cis.read());
    assertEquals((byte) 'j', (byte) cis.read());
    assertEquals((byte) 'k', (byte) cis.read());
    assertEquals((byte) 'l', (byte) cis.read());
    assertEquals((byte) ';', (byte) cis.read());
    assertEquals(cis.read(), -1);
    cis.close();
    assertEquals(cis.getVisibilities().toString(), "[A&B]");
  }
}
