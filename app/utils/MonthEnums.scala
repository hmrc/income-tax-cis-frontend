/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.json.{JsPath, JsString, Reads, Writes}

sealed trait Month {
  val value: String
}

object Month {

  //scalastyle:off
  def apply(s: String): Month = s.toUpperCase match {
    case "MAY" => MAY
    case "JUNE" => JUNE
    case "JULY" => JULY
    case "AUGUST" => AUGUST
    case "SEPTEMBER" => SEPTEMBER
    case "OCTOBER" => OCTOBER
    case "NOVEMBER" => NOVEMBER
    case "DECEMBER" => DECEMBER
    case "JANUARY" => JANUARY
    case "FEBRUARY" => FEBRUARY
    case "MARCH" => MARCH
    case "APRIL" => APRIL
    case unknown@_ => throw new Exception(s"'$unknown' cannot be converted to a month")
  }
  //scalastyle:on

  implicit val reads: Reads[Month] = JsPath.read[String].map(apply)

  implicit val writes: Writes[Month] = Writes { month => JsString(month.value) }

}

case object MAY extends Month { val value = "May"}
case object JUNE extends Month { val value = "June"}
case object JULY extends Month { val value = "July"}
case object AUGUST extends Month { val value = "August"}
case object SEPTEMBER extends Month { val value = "September"}
case object OCTOBER extends Month { val value = "October"}
case object NOVEMBER extends Month { val value = "November"}
case object DECEMBER extends Month { val value = "December"}
case object JANUARY extends Month { val value = "January"}
case object FEBRUARY extends Month { val value = "February"}
case object MARCH extends Month { val value = "March"}
case object APRIL extends Month { val value = "April"}

