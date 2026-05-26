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

package config

import audit.{AuditModel, AuditService}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.TestSuite
import play.api.libs.json.Writes
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import scala.concurrent.{ExecutionContext, Future}

trait MockAuditService { this: TestSuite =>

  val mockAuditService: AuditService = org.mockito.Mockito.mock(classOf[AuditService])

  def mockSendAudit[T](event: AuditModel[T]): Unit = {
    when(
      mockAuditService.sendAudit(any[AuditModel[T]]())(any[HeaderCarrier](), any[ExecutionContext](), any[Writes[T]]())
    ).thenReturn(Future.successful(AuditResult.Success))
  }
}
