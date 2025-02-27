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

package org.apache.inlong.manager.service.consume;

import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Factory for {@link InlongConsumeOperator}.
 */
@Service
public class InlongConsumeOperatorFactory {

    @Autowired
    private List<InlongConsumeOperator> consumeOperatorList;

    /**
     * Get an inlong consume operator instance via the given mqType
     */
    public InlongConsumeOperator getInstance(String mqType) {
        return consumeOperatorList.stream()
                .filter(inst -> inst.accept(mqType))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        String.format(ErrorCodeEnum.MQ_TYPE_NOT_SUPPORTED.getMessage(), mqType))
                );
    }
}
