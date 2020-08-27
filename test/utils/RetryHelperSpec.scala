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

package utils

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import base.SpecBase
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HttpException, UpstreamErrorResponse}

import scala.annotation.tailrec
import scala.concurrent.Future

class RetryHelperSpec extends SpecBase with MockitoSugar with ScalaFutures with GuiceOneAppPerSuite {

  private val MAX_ATTEMPTS: Int = 10
  private val INITIAL_WAIT: Int = 10
  private val WAIT_FACTOR: Float = 1.5F

  val retryHelper = new RetryHelperClass()
  val TIMEOUT = 5


  "RetryHelper" must {

    "return a successful Future" in {
      val successfulFunction = () => Future.successful("A successful future")

      whenReady(retryHelper.retryOnFailure(successfulFunction, frontendAppConfig)) {
        result => result mustEqual "A successful future"
      }
    }

    "retry on a 503 HTTP exception " in {
      val failedFunction = () => Future.failed(new HttpException("Bad Request", 503))
      whenReady(retryHelper.retryOnFailure(failedFunction, frontendAppConfig).failed, timeout(Span(TIMEOUT, Seconds))) {
        case e: HttpException => e.responseCode mustEqual SERVICE_UNAVAILABLE
      }
    }

    "back off exponentially" in {
      @tailrec
      def getMinimumExpectedDuration(iteration: Int, expectedTime: Long, currentWait: Int): Long = {
        if (iteration >= MAX_ATTEMPTS) {
          expectedTime + currentWait
        } else {
          val nextWait: Int = Math.ceil(currentWait * WAIT_FACTOR).toInt
          getMinimumExpectedDuration(iteration + 1, expectedTime + currentWait, nextWait)
        }
      }

      val failedFunction = () => Future.failed(UpstreamErrorResponse("Bad Request", 503, 0))
      val startTime = LocalDateTime.now

      whenReady(retryHelper.retryOnFailure(failedFunction, frontendAppConfig).failed, timeout(Span(TIMEOUT, Seconds))) {
        case e: UpstreamErrorResponse => e.statusCode mustEqual SERVICE_UNAVAILABLE
      }

      val endTime = LocalDateTime.now
      val expectedTime = getMinimumExpectedDuration(1, INITIAL_WAIT, INITIAL_WAIT)
      ChronoUnit.MILLIS.between(startTime, endTime) must be >= expectedTime
    }

    "show that it can pass after it fails" in {

      val NUMBER_OF_RETRIES = 5
      var counter = 0

      val failThenSuccessFunc = () => {
        if (counter < NUMBER_OF_RETRIES) {
          counter = counter + 1
          Future.failed(UpstreamErrorResponse("Bad Request", 503, 0))
        }
        else {
          Future.successful("A successful future")
        }
      }

      whenReady(retryHelper.retryOnFailure(failThenSuccessFunc, frontendAppConfig), timeout(Span(TIMEOUT, Seconds))) {
        result => {
          result mustEqual "A successful future"
          counter must be >= NUMBER_OF_RETRIES
        }
      }
    }
  }
}

class RetryHelperClass extends RetryHelper
