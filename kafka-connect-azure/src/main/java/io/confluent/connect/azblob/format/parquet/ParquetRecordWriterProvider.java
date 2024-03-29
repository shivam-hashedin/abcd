/*
 * Copyright 2019 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.connect.azblob.format.parquet;

import io.confluent.connect.avro.AvroData;
import io.confluent.connect.azblob.AzBlobSinkConnectorConfig;
import java.io.IOException;

import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.confluent.connect.storage.format.RecordWriter;
import io.confluent.connect.storage.format.RecordWriterProvider;

public class ParquetRecordWriterProvider
        implements RecordWriterProvider<AzBlobSinkConnectorConfig> {
  private static final Logger log = LoggerFactory.getLogger(ParquetRecordWriterProvider.class);
  private static final String EXTENSION = ".parquet";
  private final AvroData avroData;

  ParquetRecordWriterProvider(AvroData avroData) {
    this.avroData = avroData;
  }

  @Override
  public String getExtension() {
    return EXTENSION;
  }

  @Override
  public RecordWriter getRecordWriter(
          final AzBlobSinkConnectorConfig conf,
          final String filename) {
    return new RecordWriter() {
      public ParquetWriter<GenericRecord> writer;
      final CompressionCodecName compressionCodecName = CompressionCodecName.SNAPPY;
      final int blockSize = 256 * 1024 * 1024;
      final int pageSize = 64 * 1024;
      Schema schema = null;
      org.apache.hadoop.fs.Path hdfsppath = new org.apache.hadoop.fs.Path(filename);
      org.apache.avro.Schema avroSchema = null;


      @Override
      public void write(SinkRecord record) {
        if (avroSchema == null) {
          schema = record.valueSchema();

          try {
            log.info("Opening record writer for: {}", filename);
            avroSchema = avroData.fromConnectSchema(schema);
            writer = AvroParquetWriter
                    .<GenericRecord>builder(hdfsppath)
                    .withSchema(avroSchema)
                    .withConf(new Configuration())
                    .withCompressionCodec(compressionCodecName)
                    .withPageSize(pageSize)
                    .build();

          } catch (IOException e) {
            throw new ConnectException(e);
          }
        }

        log.trace("Sink record: {}", record.toString());

        Object value = avroData.fromConnectData(record.valueSchema(), record.value());
        try {
          writer.write((GenericRecord) value);
        } catch (IOException e) {
          throw new ConnectException(e);
        }
      }

      @Override
      public void close() {
        if (writer != null) {
          try {
            writer.close();
          } catch (IOException e) {
            throw new ConnectException(e);
          }
        }
      }

      @Override
      public void commit() {
      }
    };
  }
}