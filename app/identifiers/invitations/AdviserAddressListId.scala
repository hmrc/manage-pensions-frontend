package identifiers.invitations

import identifiers.TypedIdentifier
import models.TolerantAddress

object AdviserAddressListId extends TypedIdentifier[TolerantAddress] {
  override def toString: String = "adviserAddressList"
}
