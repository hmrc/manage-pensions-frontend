package connectors

import java.time.LocalDate

import models.PsaToBeRemovedFromScheme
import org.mockito.Matchers
import org.scalatest.AsyncFlatSpec
import org.scalatest.prop.Checkers
import play.api.libs.json.Json
import utils.WireMockHelper

class PsaRemovalConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper with Checkers {
  override protected def portConfigKey: String = "microservice.services.pension-administrator.port"

  "Delete" should "return successful following a successful deletion" in {

    server.stubFor(
      post(urlEqualTo(deleteUrl))
        .withRequestBody(equalToJson(requestJson))
    )


  }

  private val psaToDelete = PsaToBeRemovedFromScheme("238DAJFAS", "XXAJ329AJJ", new LocalDate(2009,1,1))
  private val deleteUrl = "/pension-administrator/delete"
  private val requestJson = Json.stringify(Json.toJson(psaToDelete))
}
