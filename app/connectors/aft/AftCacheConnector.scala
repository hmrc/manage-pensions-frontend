package connectors.aft

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.MicroserviceCacheConnector
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}

import scala.concurrent.{ExecutionContext, Future}

class AftCacheConnector @Inject()(
                                   config: FrontendAppConfig,
                                   http: WSClient
                                 ) {

  val url = s"${config.pensionAdminUrl}/pension-scheme-accounting-for-tax/journey-cache/isLocked"

  def lockedBy(pstr: String, startDate: String)(implicit
                             ec: ExecutionContext,
                             hc: HeaderCarrier
  ): Future[Option[String]] = {
    http.url(url)
      .withHttpHeaders(hc.withExtraHeaders(("id", pstr + startDate)).headers: _*)
      .get()
      .flatMap {
        response =>
          response.status match {
            case NOT_FOUND =>
              Future.successful(None)
            case OK =>
              if(response.body.isEmpty) {

              }
              val responseJson = Json.parse(response.body)
              val result = ((responseJson \ "name").asOpt[String], (responseJson \ "lastName").asOpt[String]) match {
                case (Some(firstName), Some(lastName)) => Some(s"$firstName $lastName")
                case _ => None
              }
              Future.successful(result)
            case _ =>
              Future.failed(new HttpException(response.body, response.status))
          }
      }
  }
}
