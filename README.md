# Manage Pensions Frontend 

- [Overview](#overview)
- [Requirements](#requirements)
- [Running the Service](#running-the-service)
- [Enrolments](#enrolments)
- [Compile & Test](#compile--test)
- [Navigation and Dependent Services](#navigation-and-dependent-services)
- [Service Documentation](#service-documentation)
- [Endpoints](#endpoints)
- [License](#license)

## Overview

This is the frontend repository for the Manage Pensions service. This frontend microservice is also known as *Manage pension schemes*. The service allows an individual to invite a person to become a pension administrator, accept an invitation to become a pension scheme administrator, remove themselves from a pension scheme and deregister as a pension scheme administrator.
 
This service also redirects a user to other services to do the following things -
1. Register as a pension scheme administrator
2. Edit their pension administrator details
3. Start registering a new pension scheme
4. Make changes to an existing pension scheme
5. Start filing an Accounting for Tax (AFT) return
6. Amend an existing Accounting for Tax (AFT) return

This service does not have its own backend, instead it uses the following backends and endpoints for its integration:

### Dependencies

| Service                           | Link                                                      |
|-----------------------------------|-----------------------------------------------------------|
| pension-administrator             | https://github.com/hmrc/pension-administrator             |
| pensions-scheme                   | https://github.com/hmrc/pensions-scheme                   |
| pension-scheme-accounting-for-tax | https://github.com/hmrc/pension-scheme-accounting-for-tax |

### Endpoints Used

| Service                           | HTTP Method | Route                                               | Purpose                                                                                                                  |
|-----------------------------------|-------------|-----------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| Pension Administrator             | POST        | /pension-administrator/invite                       | Stores an invitation in the invitations mongo collection                                                                 |
| Pension Administrator             | POST        | /pension-administrator/accept-invitation            | Creates an association of the PSA with the scheme and removes invitation                                                 |
| Pension Administrator             | POST        | /pension-administrator/invite                       | Stores an invitation in the invitations mongo collection                                                                 |
| Pension Administrator             | POST        | /pension-administrator/remove-psa                   | Removes the association between PSA and pension scheme                                                                   |
| Pension Administrator             | POST        | /pension-administrator/deregister-psa/{psaId}       | De-enrols a PSA from the service                                                                                         |
| Pension Administrator             | GET         | /pension-administrator/can-deregister/{psaId}       | Returns true is PSA has already dissociated themselves from all active schemes and false if any such associations remain |
| Pension Administrator             | GET         | /pension-administrator/get-minimal-psa              | Returns minimal details of the PSA                                                                                       |
| Pensions Scheme                   | GET         | /pensions-scheme/list-of-schemes                    | Returns a list of all the schemes that the PSA is associated to                                                          |
| Pensions Scheme                   | GET         | /pensions-scheme/scheme                             | Returns pension scheme details of the selected scheme                                                                    |
| Pensions Scheme                   | GET         | /update-scheme/get-lock                             | Returns an edit lock placed on the selected scheme, if present                                                           |
| Pensions Scheme                   | GET         | /update-scheme/get-lock-by-psa                      | Returns an edit lock for the given PSA-scheme combination, if present                                                    |
| Pensions Scheme                   | GET         | /update-scheme/get-lock-by-scheme                   | Returns an edit lock placed on the selected scheme by any PSA                                                            |
| Pensions Scheme                   | GET         | /update-scheme/isLockByPsaOrScheme                  | Returns an edit lock placed either on the selected scheme or by the logged in PSA                                        |
| Pensions Scheme                   | DELETE      | /update-scheme/release-lock                         | Release the edit lock on the scheme                                                                                      |
| Pension Scheme Accounting For Tax | GET         | /pension-scheme-accounting-for-tax/get-aft-versions | Return the data from all versions of aft returns of a scheme for a given quarter                                         |
| Pension Scheme Accounting For Tax | GET         | /pension-scheme-accounting-for-tax/get-aft-overview | Return the data overview of all aft returns of a scheme for a given time range                                           |
| Address Lookup                    | GET         | /v2/uk/addresses?postcode={postCode}                | Returns all addresses at given postcode                                                                                  |


## Requirements
This service is written in Scala and Play, so needs at least a [JRE] to run.

**Node version:** 16.20.2

**Java version:** 11

**Scala version:** 2.13.14


## Running the Service
**Service Manager Profile:** PODS_ALL

**Port:** 8205

In order to run the service, ensure Service Manager is installed (see [MDTP guidance](https://docs.tax.service.gov.uk/mdtp-handbook/documentation/developer-set-up/set-up-service-manager.html) if needed) and launch the relevant configuration by typing into the terminal:
`sm2 --start PODS_ALL`

To run the service locally, enter `sm2 --stop PENSION_ADMINISTRATOR`.

In your terminal, navigate to the relevant directory and enter `sbt run`.

Access the Authority Wizard and login with the relevant enrolment details [here](http://localhost:9949/auth-login-stub/gg-sign-in)


## Enrolments
There are several different options for enrolling through the auth login stub. In order to enrol as a dummy user to access the platform for local development and testing purposes, the following details must be entered on the auth login page.


For access to the **Pension Administrator dashboard** for local development, enter the following information: 

**Redirect url -** http://localhost:8204/manage-pension-schemes/overview 

**GNAP Token -** NO 

**Affinity Group -** Organisation 

**Enrolment Key -** HMRC-PODS-ORG 

**Identifier Name -** PsaID 

**Identifier Value -** A2100005

---

If you wish to access the **Pension Practitioner dashboard** for local development, enter the following information: 

**Redirect URL -** http://localhost:8204/manage-pension-schemes/dashboard 

**GNAP Token -** NO 

**Affinity Group -** Organisation 

**Enrolment Key -** HMRC-PODSPP-ORG 

**Identifier Name -** PspID 

**Identifier Value -** 21000005

---

**Dual enrolment** as both a Pension Administrator and Practitioner is also possible and can be accessed by entering:

**Redirect url -** http://localhost:8204/manage-pension-schemes/overview 

**GNAP Token -** NO 

**Affinity Group -** Organisation 

**Enrolment Key 1 -** HMRC-PODSPP-ORG Identifier 

**Name 1 -** PspID Identifier 

**Value 1 -** 21000005

**Enrolment Key 2 -** HMRC-PODS-ORG 

**Identifier Name 2 -** PsaID 

**Identifier Value 2 -** A2100005

---

To access the **Scheme Registration journey**, enter the following information:

**Redirect URL -** http://localhost:8204/manage-pension-schemes/you-need-to-register 

**GNAP Token -** NO 

**Affinity Group -** Organisation

---


## Compile & Test
**To compile:** Run `sbt compile`

**To test:** Use `sbt test`

**To view test results with coverage:** Run `sbt clean coverage test coverageReport`

For further information on the PODS Test Approach and wider testing including acceptance, accessibility, performance, security and E2E testing, visit the PODS Confluence page [here](https://confluence.tools.tax.service.gov.uk/pages/viewpage.action?spaceKey=PODSP&title=PODS+Test+Approach).

For Journey Tests, visit the [Journey Test Repository](| Journey tests(https://github.com/hmrc/pods-journey-tests).

View the prototype [here](https://pods-event-reporting-prototype.herokuapp.com/).


## Navigation
The Manage Pensions Frontend integrates with the Manage Pension Schemes (MPS) service and uses various stubs available on [GitHub](https://github.com/hmrc/pensions-scheme-stubs). From the Authority Wizard page you will be redirected to the dashboard. Navigate to the appropriate area by accessing items listed within the service-specific tiles on the dashboard. From here, the user can invite a person to become a pension administrator, accept an invitation to become a pension scheme administrator, remove themselves from a pension scheme and deregister as a pension scheme administrator.

There are numerous APIs implemented throughout the MPS architecture, and the relevant endpoints are illustrated below. For an overview of all PODS APIs, refer to the [PODS API Documentation](https://confluence.tools.tax.service.gov.uk/display/PODSP/PODS+API+Latest+Version).

## Note on terminology
The terms scheme reference number and submission reference number (SRN) are interchangeable within the PODS codebase; some downstream APIs use scheme reference number, some use submission reference number, probably because of oversight on part of the technical teams who developed these APIs. This detail means the same thing, the reference number that was returned from ETMP when the scheme details were submitted.

---

## License
This code is open source software Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at:

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

[↥ Back to Top](#manage-pensions-frontend)
