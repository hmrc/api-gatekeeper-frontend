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

package uk.gov.hmrc.gatekeeper.encryption

import scala.concurrent.Future

import play.api.libs.json._
import uk.gov.hmrc.crypto._
import uk.gov.hmrc.crypto.json.JsonEncryption

case class SecretRequest(data: String)

object SecretRequest {
  implicit val format: OFormat[SecretRequest] = Json.format[SecretRequest]
}

trait SendsSecretRequest {
  def payloadEncryption: PayloadEncryption

  def secretRequest[I, R](input: I)(block: SecretRequest => Future[R])(implicit w: Writes[I]) = {
    block(toSecretRequest(w.writes(input)))
  }

  private def toSecretRequest[T](payload: T)(implicit writes: Writes[T]): SecretRequest = {
    SecretRequest(payloadEncryption.encrypt(payload).as[String])
  }
}

case class SensitiveT[T](override val decryptedValue: T) extends Sensitive[T]

class PayloadEncryption(jsonEncryptionKey: String) {

  implicit val crypto: LocalCrypto = new LocalCrypto(jsonEncryptionKey)

  def encrypt[T](payload: T)(implicit writes: Writes[T]): JsValue = {
    val encrypter = JsonEncryption.sensitiveEncrypter[T, SensitiveT[T]]
    encrypter.writes(SensitiveT(payload))
  }

  def decrypt[T](payload: JsValue)(implicit reads: Reads[T]): T = {
    val encryptedValue: JsValue = payload
    val decrypter               = JsonEncryption.sensitiveDecrypter[T, SensitiveT[T]](SensitiveT.apply)
    decrypter.reads(encryptedValue)
      .asOpt
      .map(_.decryptedValue)
      .getOrElse { sys.error(s"Failed to decrypt payload: [$payload]") }
  }
}

private[encryption] class LocalCrypto(anEncryptionKey: String) extends Encrypter with Decrypter {

  implicit val aesCrypto: Encrypter with Decrypter = SymmetricCryptoFactory.aesCrypto(anEncryptionKey)

  override def encrypt(plain: PlainContent): Crypted = aesCrypto.encrypt(plain)

  override def decrypt(reversiblyEncrypted: Crypted): PlainText = aesCrypto.decrypt(reversiblyEncrypted)

  override def decryptAsBytes(reversiblyEncrypted: Crypted): PlainBytes = aesCrypto.decryptAsBytes(reversiblyEncrypted)
}
