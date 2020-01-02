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

package config

import com.google.inject.{Inject, Singleton}
import play.api.Mode
import play.api.{Configuration, Environment}

import scala.collection.mutable.ArrayBuffer

trait FeatureSwitchManagementService {
  def change(name: String, newValue: Boolean): Boolean

  def get(name: String): Boolean

  def reset(name: String): Unit
}

class FeatureSwitchManagementServiceProductionImpl @Inject()(val runModeConfiguration: Configuration,
                                                             environment: Environment) extends
  FeatureSwitchManagementService {

  protected def mode: Mode = environment.mode

  override def change(name: String, newValue: Boolean): Boolean =
    runModeConfiguration.get[Boolean](s"features.$name")

  override def get(name: String): Boolean =
    runModeConfiguration.get[Boolean](s"features.$name")

  override def reset(name: String): Unit = ()
}

@Singleton
class FeatureSwitchManagementServiceTestImpl @Inject()(val runModeConfiguration: Configuration,
                                                       environment: Environment) extends
  FeatureSwitchManagementService {

  private lazy val featureSwitches: ArrayBuffer[FeatureSwitch] = new ArrayBuffer[FeatureSwitch]()

  protected def mode: Mode = environment.mode

  override def change(name: String, newValue: Boolean): Boolean = {
    val featureSwitchExists = runModeConfiguration.getOptional[Boolean](s"features.$name")
    if (featureSwitchExists.nonEmpty) {
      reset(name)
      featureSwitches += FeatureSwitch(name, newValue)
    }
    true
  }

  override def get(name: String): Boolean =
    featureSwitches.find(_.name == name) match {
      case None => runModeConfiguration.get[Boolean](s"features.$name")
      case Some(featureSwitch) => featureSwitch.isEnabled
    }

  override def reset(name: String): Unit = {
    featureSwitches -= FeatureSwitch(name, isEnabled = false)
    featureSwitches -= FeatureSwitch(name, isEnabled = true)
  }
}

case class FeatureSwitch(name: String, isEnabled: Boolean)
