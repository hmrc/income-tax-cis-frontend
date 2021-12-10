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

package forms.validation.mappings

import play.api.data.Forms.{default, of, optional, text}
import play.api.data.{FieldMapping, Mapping}

object MappingUtil extends Formatters {

  val trimmedText: Mapping[String] = default(text, "").transform(_.trim, identity)

  val oText: Mapping[Option[String]] = optional(text)

  implicit class OTextUtil(mapping: Mapping[Option[String]]) {
    def toText: Mapping[String] =
      mapping.transform(
        x => x.fold("")(x => x),
        x => Some(x)
      )
  }

  def currency(requiredKey: String,
               wrongFormatKey: String = "common.error.invalid_currency_format",
               maxAmountKey: String = "common.error.amountMaxLimit",
               args: Seq[String] = Seq.empty[String]
              ): FieldMapping[BigDecimal] =
    of(currencyFormatter(requiredKey, wrongFormatKey, maxAmountKey, args))

}
