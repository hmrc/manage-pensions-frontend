# Manage Pensions Frontend 

## Info
   
This service is also known as *Manage pension schemes*

This service allows an individual to -
1. Invite a person to become a pension administrator
2. Accept an invitation to become a pension scheme administrator
3. Remove themselves from a pension scheme
4. Deregister as a pension scheme administrator
 
This service also redirects a user to other services to do the following things -
1. Register as a pension scheme administrator
2. Edit their pension administrator details
3. Start registering a new pension scheme
4. Make changes to an existing pension scheme
5. Start filing an AFT return
6. Amend an existing AFT return

This service does not have its own backend, instead it uses the following backends for its integration -

### Dependencies

| Service                           | Link                                                      |
|-----------------------------------|-----------------------------------------------------------|
| pension-administrator             | https://github.com/hmrc/pension-administrator             |
| pensions-scheme                   | https://github.com/hmrc/pensions-scheme                   |
| pension-scheme-accounting-for-tax | https://github.com/hmrc/pension-scheme-accounting-for-tax |

### Endpoints used

| Service                           | HTTP Method | Route                                               | Purpose                                                                                                                  |
|-----------------------------------|-------------|-----------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| Pension Administrator             | POST        | /pension-administrator/invite                       | Stores an invitation in the invitations mongo collection                                                                 |
| Pension Administrator             | POST        | /pension-administrator/accept-invitation            | Creates an association of the PSA with the scheme and removes invitation                                                 |
| Pension Administrator             | POST        | /pension-administrator/invite                       | Stores an invitation in the invitations mongo collection                                                                 |
| Pension Administrator             | POST        | /pension-administrator/remove-psa                   | Removes the association between PSA and pension scheme                                                                   |
| Pension Administrator             | POST        | /pension-administrator/deregister-psa/{psaId}       | De-enrols a PSA from the service                                                                                         |
| Pension Administrator             | GET         | /pension-administrator/can-deregister/{psaId}       | Returns true is PSA has already dissociated themselves from all active schemes and false if any such associations remain |
| Pension Administrator             | GET         | /pension-administrator/get-minimal-psa              | Returns minimal details of the PSA                                                                                       |
| Pension Administrator             | GET         | /pension-administrator/psa-subscription-details     | Returns all the details of a PSA subscription                                                                            |
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

## Running the service
 
Service Manager: PODS_ALL

Port: 8204

Link: http://localhost:8204/manage-pension-schemes

Enrolment: `HMRC-PODS-ORG` `PsaId` `A2100005` (local and Staging environments only)

## Tests and prototype

[View the prototype here](https://pods-prototype.herokuapp.com/)

| Repositories      | Link                                           |
|-------------------|------------------------------------------------|
| Journey tests     | https://github.com/hmrc/pods-journey-tests     |
| Performance tests | https://github.com/hmrc/pods-performance-tests |
| Prototype         | https://github.com/hmrc/pods-prototype         |           

## Note on terminology
The terms scheme reference number and submission reference number (SRN) are interchangeable within the PODS codebase; some downstream APIs use scheme reference number, some use submission reference number, probably because of oversight on part of the technical teams who developed these APIs. This detail means the same thing, the reference number that was returned from ETMP when the scheme details were submitted.
