/*
 * Copyright 2024 HM Revenue & Customs
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

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import forms.YesNoForm
import models.mongo.{JourneyAnswers, JourneyStatus}
import models.mongo.JourneyStatus.{Completed, InProgress}
import models.CISType
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc._
import services.SectionCompletedService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.SectionCompletedView
import actions.TaxYearAction
import models.CISType.ConstructionIndustryScheme

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SectionCompletedController @Inject()(implicit val cc: MessagesControllerComponents,
                                                authAction: AuthorisedAction,
                                                view: SectionCompletedView,
                                                errorHandler: ErrorHandler,
                                                implicit val appConfig: AppConfig,
                                                sectionCompletedService: SectionCompletedService,
                                                ec: ExecutionContext
                                               ) extends FrontendController(cc) with I18nSupport {

  def form(): Form[Boolean] = YesNoForm.yesNoForm("sectionCompleted.error.required")

  def journeyName(cisType: CISType): String = {
    cisType match {
      case ConstructionIndustryScheme => "ConstructionIndustryScheme"
    }
  }

  def show(taxYear: Int, cisType: CISType): Action[AnyContent] =
    (authAction andThen TaxYearAction(taxYear, appConfig, ec)).async { implicit user =>
      sectionCompletedService.get(user.user.mtditid, taxYear, journeyName(cisType)).flatMap {
        case Some(value) =>
          value.data("status").validate[JourneyStatus].asOpt match {
            case Some(JourneyStatus.Completed) =>
              Future.successful(Ok(view(form().fill(true), taxYear, cisType)))

            case Some(JourneyStatus.InProgress) =>
              Future.successful(Ok(view(form().fill(false), taxYear, cisType)))

            case _ => Future.successful(Ok(view(form(), taxYear, cisType)))
          }
        case None => Future.successful(Ok(view(form(), taxYear, cisType)))
      }
    }


  def submit(taxYear: Int, cisType: CISType): Action[AnyContent] = (authAction andThen TaxYearAction(taxYear, appConfig, ec)).async { implicit user =>
    form()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, cisType))),
        answer => {
          saveAndRedirect(answer, taxYear, cisType, user.user.mtditid)
        }
      )
  }

  private def saveAndRedirect(answer: Boolean, taxYear: Int, cisType: CISType, mtditid: String)(implicit hc: HeaderCarrier): Future[Result] = {
    val status: JourneyStatus = if (answer) Completed else InProgress
    val journeyName = cisType match {
      case ConstructionIndustryScheme => "ConstructionIndustryScheme"
    }
    val model = JourneyAnswers(mtditid, taxYear, journeyName, Json.obj({
      "status" -> status
    }), Instant.now)
    sectionCompletedService.set(model)
    Future.successful(Redirect(appConfig.commonTaskListUrl(taxYear)))
  }

}