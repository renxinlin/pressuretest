/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.skywalking.plugin.test.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

public class DockerContainerRunningGenerator extends AbstractRunningGenerator {
    private static Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    protected DockerContainerRunningGenerator() {
    }

    @Override
    public void generateAdditionFiles(IConfiguration configuration) {
        // DO Nothing
    }

    @Override
    public String runningScript(IConfiguration configuration) {
        Map<String, Object> root = new HashMap<>();
        root.put("agent_home", configuration.agentHome());
        root.put("scenario_home", configuration.scenarioHome());

        root.put("scenario_name", configuration.scenarioName());
        root.put("scenario_version", configuration.scenarioVersion());
        root.put("health_check", configuration.healthCheck());
        root.put("start_script", configuration.startScript());
        root.put("entry_service", configuration.entryService());
        root.put("test_framework", configuration.testFramework());
        root.put("docker_image_name", configuration.dockerImageName());
        root.put("docker_image_version", configuration.dockerImageVersion());
        root.put("docker_container_name", configuration.dockerContainerName());
        StringWriter out = null;

        try {
            out = new StringWriter();
            cfg.getTemplate("container-start-script.template").process(root, out);
        } catch (Exception e) {
            logger.error("Failed to generate running script.", e);
        }
        return out.toString();
    }
}
