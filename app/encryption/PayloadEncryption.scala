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

package encryption

import play.api.libs.json._
import uk.gov.hmrc.crypto._
import uk.gov.hmrc.crypto.json.{JsonDecryptor, JsonEncryptor}

import scala.concurrent.Future

case class SecretRequest(data: String)

object SecretRequest {
  implicit val format = Json.format[SecretRequest]
}

trait SendsSecretRequest  {
  def payloadEncryption: PayloadEncryption

  def secretRequest[I,R](input: I)(block: SecretRequest => Future[R])(implicit w: Writes[I]) = {
    block(toSecretRequest(w.writes(input)))
  }

  private def toSecretRequest[T](payload: T)(implicit writes: Writes[T]): SecretRequest = {
    SecretRequest(payloadEncryption.encrypt(payload).as[String])
  }
}

class PayloadEncryption(jsonEncryptionKey: String) {

  implicit val crypto = new LocalCrypto(jsonEncryptionKey)

  def encrypt[T](payload: T)(implicit writes: Writes[T]): JsValue = {
    val encryptor = new JsonEncryptor[T]()(crypto, writes)
    encryptor.writes(Protected(payload))
  }

  def decrypt[T](payload: JsValue)(implicit reads: Reads[T]): T = {
    val decryptor = new JsonDecryptor()(crypto, reads)
    val decrypted: JsResult[Protected[T]] = decryptor.reads(payload)

    decrypted.asOpt.map(_.decryptedValue).getOrElse(throw new scala.RuntimeException(s"Failed to decrypt payload: [$payload]"))
  }
}

private[encryption] class LocalCrypto(anEncryptionKey: String) extends CompositeSymmetricCrypto {
  override protected val currentCrypto: Encrypter with Decrypter = new AesCrypto {
    override protected val encryptionKey: String = anEncryptionKey
  }
  override protected val previousCryptos: Seq[Decrypter] = Seq.empty
}
