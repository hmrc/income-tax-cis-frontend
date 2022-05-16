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

import actions.ActionsProvider
import config.AppConfig
import models.pages.ContractorSummaryPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.ContractorSummaryView

import javax.inject.Inject

class ContractorSummaryController @Inject()(actionsProvider: ActionsProvider,
                                            pageView: ContractorSummaryView,
                                            inYearUtil: InYearUtil)
                                           (implicit mcc: MessagesControllerComponents, appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, contractor: String): Action[AnyContent] = actionsProvider.userPriorDataFor(taxYear, contractor) { implicit request =>
    Ok(pageView(ContractorSummaryPage(taxYear, inYearUtil.inYear(taxYear), contractor, request.incomeTaxUserData)))
  }
}
