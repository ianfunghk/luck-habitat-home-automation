import groovy.transform.Field

@Field final int _dimChkSec = 30

definition (
    name: "Motion light with dimming",
    namespace: "ianfunghk",
    author: "Ian Fung",
    description: "Dim the light before turning off",
    category: "Luck Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
  section("Turn on when motion detected:") {
    input "themotion",
    "capability.motionSensor",
    required: true,
    title: "Where?"
  }
  section("Turn off when there's been no movement for") {
    input "secs",
    "number",
    required: true,
    title: "Secs?"
  }
  section("Turn on this light") {
    input "theswitch",
    "capability.switch",
    required: true
  }
}

def installed() {
  log.debug "Installed with settings: ${settings}"
  initialize()
}

def updated() {
  log.debug "Updated with settings: ${settings}"
  unsubscribe()
  initialize()
}

def initialize() {
  subscribe(themotion, "motion.active", motionDetectedHandler)
  subscribe(themotion, "motion.inactive", motionStoppedHandler)
}

// implement event handlers
def motionDetectedHandler(evt) {
  log.debug "motionDetectedHandler called: $evt"
  unschedule(turnOff)
  theswitch.on()
  theswitch.setLevel(100)
}

def motionStoppedHandler(evt) {
  log.debug "motionStoppedHandler called: $evt"
  runIn(secs, checkMotion)
  log.debug "scheduled checkMotion run after $secs secs"
}

def checkMotion() {
  log.debug "In checkMotion scheduled method"

  theswitch.setLevel(50)

  // get the current state object for the motion sensor
  def motionState = themotion.currentState("motion")

  if (motionState.value == "inactive") {
    // get the time elapsed between now and when the motion reported inactive
    def elapsed = now() - motionState.date.time

    // elapsed time is in milliseconds, so the threshold must be converted to milliseconds too
    def threshold = 1000 * secs

    if (elapsed >= threshold) {
      log.debug "Motion has stayed inactive long enough since last check ($elapsed ms):  schedule switch off"
      runIn(_dimChkSec, turnOff)
    } else {
      log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms):  doing nothing"
    }
  } else {
    // Motion active; just log it and do nothing
    log.debug "Motion is active, do nothing and wait for inactive"
  }
}

def turnOff() {
  log.debug "In turnOff scheduled method"

  // get the current state object for the motion sensor
  def motionState = themotion.currentState("motion")

  if (motionState.value == "inactive") {
    // get the time elapsed between now and when the motion reported inactive
    def elapsed = now() - motionState.date.time

    // elapsed time is in milliseconds, so the threshold must be converted to milliseconds too
    def threshold = 1000 * (secs + _dimChkSec)

    if (elapsed >= threshold) {
      log.debug "Motion has stayed inactive long enough since last check ($elapsed ms):  turning switch off now"
      theswitch.off()
    } else {
      log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms):  doing nothing"
    }
  } else {
    // Motion active; just log it and do nothing
    log.debug "Motion is active, do nothing and wait for inactive"
  }
}
