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

package controllers

import java.time.Month

import actions.ActionsProvider
import config.AppConfig
import javax.inject.Inject
import models.pages.ContractorCYAPage.mapToInYearPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{SessionHelper, UrlUtils}
import views.html.ContractorCYAView

import scala.concurrent.ExecutionContext

class ContractorCYAController @Inject()(actionsProvider: ActionsProvider,
                                        pageView: ContractorCYAView)
                                       (implicit mcc: MessagesControllerComponents, ec: ExecutionContext, appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int,
           month: String,
           contractor: String): Action[AnyContent] = actionsProvider.inYearWithPreviousDataFor(taxYear, month, contractor) { implicit request =>
    val pageModel = mapToInYearPage(
      taxYear,
      request.incomeTaxUserData.inYearCisDeductionsWith(UrlUtils.decode(contractor)).get,
      Month.valueOf(month.toUpperCase)
    )

    Ok(pageView(pageModel))
  }
}
