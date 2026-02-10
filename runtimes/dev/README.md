# Dev runtimes for identity hub and issuerservice

This folder contains minimal versions of the identity hub and the issuer service, which are specifically meant serve the needs of developers who are working in X-projects in their initial steps towards defining their own credentials and want to test their EDC runtimes against a somewhat realistic identity environment, using for instance a locally running docker compose setup.

Before you go on, please make sure to have a Java JDK version 21 installed, when running these commands. Java 25 unfortunately won't work currently. 

In order to create the docker images, please run (from the project root directory)

```shell
./gradlew :runtimes:dev:identityhub-dev:dockerize
```

and 

```shell
./gradlew :runtimes:dev:issuerservice-dev:dockerize
```
respectively. 

It is absolutely not recommended for production use cases. Also, please note, that the solutions that were used especially in the issuerservice-dev could be considered a bit 'hacky' by some. And the internals of the upstream EDC, that these solutions are relying on may change in the future. Therefore, it can not be guaranteed that future EDC identityhub upstream version will be supported by these dev-runtimes. 

## The identityhub-dev

The identityhub-dev is using the tractus-x Super-User-Extension (though updated for upstream version 0.15.1). Beyond that, it applies a customized version of tractus-x's TxScopeToCriterionTransformer, that has basically just removed the scope filter. This means, you are no longer bound to a specific credential-type-prefix. This should be quite helpful for other X-projects, which might prefer not to use a tractus-X specific prefix. 

## The issuerservice-dev

The issuerservice-dev also leverages on the tractus-x Super-User-Extension. But its main feature is, that it allows you to 
freely define your own customized credentialSubject fields and experiment with them, without being bound to hard-coded contents like in the EDC's official [MVD](https://github.com/eclipse-edc/MinimumViableDataspace/tree/main). 

### Providing setup data

The main point is, that you can directly define, which holder-participant gets which credentialSubject (in the sense of a [verifiable credential](https://www.w3.org/TR/vc-data-model-2.0/)). 

For this, this issuerservice-dev contains an extension which comes with an API (usable at boot time or at runtime). 

#### Using a default setup

You have the option to give the issuerservice-dev a setup file at boot time. This means, you can for example mount a prepared
file into the docker container. Additionally, you have to notify the quickfix extension about that file, for example like this

`edc.ih.issuer.dev.defaultconfig=/app/setup.json`

When you are using a docker compose setup, you might consider a volume mount on the container of the issuerservice-dev. But 
this is not mandatory. You can skip this and just use the runtime configuration variant as explained below. 

#### Configuring the setup at runtime

The extension comes with a REST endpoint, where you can overwrite the current setup for your specific use case.

It is located under the issuer-admin, so assuming that you have not changed the default paths and ports, it should be
accessible under

`http://<issuer-service-host>:15152/api/issuer/v1alpha/credentialsetup/bXktaXNzdWVy`

This is a POST endpoint. You are expected to provide a request body of the content type "application/json" and you need
to authenticate yourself, by adding an x-api-key header, which is set by via the property: 

```
edc.ih.issuer.dev.issuance.apikey
```

The default value is set to "YWRtaW4.adminKey". 

Notice, that the last path segment (here: "bXktaXNzdWVy") is the participantContextId, encoded in Base 64. Please change
that according to the issuer-id, which you want to use in your demo scenario.

This also means, that you can provide different setups for each individual issuer participant (you can have more than one).

If you want to see, which setup you have currently configured for which issuer participant, you can do a GET request.
The URL is identical to the above-mentioned POST endpoint. Authentication is required here too.

#### The structure of the setup data

First, let's have a look at a simple example:

```
{
  "MC-Cred-Def": {

    "default": {
      "credentialSubject": {
        "isMember": true
      }
    },
    
    "blackList": [],

    "did:web:idhub-user1-service": {
      "credentialSubject": {
        "isMember": true,
        "anotherProperty": 43,
        "nestedProperty": {
          "foo": [
            1,
            "bar",
            false,
            {}
          ]
        }
      }
    }
  }
}
```

This JSON objects contains one field, whose key is indicating the credential definition id, for which the contained value
object is meant to provide data.

Notice that we have a default field. This field contains a "credentialSubject" entry, that will be applied to all potential holders,
which are requesting a credential for the given credential definition id.

Then, we have a "blackList". This list can be filled with the did:web-ids of participants, who you explicitly don't want to
give the default-credentialSubject be given to. I.E. unless you specify something especially for that participant, he won't
get any credential of that credential definition id.

Beyond that, you can add additional key-value pairs. The key should be the did:web-id of a certain participant. Within
the value-object, you should then enter an individualized credentialSubject, which will be used, when this specific participant
does a request for that credential definition id. Note, that this even overrides the blackList.

And of course, you can have more than just one credential definition id. I.e. you can do something like

```
{
  "MC-Cred-Def": {
    "blackList": [],
    "default": {
      "credentialSubject": {
        "isMember": true
      }
    },
    "did:web:idhub-user1-service": {
      "credentialSubject": {
        "isMember": true,
        "anotherProperty": 43,
        "nestedProperty": {
          "foo": [
            1,
            "bar",
            false,
            {}
          ]
        }
      }
    }
  },
  
  "New-Cred-Def": {
    "default": {
      "credentialSubject": {
        "someImportantData": [ "foo", "bar", false, {} ], 
        "additionalAttribute": { ... }
      }
    }
    ...
  }
}
```