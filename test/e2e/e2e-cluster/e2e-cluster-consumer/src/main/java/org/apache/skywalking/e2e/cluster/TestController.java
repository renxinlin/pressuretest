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
 *
 */

package org.apache.skywalking.e2e.cluster;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * @author kezhenxu94
 */
@RestController
@RequestMapping("/e2e")
public class TestController {
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String[] URL = {"http://127.0.0.1:9090/e2e/users", "http://127.0.0.1:9091/e2e/users"};

    @GetMapping("/health-check")
    public String hello() {
        return "healthy";
    }

    @PostMapping("/users")
    public User createAuthor(@RequestBody final User user) throws InterruptedException {
        Thread.sleep(1000L);
        ResponseEntity<User> response = null;
        for (String url : URL) {
            response = restTemplate.postForEntity(
                    url, user, User.class
            );
        }
        return response.getBody();
    }
}
