/*
 * Copyright 2023 HM Revenue & Customs
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

package forms

import models.forms.ContractorDetails
import play.api.data.{Form, FormError}
import support.UnitTest

class ContractorDetailsFormSpec extends UnitTest {

  private def theForm(isAgent: Boolean, employerRefs: Seq[String] = Seq.empty): Form[ContractorDetails] = ContractorDetailsForm.contractorDetailsForm(isAgent, employerRefs)

  private val contractorName = "contractorName"
  private val employerReferenceNumber = "employerReferenceNumber"
  private val loremIpsum = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore" +
    " magna aliqua. Ut enim ad minim veniam, quis nostrud" +
    " exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in" +
    " voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt" +
    " in culpa qui officia deserunt mollit anim id est laborum."

  private val validInput = Map(contractorName -> "Contractor name", employerReferenceNumber -> "123/AB12345")
  private val validResult = ContractorDetails("Contractor name", "123/AB12345")

  "The ContractorDetailsForm" should {
    "Correctly validate a contractor details view model" when {
      "a valid model is entered" in {
        val actual = theForm(isAgent = false).bind(validInput)
        actual.value shouldBe Some(validResult)
      }
    }

    "Invalidate when no contractor name provided, individual" in {
      val testInput = Map(employerReferenceNumber -> "123/AB12345")
      val actual = theForm(isAgent = false).bind(testInput)
      actual.errors should contain(FormError(contractorName, "contractor-details.name.error.noEntry.individual"))
    }

    "Invalidate when the employerRef is already used" in {
      val testInput = Map(contractorName -> contractorName, employerReferenceNumber -> "123/AB12345")
      val actual = theForm(isAgent = false, Seq("123/AB12345")).bind(testInput)
      actual.errors should contain(FormError(employerReferenceNumber, "contractor-details.employer-ref.error.duplicate"))
    }

    "Invalidate when no contractor name provided, agent" in {
      val testInput = Map(employerReferenceNumber -> "123/AB12345")
      val actual = theForm(isAgent = true).bind(testInput)
      actual.errors should contain(FormError(contractorName, "contractor-details.name.error.noEntry.agent"))
    }

    "Invalidate when contractor name too long" in {
      val testInput = Map(contractorName -> loremIpsum, employerReferenceNumber -> "123/AB12345")
      val actual = theForm(isAgent = true).bind(testInput)
      actual.errors should contain(FormError(contractorName, "contractor-details.name.error.notCharLimit"))
    }

    "Invalidate when contractor name contains invalid characters" in {
      val testInput = Map(contractorName -> "水", employerReferenceNumber -> "123/AB12345")
      val actual = theForm(isAgent = true).bind(testInput)
      actual.errors should contain(FormError(contractorName, "contractor-details.name.error.wrongFormat"))
    }

    "Invalidate when no employer Reference Number provided" in {
      val testInput = Map(contractorName -> "Contractor name")
      val actual = theForm(isAgent = false).bind(testInput)
      actual.errors should contain(FormError(employerReferenceNumber, "contractor-details.employer-ref.error.noEntry"))
    }

    "Invalidate when employer Reference Number contains invalid characters" in {
      val testInput = Map(contractorName -> "Name", employerReferenceNumber -> "1/23AB12345")
      val actual = theForm(isAgent = true).bind(testInput)
      actual.errors should contain(FormError(employerReferenceNumber, "contractor-details.employer-ref.error.wrongFormat"))
    }
  }
}
