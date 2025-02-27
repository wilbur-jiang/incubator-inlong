/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.parser;

import java.util.ArrayList;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.inlong.common.enums.MetaField;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.formats.common.VarBinaryFormatInfo;
import org.apache.inlong.sort.parser.impl.FlinkSqlParser;
import org.apache.inlong.sort.parser.result.ParseResult;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.GroupInfo;
import org.apache.inlong.sort.protocol.MetaFieldInfo;
import org.apache.inlong.sort.protocol.StreamInfo;
import org.apache.inlong.sort.protocol.enums.ExtractMode;
import org.apache.inlong.sort.protocol.node.Node;
import org.apache.inlong.sort.protocol.node.extract.MySqlExtractNode;
import org.apache.inlong.sort.protocol.node.format.CsvFormat;
import org.apache.inlong.sort.protocol.node.load.KafkaLoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.relation.NodeRelation;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AllMigrateTest {

    private MySqlExtractNode buildAllMigrateExtractNode() {

        Map<String, String> option = new HashMap<>();
        option.put("append-mode", "true");
        option.put("migrate-all", "true");
        List<String> tables = new ArrayList(10);
        tables.add("test.*");
        List<FieldInfo> fields = Collections.singletonList(
            new MetaFieldInfo("data", MetaField.DATA));

        return new MySqlExtractNode("1", "mysql_input", fields,
                null, option, null,
            tables, "localhost", "root", "inlong",
                "test", null, null, false, null,
            ExtractMode.CDC, null,null);
    }

    private MySqlExtractNode buildAllMigrateExtractNodeWithBytesFormat() {
        List<FieldInfo> fields = Collections.singletonList(
            new MetaFieldInfo("data", MetaField.DATA_BYTES_DEBEZIUM));
        Map<String, String> option = new HashMap<>();
        option.put("append-mode", "true");
        option.put("migrate-all", "true");
        return new MySqlExtractNode("1", "mysql_input", fields,
            null, option, null,
            Collections.singletonList("test"), "localhost", "root", "inlong",
            "test", null, null, false, "UTC-8", ExtractMode.CDC,
            null, null);
    }

    private KafkaLoadNode buildAllMigrateKafkaNodeWithBytesFormat() {
        List<FieldInfo> fields = Collections.singletonList(
            new FieldInfo("data", new VarBinaryFormatInfo()));
        List<FieldRelation> relations = Collections.singletonList(
            new FieldRelation(new FieldInfo("data", new VarBinaryFormatInfo()),
                new FieldInfo("data", new VarBinaryFormatInfo())));
        CsvFormat csvFormat = new CsvFormat();
        csvFormat.setDisableQuoteCharacter(true);
        return new KafkaLoadNode("2", "kafka_output", fields, relations, null, null,
                "topic", "localhost:9092",
                csvFormat, null,
                null, null);
    }

    private KafkaLoadNode buildAllMigrateKafkaNode() {
        List<FieldInfo> fields = Collections.singletonList(
            new FieldInfo("data", new StringFormatInfo()));
        List<FieldRelation> relations = Collections.singletonList(
            new FieldRelation(new FieldInfo("data", new StringFormatInfo()),
                new FieldInfo("data", new StringFormatInfo())));
        CsvFormat csvFormat = new CsvFormat();
        csvFormat.setDisableQuoteCharacter(true);
        return new KafkaLoadNode("2", "kafka_output", fields, relations, null, null,
            "topic", "localhost:9092",
            csvFormat, null,
            null, null);
    }

    private NodeRelation buildNodeRelation(List<Node> inputs, List<Node> outputs) {
        List<String> inputIds = inputs.stream().map(Node::getId).collect(Collectors.toList());
        List<String> outputIds = outputs.stream().map(Node::getId).collect(Collectors.toList());
        return new NodeRelation(inputIds, outputIds);
    }

    /**
     * Test all migrate, the full database data is represented as a canal string
     *
     * @throws Exception The exception may throws when execute the case
     */
    @Test
    public void testAllMigrate() throws Exception {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node inputNode = buildAllMigrateExtractNode();
        Node outputNode = buildAllMigrateKafkaNode();
        StreamInfo streamInfo = new StreamInfo("1", Arrays.asList(inputNode, outputNode),
                Collections.singletonList(buildNodeRelation(Collections.singletonList(inputNode),
                        Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

    /**
     * Test all migrate, the full database data is represented as bytes of canal json
     *
     * @throws Exception The exception may throws when execute the case
     */
    @Test
    public void testAllMigrateWithBytesFormat() throws Exception {
        EnvironmentSettings settings = EnvironmentSettings
            .newInstance()
            .useBlinkPlanner()
            .inStreamingMode()
            .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node inputNode = buildAllMigrateExtractNodeWithBytesFormat();
        Node outputNode = buildAllMigrateKafkaNodeWithBytesFormat();
        StreamInfo streamInfo = new StreamInfo("1", Arrays.asList(inputNode, outputNode),
            Collections.singletonList(buildNodeRelation(Collections.singletonList(inputNode),
                Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

}
