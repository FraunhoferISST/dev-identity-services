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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.identityhub.spi.authorization.AuthorizationService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextId.onEncoded;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("v1alpha/credentialsetup/{participantContextId}")
public class DevController {

    private final AuthorizationService authorizationService;
    private final DevCredentialGenerator credentialGenerator;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;

    DevController(AuthorizationService authorizationService, DevCredentialGenerator credentialGenerator,
                  ServiceExtensionContext context, String apiKey) {
        this.credentialGenerator = credentialGenerator;
        this.authorizationService = authorizationService;
        this.apiKey = apiKey;
    }

    @POST
    public Response setCredentialSetupForParticipantContextId(@PathParam("participantContextId") String participantContextId,
                                                              IssuerConfig definitionDto, @HeaderParam("x-api-key") String givenApiKey) {
        if (!apiKey.equals(givenApiKey)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        var decodedParticipantId = onEncoded(participantContextId).orElseThrow(InvalidRequestException::new);
        try {
            credentialGenerator.setNewConfigDto(definitionDto, decodedParticipantId);
            return Response.accepted(objectMapper.writeValueAsString(definitionDto)).build();
        } catch (Exception e) {
            return Response.status(500).build();
        }
    }

    @GET
    public Response getCredentialSetupForParticipantContextId(@PathParam("participantContextId") String participantContextId,
                                                              @HeaderParam("x-api-key") String givenApiKey) throws JsonProcessingException {
        if (!apiKey.equals(givenApiKey)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        var decodedParticipantId = onEncoded(participantContextId).orElseThrow(InvalidRequestException::new);
        return Response.ok(objectMapper.writeValueAsString(credentialGenerator.getExistingConfigDto(decodedParticipantId))).build();

    }
}
