# Manage Pensions Frontend 

[![Build Status](https://travis-ci.org/hmrc/manage-pensions-frontend.svg)](https://travis-ci.org/hmrc/manage-pensions-frontend) [ ![Download](https://api.bintray.com/packages/hmrc/releases/manage-pensions-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/manage-pensions-frontend/_latestVersion)

This is a placeholder README.md for a new repository

### License 

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")


# Manage Pensions Frontend 

## Info

This service is also known as *Manage pension schemes*

This service allows an individual to -
1. Invite a person to become a pension administrator
2. Accept an invitation to become a pension scheme administrator
3. Remove themselves from a pension scheme
4. Deregister as a pension scheme administer

This service also redirects a user to other services to do the following things -
1. Register as a pension scheme administrator
2. Edit their pension administrator details
3. Start registering a new pension scheme
4. Make changes to an existing pension scheme
5. Start filing an AFT return
6. Amend an existing AFT return

This service does not have it's own backend, instead it the following backends for its integration -
1. pension-administrator
2. pensions-scheme
3. pension-scheme-accounting-for-tax

### Dependencies

|Service                            |Link                                                       |
|-----------------------------------|-----------------------------------------------------------|
|pension-administrator              |https://github.com/hmrc/pension-administrator              |
|pensions-scheme                    |https://github.com/hmrc/pensions-scheme                    |
|pension-scheme-accounting-for-tax  |https://github.com/hmrc/pension-scheme-accounting-for-tax  |

### Endpoints used

|Service        |HTTP Method |Route                                  |Purpose |
|---------------|--- |----------------|----------------------------------|
|Tai            |GET |/tai/${nino}/tax-account/${year} /expenses/flat-rate-expenses| Returns details of a users tax account specifically that of IABD 57 |
|Tai            |POST|/tai/${nino}/tax-account/${year} /expenses/flat-rate-expenses| Updates a users tax account specifically that of IABD 57  |
|Citizen Details|GET |/citizen-details/${nino}/etag|retrieves the users etag which is added to their update request to NPS to ensure optimistic locking|

## Running the service

Service Manager: PODS_ALL

Port: 8204

Link: http://localhost:8204/manage-pension-schemes

PSAIds: `A2100005` (local and Staging environments only)

## Tests and prototype

[View the prototype here](https://pods-prototype.herokuapp.com/)

|Repositories           |Link                                                                   |
|-----------------------|-----------------------------------------------------------------------|
|Journey tests          |https://github.com/hmrc/pods-journey-tests                             |
|Performance tests      |https://github.com/hmrc/pods-performance-tests                         |
|Prototype              |https://github.com/hmrc/pods-prototype                                 |
