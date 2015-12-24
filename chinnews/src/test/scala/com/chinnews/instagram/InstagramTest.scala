package com.chinnews.instagram

import info.chinnews.instagram.actors.SubscriptionParserActor
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by Tsarevskiy
  */
class InstagramTest extends FlatSpec with Matchers {

  it should "parse message from instagram subscription" in {

    val message = "[{\"changed_aspect\": \"media\", " +
      "\"object\": \"tag\", \"object_id\": \"moscow\", \"time\": 1450979289, " +
      "\"subscription_id\": 21371989, \"data\": {}}]".replace("{}", "\" \"")

    SubscriptionParserActor.forAllJsonArrayElements(message,
      (el: String) =>
        SubscriptionParserActor.handleOneJsonElement(el,
        subscriptionUpdate => subscriptionUpdate.getSubscriptionId should be (21371989)))
  }

}
