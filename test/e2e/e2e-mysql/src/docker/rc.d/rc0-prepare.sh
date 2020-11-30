#!/usr/bin/env bash
# Licensed to the SkyAPM under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

MYSQL_URL="http://central.maven.org/maven2/mysql/mysql-connector-java/8.0.13/mysql-connector-java-8.0.13.jar"
MYSQL_DRIVER="mysql-connector-java-8.0.13.jar"

echo "MySQL database is storage provider..."   
# Download MySQL connector.
curl ${MYSQL_URL} > "${SW_HOME}/oap-libs/${MYSQL_DRIVER}"
[[ $? -ne 0 ]] && echo "Fail to download ${MYSQL_DRIVER}." && exit 1

# Modify application.yml to set MySQL as storage provider.
cat /application.yml > "${SW_HOME}/config/application.yml"
