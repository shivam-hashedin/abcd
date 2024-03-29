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
import io.confluent.connect.azblob.storage.AzBlobStorage;
import io.confluent.connect.storage.format.Format;
import io.confluent.connect.storage.format.RecordWriterProvider;
import io.confluent.connect.storage.format.SchemaFileReader;
import io.confluent.connect.storage.hive.HiveFactory;

public class ParquetFormat implements Format<AzBlobSinkConnectorConfig, String> {

  private final AzBlobStorage storage;
  private final AvroData avroData;

  public ParquetFormat(AzBlobStorage storage) {
    this.storage = storage;
    this.avroData = new AvroData(
            storage.conf().getInt(AzBlobSinkConnectorConfig.SCHEMA_CACHE_SIZE_CONFIG)
    );
  }

  @Override
  public RecordWriterProvider<AzBlobSinkConnectorConfig> getRecordWriterProvider() {
    return new ParquetRecordWriterProvider(avroData);
  }

  @Override
  public SchemaFileReader<AzBlobSinkConnectorConfig, String> getSchemaFileReader() {
    throw new UnsupportedOperationException("Reading schemas from blob is not currently supported");
  }

  @Override
  public HiveFactory getHiveFactory() {
    throw new UnsupportedOperationException(
            "Hive integration is not currently supported in blob Connector");
  }

}
