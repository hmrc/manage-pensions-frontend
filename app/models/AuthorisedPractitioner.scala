/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

import java.time.LocalDate

import play.api.libs.json.Json
import play.api.libs.json.OFormat

case class AuthorisingPSA(firstName: Option[String], lastName: Option[String], middleName: Option[String], organisationOrPartnershipName: Option[String]) {
  def name: String = (organisationOrPartnershipName, firstName, lastName) match {
    case (Some(v), _, _) => v
    case (_, Some(f), Some(l)) => s"$f $l"
    case _ => throw new RuntimeException("No authorising psa name")
  }
}

object AuthorisingPSA {
  implicit val formats: OFormat[AuthorisingPSA] = Json.format[AuthorisingPSA]
}

case class AuthorisingIndividual(firstName: String, lastName: String) {
  def fullName: String = s"$firstName $lastName"
}

object AuthorisingIndividual {
  implicit val formats: OFormat[AuthorisingIndividual] = Json.format[AuthorisingIndividual]
}

case class AuthorisedPractitioner(
  organisationOrPartnershipName: Option[String],
  individual: Option[AuthorisingIndividual],
  authorisingPSAID: String,
  authorisingPSA: AuthorisingPSA,
  relationshipStartDate:LocalDate,
  id: String) {
  def name = (individual, organisationOrPartnershipName) match {
    case (Some(i), _) => i.fullName
    case (_, Some(o)) => o
    case _ => throw new RuntimeException("No psp name")
  }
}

object AuthorisedPractitioner {

  implicit val formats: OFormat[AuthorisedPractitioner] = Json.format[AuthorisedPractitioner]
}


/*
   "pspDetails":[
      {
         "authorisingPSAID":"A2100005",
         "authorisingPSA":{
            "firstName":"Nigel",
            "lastName":"Smith",
            "middleName":"Robert"
         },
         "relationshipStartDate":"2021-04-01",
         "id":"A2200005",
         "organisationOrPartnershipName":"PSP Limited Company 1"
      },
      {
         "authorisingPSAID":"A2100007",
         "individual":{
            "firstName":"PSP Individual",
            "lastName":"Second"
         },
         "authorisingPSA":{
            "organisationOrPartnershipName":"Acme Ltd"
         },
         "relationshipStartDate":"2021-04-01",
         "id":"A2200007"
      }
   ],
 */