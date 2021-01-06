/*
 * Copyright 2021 HM Revenue & Customs
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

package viewmodels

import models.Link
import play.twirl.api.Html

case class PspSchemeDashboardCardViewModel(
                                            id: String,
                                            heading: String,
                                            subHeadings: Seq[(String, String)] = Seq.empty,
                                            optionalSubHeading: Option[(String, String)] = None,
                                            subHeadingParam: String = "bold",
                                            links: Seq[Link] = Nil,
                                            html: Option[Html] = None
                                          )
