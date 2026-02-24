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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.spi.authorization.AuthorizationService;
import org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext;
import org.eclipse.edc.issuerservice.issuance.generator.JwtCredentialGenerator;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry;
import org.eclipse.edc.jwt.signer.spi.JwsSignerProvider;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.web.spi.WebService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

public class SimpleIssuanceExtension implements ServiceExtension {

    public static final String NAME = "Demonstrator Credential Generator Extension";

    @Inject
    private WebService webService;

    @Inject
    private CredentialGeneratorRegistry credentialGeneratorRegistry;

    @Inject
    private JwsSignerProvider jwsSignerProvider;

    @Inject
    private Clock clock;

    @Setting(value = "Path to Json File, that holds pre-defined default config (optional)")
    public static final String PREDEFINED_CREDENTIALSUBJECTS = "edc.ih.issuer.dev.defaultconfig";

    @Setting(value = "Set the apiKey to access the DevController endpoints")
    public static final String API_KEY = "edc.ih.issuer.dev.issuance.apikey";


    @Override
    public void initialize(ServiceExtensionContext context) {
        // try to use given json-config
        IssuerConfig issuerConfig;
        try {
            String givenPath = context.getSetting(PREDEFINED_CREDENTIALSUBJECTS, null);
            context.getMonitor().withPrefix(this.getClass().getSimpleName()).info("Found givenPath for config" + givenPath);
            String fileAsString = Files.readString(Path.of(context.getSetting(PREDEFINED_CREDENTIALSUBJECTS, null)));
            issuerConfig = new ObjectMapper().readValue(fileAsString, IssuerConfig.class);
        } catch (Exception ex) {
            issuerConfig = new IssuerConfig();
            context.getMonitor().withPrefix(this.getClass().getSimpleName()).severe("Did not pick up an initial config, will use empty config", ex);
        }
        context.getMonitor().withPrefix(this.getClass().getSimpleName()).info("Using as default Config: " + issuerConfig);

        String apiKey = context.getSetting(API_KEY, "YWRtaW4.adminKey");

        // replace default JwtCredentialGenerator with DevCredentialGenerator
        JwtCredentialGenerator defaultJwtCredentialGenerator = new JwtCredentialGenerator(new JwtGenerationService(jwsSignerProvider), clock);
        var devCredentialGenerator = new DevCredentialGenerator(defaultJwtCredentialGenerator, context, issuerConfig);
        credentialGeneratorRegistry.addGenerator(CredentialFormat.VC1_0_JWT, devCredentialGenerator);

        // setup REST-Api controller for runtime management access
        DevController demonstratorController = new DevController(devCredentialGenerator, context, apiKey);
        webService.registerResource(IdentityHubApiContext.ISSUERADMIN, demonstratorController);
    }

    @Override
    public String name() {
        return NAME;
    }
}
