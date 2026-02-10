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

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGenerator;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A CredentialGenerator that will use the credentialSubject data defined via the
 * json input
 */
public class DevCredentialGenerator implements CredentialGenerator {

    private final CredentialGenerator wrappedCredentialGenerator;

    private final IssuerConfig defaultIssuerConfig;

    private final Monitor monitor;

    private Map<String, IssuerConfig> participantConfigDtos = new HashMap<>();
    private final ReadWriteLock configLock = new ReentrantReadWriteLock();

    DevCredentialGenerator(CredentialGenerator credentialGenerator, ServiceExtensionContext context, IssuerConfig issuerConfig) {
        this.wrappedCredentialGenerator = credentialGenerator;
        this.defaultIssuerConfig = issuerConfig;
        this.monitor = context.getMonitor().withPrefix(this.getClass().getSimpleName());
    }

    @Override
    public Result<VerifiableCredentialContainer> generateCredential(String participantContextId, CredentialDefinition definition, String privateKeyAlias, String publicKeyId, String issuerId, String participantId, Map<String, Object> claims) {
        var configClaims = retrieveForHolderId(participantId, definition.getId(), definition.getParticipantContextId());
        return wrappedCredentialGenerator.generateCredential(participantContextId, definition, privateKeyAlias, publicKeyId, issuerId, participantId, configClaims);
    }

    @Override
    public Result<String> signCredential(String participantContextId, VerifiableCredential credential, String privateKeyAlias, String publicKeyId) {
        return wrappedCredentialGenerator.signCredential(participantContextId, credential, privateKeyAlias, publicKeyId);
    }

    public void setNewConfigDto(IssuerConfig issuerConfig, String participantContextId) {
        try {
            configLock.writeLock().lock();
            this.participantConfigDtos.put(participantContextId, issuerConfig);
            monitor.info("New config has been set for " + participantContextId);
        } finally {
            configLock.writeLock().unlock();
        }
    }

    public IssuerConfig getExistingConfigDto(String participantContextId) {
        try {
            configLock.readLock().lock();
            return participantConfigDtos.computeIfAbsent(participantContextId, any -> defaultIssuerConfig);
        } finally {
            configLock.readLock().unlock();
        }
    }

    private Map<String, Object> retrieveForHolderId(String holderId, String credentialDefinitionId, String participantContextId) {
        String failureMessage;
        try {
            configLock.readLock().lock();
            IssuerConfig.HolderSettings settings = participantConfigDtos.computeIfAbsent(participantContextId, any -> defaultIssuerConfig).getDefinition(credentialDefinitionId);
            if (settings != null) {
                if (settings.containsKey(holderId) && settings.get(holderId).containsKey("credentialSubject")) {
                    return settings.get(holderId);
                } else if (settings.containsKey("default") && settings.get("default").containsKey("credentialSubject")) {
                    if (!settings.blackListContains(holderId)) {
                        return settings.get("default");
                    }
                }
            }
            failureMessage = "Rejecting request from " + holderId + " for " + credentialDefinitionId;
            monitor.severe(failureMessage);
        } finally {
            configLock.readLock().unlock();
        }
        return Map.of();
    }
}
