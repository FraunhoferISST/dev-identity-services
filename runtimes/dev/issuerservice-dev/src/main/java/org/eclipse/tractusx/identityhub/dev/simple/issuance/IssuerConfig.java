/*
 *   Copyright (c) 2025 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 *   Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 *   See the NOTICE file(s) distributed with this work for additional
 *   information regarding copyright ownership.
 *
 *   This program and the accompanying materials are made available under the
 *   terms of the Apache License, Version 2.0 which is available at
 *   https://www.apache.org/licenses/LICENSE-2.0.
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *   License for the specific language governing permissions and limitations
 *   under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.tractusx.identityhub.dev.simple.issuance;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A dataset that contains the credentialSubjects for the all the CredentialDefinitions, which the
 * issuer is willing to provide.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IssuerConfig {

    Map<String, HolderSettings> definitionsToSettingsMap = new HashMap<>();

    @JsonAnySetter
    public void addDefinition(String definitionName, HolderSettings settings) {
        definitionsToSettingsMap.put(definitionName, settings);
    }

    @JsonAnyGetter
    public Map<String, HolderSettings> getDefinitions() {
        return definitionsToSettingsMap;
    }

    public HolderSettings getDefinition(String definitionName) {
        return definitionsToSettingsMap.get(definitionName);
    }

    /**
     * Contains config data per holder id. May contain a "default" entry, that gets applied,
     * if holder is not blacklisted.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HolderSettings {

        @JsonProperty(value = "blackList")
        private List<String> blacklist = new ArrayList<>();

        private final Map<String, Map<String, Object>> idToHolderProperties = new HashMap<>();

        public boolean containsKey(String id) {
            return idToHolderProperties.containsKey(id);
        }

        public Map<String, Object> get(String id) {
            return idToHolderProperties.get(id);
        }

        public boolean blackListContains(String id) {
            return blacklist.contains(id);
        }

        @JsonAnyGetter
        public Map<String, Map<String, Object>> getEntries() {
            return idToHolderProperties;
        }

        @SuppressWarnings("unchecked")
        @JsonAnySetter
        public void addEntry(String id, Object value) {
            if (value instanceof Map<?, ?> m) {
                if (m.containsKey("credentialSubject")) {
                    Object cs = m.get("credentialSubject");
                    if (cs instanceof Map<?, ?>) {
                        idToHolderProperties
                                .computeIfAbsent(id, k -> new HashMap<>())
                                .put("credentialSubject", cs);
                    }
                }
            }
        }

        @Override
        public String toString() {
            return "HolderSettings{" +
                    "blacklist=" + blacklist +
                    ", idToHolderProperties=" + idToHolderProperties +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "IssuerConfig{" +
                "definitionsToSettingsMap=" + definitionsToSettingsMap +
                '}';
    }
}
